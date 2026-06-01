package com.example.dsp

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class GlobalAudioBypassManager private constructor() {
    companion object {
        val instance = GlobalAudioBypassManager()
    }

    private var context: Context? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var visualizer: Visualizer? = null

    val isBypassActive = MutableStateFlow(false)
    val spectrumFlow = MutableStateFlow(FloatArray(16) { 0.05f })

    private val localSpectrumValues = FloatArray(16) { 0.05f }

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun setBypassEnabled(enable: Boolean) {
        if (enable == isBypassActive.value) return
        
        if (enable) {
            try {
                // Instantiate global Equalizer bound to audio session 0 with high priority (1000)
                equalizer = Equalizer(1000, 0).apply {
                    enabled = true
                }
                Log.d("GlobalAudioBypass", "Global Equalizer successfully initialized. Bands: ${equalizer?.numberOfBands}")
            } catch (e: Exception) {
                Log.e("GlobalAudioBypass", "Failed to initialize global Equalizer: ${e.message}")
            }

            try {
                // Instantiate global BassBoost bound to audio session 0
                bassBoost = BassBoost(1000, 0).apply {
                    enabled = true
                }
                Log.d("GlobalAudioBypass", "Global BassBoost successfully initialized.")
            } catch (e: Exception) {
                Log.e("GlobalAudioBypass", "Failed to initialize global BassBoost: ${e.message}")
            }

            try {
                // Instantiate global Visualizer bound to audio session 0 to capture global FFT spectrum
                val captureSize = Visualizer.getCaptureSizeRange()[1] // Try highest capture size
                visualizer = Visualizer(0).apply {
                    this.captureSize = captureSize
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            // Audio waveforms are not used for visualizer bars
                        }

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft == null || fft.isEmpty() || !isBypassActive.value) return
                            processGlobalFft(fft)
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, false, true)
                    enabled = true
                }
                Log.d("GlobalAudioBypass", "Global Visualizer successfully initialized.")
            } catch (e: Exception) {
                Log.e("GlobalAudioBypass", "Failed to initialize global Visualizer: ${e.message}")
            }

            isBypassActive.value = true
            syncCurrentState()
        } else {
            isBypassActive.value = false
            
            // Clean release
            try {
                visualizer?.enabled = false
                visualizer?.release()
            } catch (e: Exception) {
                Log.e("GlobalAudioBypass", "Error releasing visualizer: ${e.message}")
            }
            visualizer = null

            try {
                equalizer?.enabled = false
                equalizer?.release()
            } catch (e: Exception) {
                Log.e("GlobalAudioBypass", "Error releasing equalizer: ${e.message}")
            }
            equalizer = null

            try {
                bassBoost?.enabled = false
                bassBoost?.release()
            } catch (e: Exception) {
                Log.e("GlobalAudioBypass", "Error releasing bassBoost: ${e.message}")
            }
            bassBoost = null

            Log.d("GlobalAudioBypass", "Global Audio Bypass deactivated and resources clean released.")
        }
    }

    private fun processGlobalFft(fft: ByteArray) {
        val n = fft.size
        if (n <= 1) return

        // Process real-time high quality logarithmic spectrum bands from FFT data
        for (band in 0 until 16) {
            val startRatio = (band.toDouble() / 16.0).pow(1.6)
            val endRatio = ((band + 1).toDouble() / 16.0).pow(1.6)
            
            val startBin = (startRatio * (n / 2)).toInt().coerceIn(1, n / 2)
            val endBin = (endRatio * (n / 2)).toInt().coerceIn(startBin + 1, n / 2 + 1)

            var sumMagnitude = 0.0
            var count = 0
            for (bin in startBin until endBin) {
                val realIdx = 2 * bin
                val imagIdx = 2 * bin + 1
                if (imagIdx < n) {
                    val r = fft[realIdx].toDouble()
                    val i = fft[imagIdx].toDouble()
                    val mag = sqrt(r * r + i * i)
                    sumMagnitude += mag
                    count++
                }
            }

            val avgMagnitude = if (count > 0) sumMagnitude / count else 0.0
            
            // Scaled amplitude mapping for responsive beautiful UI bars
            val baseScale = 25.0
            val bandBoost = when {
                band < 3 -> 1.4f  // Bass boost
                band in 3..8 -> 1.0f
                else -> 0.8f
            }
            val scaled = ((avgMagnitude / baseScale) * bandBoost).toFloat().coerceIn(0.05f, 1.2f)

            // Smooth decay integration
            val prev = localSpectrumValues[band]
            val decayed = prev * 0.7f + scaled * 0.3f
            localSpectrumValues[band] = decayed.coerceIn(0.05f, 1.2f)
        }

        // Notify UI collectors
        spectrumFlow.value = localSpectrumValues.clone()
    }

    fun syncCurrentState() {
        val eq = equalizer
        if (eq != null && isBypassActive.value) {
            try {
                // Sync current EQ bands based on AudioEngine's eqGains values
                val gains = AudioEngine.instance.eqGains
                val numBands = eq.numberOfBands.toInt()
                val levelRange = eq.bandLevelRange
                val minLevel = levelRange[0].toInt()
                val maxLevel = levelRange[1].toInt()

                for (i in 0 until numBands) {
                    val ratio = i.toFloat() / (numBands - 1).coerceAtLeast(1)
                    val targetIdx = (ratio * (gains.size - 1)).roundToInt().coerceIn(0, gains.size - 1)
                    val dbGain = gains[targetIdx]

                    // Convert db to milliBels (1 dB = 100 milliBels)
                    val milliBels = (dbGain * 100f).roundToInt().coerceIn(minLevel, maxLevel)
                    eq.setBandLevel(i.toShort(), milliBels.toShort())
                }
                Log.d("GlobalAudioBypass", "EQ bands successfully synchronized to system equalizer!")
            } catch (e: Exception) {
                Log.e("GlobalAudioBypass", "Error syncing EQ bands: ${e.message}")
            }
        }

        val bb = bassBoost
        if (bb != null && bb.strengthSupported && isBypassActive.value) {
            try {
                // Map mixSub fader (representing Bass level, normally 0.0 to 1.5) to bass strength (0 to 1000)
                val subValue = AudioEngine.instance.mixSub
                val strength = (subValue * 700f).roundToInt().coerceIn(0, 1000)
                bb.setStrength(strength.toShort())
                Log.d("GlobalAudioBypass", "Bass Boost synchronized to system. Strength: $strength")
            } catch (e: Exception) {
                Log.e("GlobalAudioBypass", "Error syncing Bass Boost: ${e.message}")
            }
        }
    }
}
