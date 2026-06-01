package com.example.dsp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioRecord
import android.media.AudioPlaybackCaptureConfiguration
import android.media.MediaRecorder.AudioSource
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*
import kotlin.random.Random

class AudioEngine {
    companion object {
        val instance = AudioEngine()
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE_FACTOR = 4
    }

    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null
    private var isStereoInput = false
    private val isRunning = AtomicBoolean(false)
    private var workerThread: Thread? = null

    // Real-time levels for VU Meters
    val volumeL = MutableStateFlow(0f)
    val volumeR = MutableStateFlow(0f)

    // Real-time spectrum values for 16-band visualizer
    private val spectrumValues = FloatArray(16) { 0f }
    val spectrumFlow = MutableStateFlow(FloatArray(16) { 0.1f })

    // Playback state
    val isPlaying = MutableStateFlow(false)

    // DSP Adjustable Properties (thread-safe, updated from VM)
    @Volatile var masterVolL = 0.8f
    @Volatile var masterVolR = 0.8f
    @Volatile var driverName = "AAudio"
    @Volatile var bufferSize = 256

    // Mixer gains
    @Volatile var mixSub = 0.8f
    @Volatile var mixLow = 0.8f
    @Volatile var mixMid = 0.8f
    @Volatile var mixHigh = 0.8f

    // EQ Config
    @Volatile var eqMode = 7 // 7, 15, or 31
    private val eqBiquadsLeft = Array(31) { BiquadFilter() }
    private val eqBiquadsRight = Array(31) { BiquadFilter() }
    @Volatile var eqGains = FloatArray(31) { 0.0f }

    // Routing Chain Order (default)
    @Volatile var routingChain = listOf("EQ", "CROSSOVER", "COMPRESSOR", "WIDENER", "LIMITER")
    @Volatile var eqBypass = false
    @Volatile var crossoverBypass = false
    @Volatile var compressorBypass = false
    @Volatile var widenerBypass = false
    @Volatile var limiterBypass = false

    // Compressor Props
    @Volatile var compThreshold = -20f
    @Volatile var compRatio = 4.0f
    @Volatile var compAttack = 10f
    @Volatile var compRelease = 100f
    private var compEnvelope = 0.0

    // Stereo imager Props
    @Volatile var wideFactor = 1.0f

    // Limiter Props
    @Volatile var limitCeiling = -1.0f

    // Crossover Outputs
    @Volatile var crossSubOut = 0.8f
    @Volatile var crossLowOut = 0.8f
    @Volatile var crossMidOut = 0.8f
    @Volatile var crossHighOut = 0.8f
    
    @Volatile var crossSubLowCutoff = 120f
    @Volatile var crossLowMidCutoff = 400f
    @Volatile var crossMidHighCutoff = 4000f

    // Internal Filters
    private val crossSubLPStr = BiquadFilter()
    private val crossLowHPStr = BiquadFilter()
    private val crossLowLPStr = BiquadFilter()
    private val crossMidHPStr = BiquadFilter()
    private val crossMidLPStr = BiquadFilter()
    private val crossHighHPStr = BiquadFilter()

    // 4 visualizer peak meters
    val bandPowerSub = MutableStateFlow(0f)
    val bandPowerLow = MutableStateFlow(0f)
    val bandPowerMid = MutableStateFlow(0f)
    val bandPowerHigh = MutableStateFlow(0f)

    init {
        updateEqFilters()
        updateCrossoverFilters()
    }

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)
        isPlaying.value = true

        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        
        val chosenBufSize = max(minBufSize, bufferSize * BUFFER_SIZE_FACTOR * 4)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SAMPLE_RATE)
                        .build()
                )
                .setBufferSizeInBytes(chosenBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to init AudioTrack: ${e.message}")
        }

        // Initialize AudioRecord (either via system Playback Capture or MIC fallback)
        audioRecord = initAudioRecord()

        workerThread = Thread {
            renderLoop()
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun setMediaProjection(projection: MediaProjection?) {
        this.mediaProjection = projection
        if (isRunning.get()) {
            synchronized(this) {
                try {
                    audioRecord?.let {
                        if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            it.stop()
                        }
                        it.release()
                    }
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Error stopping record for projection update: ${e.message}")
                }
                audioRecord = initAudioRecord()
            }
        }
    }

    private fun initAudioRecord(): AudioRecord? {
        val mp = mediaProjection
        if (mp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Setup playback capture configuration for system audio / YouTube / etc.
                val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mp)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                val recBufSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_FLOAT
                )

                val record = AudioRecord.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setSampleRate(SAMPLE_RATE)
                            .build()
                    )
                    .setAudioPlaybackCaptureConfig(captureConfig)
                    .setBufferSizeInBytes(max(recBufSize, bufferSize * 2 * 4))
                    .build()

                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    record.startRecording()
                    isStereoInput = true
                    Log.d("AudioEngine", "System Playback Capture AudioRecord successfully initialized!")
                    return record
                } else {
                    record.release()
                }
            } catch (e: Exception) {
                Log.e("AudioEngine", "Failed to init system Playback Capture: ${e.message}")
            }
        }

        // Fallback to MIC if media projection not available or failed
        var record: AudioRecord? = null
        var isStereo = true
        val recBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        
        try {
            record = AudioRecord(
                AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT,
                max(recBufSize, bufferSize * 2 * 4)
            )
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                record.startRecording()
            } else {
                record.release()
                record = null
            }
        } catch (e: SecurityException) {
            Log.e("AudioEngine", "Security exception initializing stereo AudioRecord: ${e.message}")
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to init stereo AudioRecord: ${e.message}")
        }

        if (record == null) {
            isStereo = false
            val minMonoBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            try {
                record = AudioRecord(
                    AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    max(minMonoBuf, bufferSize * 4)
                )
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    record.startRecording()
                } else {
                    record.release()
                    record = null
                }
            } catch (e: SecurityException) {
                Log.e("AudioEngine", "Security exception initializing mono AudioRecord: ${e.message}")
            } catch (e: Exception) {
                Log.e("AudioEngine", "Failed to init mono AudioRecord: ${e.message}")
            }
        }

        isStereoInput = isStereo
        return record
    }

    fun stop() {
        if (!isRunning.get()) return
        isRunning.set(false)
        isPlaying.value = false
        try {
            workerThread?.join(500)
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error joining thread: ${e.message}")
        }
        audioTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error stopping track: ${e.message}")
            }
        }
        audioTrack = null

        audioRecord?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e("AudioEngine", "Error stopping record: ${e.message}")
            }
        }
        audioRecord = null

        workerThread = null
    }

    fun toggle() {
        if (isPlaying.value) {
            stop()
        } else {
            start()
        }
    }

    fun updateEqGains(gains: FloatArray) {
        eqGains = gains
        updateEqFilters()
    }

    fun updateEqFilters() {
        // Center frequencies for modes
        val freqs = when (eqMode) {
            7 -> doubleArrayOf(60.0, 150.0, 400.0, 1000.0, 2500.0, 6000.0, 15000.0)
            15 -> doubleArrayOf(25.0, 40.0, 63.0, 100.0, 160.0, 250.0, 400.0, 630.0, 1000.0, 1600.0, 2500.0, 4000.0, 6300.0, 1000.0, 16000.0)
            else -> doubleArrayOf(
                20.0, 25.0, 31.5, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0, 315.0, 400.0, 500.0, 630.0, 800.0, 
                1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0, 8000.0, 10000.0, 12500.0, 16000.0, 20000.0
            )
        }
        
        val qValue = if (eqMode == 7) 1.0 else if (eqMode == 15) 1.4 else 2.8

        for (i in freqs.indices) {
            val gainDb = if (i < eqGains.size) eqGains[i].toDouble() else 0.0
            eqBiquadsLeft[i].setPeakingEq(SAMPLE_RATE.toDouble(), freqs[i], qValue, gainDb)
            eqBiquadsRight[i].setPeakingEq(SAMPLE_RATE.toDouble(), freqs[i], qValue, gainDb)
        }
    }

    fun updateCrossoverFilters() {
        val sampleRateDb = SAMPLE_RATE.toDouble()
        crossSubLPStr.setLowPass(sampleRateDb, crossSubLowCutoff.toDouble(), 0.707)
        
        crossLowHPStr.setHighPass(sampleRateDb, crossSubLowCutoff.toDouble(), 0.707)
        crossLowLPStr.setLowPass(sampleRateDb, crossLowMidCutoff.toDouble(), 0.707)
        
        crossMidHPStr.setHighPass(sampleRateDb, crossLowMidCutoff.toDouble(), 0.707)
        crossMidLPStr.setLowPass(sampleRateDb, crossMidHighCutoff.toDouble(), 0.707)
        
        crossHighHPStr.setHighPass(sampleRateDb, crossMidHighCutoff.toDouble(), 0.707)
    }

    private fun renderLoop() {
        // Render block size (e.g., 256 stereophonic floats)
        val numSamples = bufferSize
        val floatBuffer = FloatArray(numSamples * 2)

        // Record input buffer
        val inputChannels = if (isStereoInput) 2 else 1
        val inputBuffer = FloatArray(numSamples * inputChannels)

        // Envelope smooth times
        var rmsL = 0f
        var rmsR = 0f

        // Crossover peak trackers
        var subRMS = 0f
        var lowRMS = 0f
        var midRMS = 0f
        var highRMS = 0f

        val randomSource = Random(42)

        while (isRunning.get()) {
            val masterL = masterVolL
            val masterR = masterVolR

            // 1. Read actual signal from recording device
            val currentRecord = audioRecord
            var samplesRead = 0
            if (currentRecord != null && currentRecord.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    samplesRead = currentRecord.read(inputBuffer, 0, inputBuffer.size, AudioRecord.READ_NON_BLOCKING)
                } catch (e: Exception) {
                    Log.e("AudioEngine", "Error reading audio samples: ${e.message}")
                }
            }

            // Fill buffer with silence if read failed, or process actual input
            for (i in 0 until numSamples) {
                var rawL = 0.0
                var rawR = 0.0

                if (samplesRead > 0) {
                    if (isStereoInput) {
                        val baseIdx = i * 2
                        if (baseIdx + 1 < inputBuffer.size) {
                            rawL = inputBuffer[baseIdx].toDouble()
                            rawR = inputBuffer[baseIdx + 1].toDouble()
                        }
                    } else {
                        if (i < inputBuffer.size) {
                            val monoVal = inputBuffer[i].toDouble()
                            rawL = monoVal
                            rawR = monoVal
                        }
                    }
                }

                // Apply multi-band signal chain according to current routing matrix
                val chain = routingChain
                for (eff in chain) {
                    when (eff) {
                        "EQ" -> {
                            if (!eqBypass) {
                                // Apply graphic EQ cascade
                                val bandsCount = if (eqMode == 7) 7 else if (eqMode == 15) 15 else 31
                                for (b in 0 until bandsCount) {
                                    rawL = eqBiquadsLeft[b].process(rawL)
                                    rawR = eqBiquadsRight[b].process(rawR)
                                }
                            }
                        }
                        "CROSSOVER" -> {
                            if (!crossoverBypass) {
                                // 4-Way Active filter split
                                val subSig = crossSubLPStr.process((rawL + rawR) / 2.0)
                                
                                val lowSigRaw = crossLowLPStr.process((rawL + rawR) / 2.0)
                                val lowSig = crossLowHPStr.process(lowSigRaw)
                                
                                val midSigRaw = crossMidLPStr.process((rawL + rawR) / 2.0)
                                val midSig = crossMidHPStr.process(midSigRaw)
                                
                                val highSig = crossHighHPStr.process((rawL + rawR) / 2.0)

                                // Tracks bands peaks for active Crossover screen
                                subRMS = (subRMS * 0.99f) + (subSig.toFloat().pow(2) * 0.01f)
                                lowRMS = (lowRMS * 0.99f) + (lowSig.toFloat().pow(2) * 0.01f)
                                midRMS = (midRMS * 0.99f) + (midSig.toFloat().pow(2) * 0.01f)
                                highRMS = (highRMS * 0.99f) + (highSig.toFloat().pow(2) * 0.01f)

                                // Sum back multiplied by individual Crossover faders
                                val summedSig = (subSig * crossSubOut + 
                                                 lowSig * crossLowOut + 
                                                 midSig * crossMidOut + 
                                                 highSig * crossHighOut)
                                                 
                                rawL = summedSig
                                rawR = summedSig
                            }
                        }
                        "COMPRESSOR" -> {
                            if (!compressorBypass) {
                                val inputPower = (rawL * rawL + rawR * rawR) / 2.0
                                val ipDb = if (inputPower > 1e-12) 10.0 * log10(inputPower) else -120.0
                                
                                // Threshold, ratio math
                                val overThresh = ipDb - compThreshold
                                val attCoef = exp(-1.0 / (SAMPLE_RATE * (compAttack / 1000.0)))
                                val relCoef = exp(-1.0 / (SAMPLE_RATE * (compRelease / 1000.0)))

                                val targetEnv = if (overThresh > 0.0) overThresh else 0.0
                                if (targetEnv > compEnvelope) {
                                    compEnvelope = compEnvelope * attCoef + targetEnv * (1.0 - attCoef)
                                } else {
                                    compEnvelope = compEnvelope * relCoef + targetEnv * (1.0 - relCoef)
                                }

                                val gainReductionDb = -compEnvelope * (1.0 - 1.0 / compRatio)
                                val scaleFactor = 10.0.pow(gainReductionDb / 20.0)

                                rawL *= scaleFactor
                                rawR *= scaleFactor
                            }
                        }
                        "WIDENER" -> {
                            if (!widenerBypass) {
                                // Mid side coding: Mid = (L+R)/2, Side = (L-R)/2
                                val mid = (rawL + rawR) / 2.0
                                val side = (rawL - rawR) / 2.0 * wideFactor
                                
                                rawL = mid + side
                                rawR = mid - side
                            }
                        }
                        "LIMITER" -> {
                            if (!limiterBypass) {
                                val ceilingLinear = 10.0.pow(limitCeiling.toDouble() / 20.0)
                                // Soft saturation clamp
                                if (rawL > ceilingLinear) rawL = ceilingLinear + (rawL - ceilingLinear) * 0.05
                                if (rawL < -ceilingLinear) rawL = -ceilingLinear + (rawL + ceilingLinear) * 0.05
                                
                                if (rawR > ceilingLinear) rawR = ceilingLinear + (rawR - ceilingLinear) * 0.05
                                if (rawR < -ceilingLinear) rawR = -ceilingLinear + (rawR + ceilingLinear) * 0.05
                            }
                        }
                    }
                }

                // Master fader volume gains
                rawL *= masterL
                rawR *= masterR

                // Push samples into output float buffer
                floatBuffer[i * 2] = rawL.toFloat()
                floatBuffer[i * 2 + 1] = rawR.toFloat()

                // Calculate output level RMS trackers
                rmsL = (rmsL * 0.999f) + (rawL.toFloat().pow(2) * 0.001f)
                rmsR = (rmsR * 0.999f) + (rawR.toFloat().pow(2) * 0.001f)
            }

            // Expose peak meters to Flows
            volumeL.value = sqrt(rmsL)
            volumeR.value = sqrt(rmsR)

            bandPowerSub.value = sqrt(subRMS)
            bandPowerLow.value = sqrt(lowRMS)
            bandPowerMid.value = sqrt(midRMS)
            bandPowerHigh.value = sqrt(highRMS)

            // Calculate real reactive visualizer spectrum bands from the actual live audio buffer!
            val blockSize = numSamples / 16
            for (index in 0 until 16) {
                var sumSq = 0f
                val startIdx = index * blockSize
                val endIdx = (index + 1) * blockSize
                for (j in startIdx until endIdx) {
                    if (j < numSamples) {
                        val sample = (floatBuffer[j * 2] + floatBuffer[j * 2 + 1]) / 2f
                        sumSq += sample * sample
                    }
                }
                val mean = sumSq / blockSize
                val rms = sqrt(mean)

                // Scale the RMS to make it super visually active and beautiful.
                // Apply a gentle boost so lower inputs are still clearly visualized.
                val targetAmp = (rms * 15.0f + 0.05f).coerceIn(0.05f, 1.2f)

                val currentPowerFactor = if (eqBypass) 1.0f else {
                    val eqBin = (index * eqGains.size / 16).coerceIn(0, eqGains.size - 1)
                    10.0f.pow(eqGains[eqBin] / 20.0f)
                }

                val jitter = (randomSource.nextFloat() * 0.04f) - 0.02f
                val decayedVal = spectrumValues[index] * 0.7f + (targetAmp * currentPowerFactor + jitter).coerceIn(0f, 1.2f) * 0.3f
                spectrumValues[index] = decayedVal.coerceIn(0f, 1.2f)
            }

            // Push updated spectrum stream to visualizer flows
            spectrumFlow.value = spectrumValues.clone()

            // Write processed floats to AudioTrack buffer
            audioTrack?.let { track ->
                if (isRunning.get()) {
                    try {
                        track.write(floatBuffer, 0, floatBuffer.size, AudioTrack.WRITE_NON_BLOCKING)
                    } catch (e: Exception) {
                        Log.e("AudioEngine", "Error writing audio data: ${e.message}")
                    }
                }
            }

            // Yield briefly to limit CPU usage on background render
            val blockMs = (numSamples * 1000L / SAMPLE_RATE).coerceIn(1, 50)
            try {
                Thread.sleep(blockMs)
            } catch (e: InterruptedException) {
                // Thread interrupted
            }
        }
    }
}
