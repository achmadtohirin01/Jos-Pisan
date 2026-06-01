package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.dsp.AudioEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = AudioRepository(db.audioDao())
    val audioEngine = AudioEngine.instance

    // Expose audio engine real time states to Compose
    val isPlaying = audioEngine.isPlaying
    val volumeL = audioEngine.volumeL
    val volumeR = audioEngine.volumeR
    val spectrumFlow = audioEngine.spectrumFlow
    val bandPowerSub = audioEngine.bandPowerSub
    val bandPowerLow = audioEngine.bandPowerLow
    val bandPowerMid = audioEngine.bandPowerMid
    val bandPowerHigh = audioEngine.bandPowerHigh

    // Room Database states
    val settingsState: StateFlow<AudioSettingsEntity> = repository.settingsFlow
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AudioSettingsEntity()
        )

    val presetsState: StateFlow<List<AudioPresetEntity>> = repository.allPresetsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val routingState: StateFlow<RoutingEntity> = repository.routingFlow
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RoutingEntity()
        )

    val selectedPresetName = MutableStateFlow("Flat")

    init {
        // Sync database configurations into AudioEngine upon launch
        viewModelScope.launch {
            // Wait for DB to populate
            settingsState.collectLatest { settings ->
                syncSettingsToEngine(settings)
            }
        }
        viewModelScope.launch {
            routingState.collectLatest { routing ->
                syncRoutingToEngine(routing)
            }
        }
    }

    private fun syncSettingsToEngine(settings: AudioSettingsEntity) {
        audioEngine.driverName = settings.audioDriver
        audioEngine.bufferSize = settings.bufferSize
        audioEngine.masterVolL = settings.masterVolumeL
        audioEngine.masterVolR = settings.masterVolumeR
        
        // Mixer
        audioEngine.mixSub = settings.mixerSub
        audioEngine.mixLow = settings.mixerLow
        audioEngine.mixMid = settings.mixerMid
        audioEngine.mixHigh = settings.mixerHigh
        
        // EQ
        audioEngine.eqMode = settings.eqMode
        
        // Compressor
        audioEngine.compressorBypass = settings.compBypass
        audioEngine.compThreshold = settings.compThreshold
        audioEngine.compRatio = settings.compRatio
        audioEngine.compAttack = settings.compAttack
        audioEngine.compRelease = settings.compRelease
        
        // Widener
        audioEngine.widenerBypass = settings.wideBypass
        audioEngine.wideFactor = settings.wideFactor
        
        // Limiter
        audioEngine.limiterBypass = settings.limitBypass
        audioEngine.limitCeiling = settings.limitCeiling
        
        // Crossover
        audioEngine.crossSubOut = settings.crossSubOut
        audioEngine.crossLowOut = settings.crossLowOut
        audioEngine.crossMidOut = settings.crossMidOut
        audioEngine.crossHighOut = settings.crossHighOut
        
        audioEngine.crossSubLowCutoff = settings.crossSubLowCutoff
        audioEngine.crossLowMidCutoff = settings.crossLowMidCutoff
        audioEngine.crossMidHighCutoff = settings.crossMidHighCutoff

        audioEngine.updateCrossoverFilters()
        audioEngine.updateEqFilters()
    }

    private fun syncRoutingToEngine(routing: RoutingEntity) {
        val list = routing.chainOrder.split(",").filter { it.isNotEmpty() }
        audioEngine.routingChain = list
        audioEngine.eqBypass = routing.eqBypass
        audioEngine.crossoverBypass = routing.crossoverBypass
        audioEngine.compressorBypass = routing.compressorBypass
        audioEngine.widenerBypass = routing.widenerBypass
        audioEngine.limiterBypass = routing.limiterBypass
    }

    // Toggle real-time synthesized engine play/pause
    fun togglePlayback() {
        audioEngine.toggle()
    }

    fun updateTheme(themeName: String) {
        val current = settingsState.value
        updateSettings(current.copy(selectedTheme = themeName))
    }

    fun updateAudioDriver(driver: String) {
        val current = settingsState.value
        updateSettings(current.copy(audioDriver = driver))
    }

    fun updateBufferSize(size: Int) {
        val current = settingsState.value
        updateSettings(current.copy(bufferSize = size))
    }

    fun updateMasterVolumes(volL: Float, volR: Float) {
        val current = settingsState.value
        updateSettings(current.copy(masterVolumeL = volL, masterVolumeR = volR))
    }

    fun updateMixerFaders(sub: Float, low: Float, mid: Float, high: Float) {
        val current = settingsState.value
        updateSettings(current.copy(
            mixerSub = sub,
            mixerLow = low,
            mixerMid = mid,
            mixerHigh = high
        ))
    }

    fun updateCrossoverGains(subOut: Float, lowOut: Float, midOut: Float, highOut: Float) {
        val current = settingsState.value
        updateSettings(current.copy(
            crossSubOut = subOut,
            crossLowOut = lowOut,
            crossMidOut = midOut,
            crossHighOut = highOut
        ))
    }

    fun updateCrossoverCutoffs(subLow: Float, lowMid: Float, midHigh: Float) {
        val current = settingsState.value
        updateSettings(current.copy(
            crossSubLowCutoff = subLow,
            crossLowMidCutoff = lowMid,
            crossMidHighCutoff = midHigh
        ))
    }

    fun updateEqMode(mode: Int) {
        val current = settingsState.value
        updateSettings(current.copy(eqMode = mode))
        
        // Reset gain adjustments corresponding to newly selected band size
        viewModelScope.launch {
            val preset = repository.getPreset(selectedPresetName.value) ?: return@launch
            val gainsString = when (mode) {
                7 -> preset.gains7
                15 -> preset.gains15
                else -> preset.gains31
            }
            val floats = gainsString.split(",").map { it.toFloatOrNull() ?: 0f }.toFloatArray()
            audioEngine.eqGains = floats
            audioEngine.updateEqFilters()
        }
    }

    fun updateEqSlider(index: Int, gainValue: Float) {
        viewModelScope.launch {
            val currentSettings = settingsState.value
            val presetName = selectedPresetName.value
            val preset = repository.getPreset(presetName)
            if (preset != null) {
                val mode = currentSettings.eqMode
                
                // Read respective array, update, save
                val gainsStr = when (mode) {
                    7 -> preset.gains7
                    15 -> preset.gains15
                    else -> preset.gains31
                }
                val gainsArr = gainsStr.split(",").map { it.toFloatOrNull() ?: 0f }.toFloatArray()
                if (index < gainsArr.size) {
                    gainsArr[index] = gainValue
                }
                
                val updatedPreset = when (mode) {
                    7 -> preset.copy(gains7 = gainsArr.joinToString(","))
                    15 -> preset.copy(gains15 = gainsArr.joinToString(","))
                    else -> preset.copy(gains31 = gainsArr.joinToString(","))
                }

                audioEngine.eqGains = gainsArr
                audioEngine.updateEqFilters()

                repository.savePreset(updatedPreset)
            }
        }
    }

    fun selectPreset(name: String) {
        selectedPresetName.value = name
        viewModelScope.launch {
            val preset = repository.getPreset(name)
            if (preset != null) {
                val mode = settingsState.value.eqMode
                val gainsStr = when (mode) {
                    7 -> preset.gains7
                    15 -> preset.gains15
                    else -> preset.gains31
                }
                val gainsArr = gainsStr.split(",").map { it.toFloatOrNull() ?: 0f }.toFloatArray()
                audioEngine.eqGains = gainsArr
                audioEngine.updateEqFilters()
            }
        }
    }

    fun createCustomPreset(name: String) {
        viewModelScope.launch {
            val currentSettings = settingsState.value
            val mode = currentSettings.eqMode
            
            // Re-use current engine gains to seed new custom preset
            val currentGains = audioEngine.eqGains.joinToString(",")
            val default7 = if (mode == 7) currentGains else "0.0,0.0,0.0,0.0,0.0,0.0,0.0"
            val default15 = if (mode == 15) currentGains else "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0"
            val default31 = if (mode == 31) currentGains else List(31) { "0.0" }.joinToString(",")

            val newPreset = AudioPresetEntity(
                name = name,
                isSystem = false,
                gains7 = default7,
                gains15 = default15,
                gains31 = default31
            )
            repository.savePreset(newPreset)
            selectedPresetName.value = name
        }
    }

    fun deletePreset(name: String) {
        viewModelScope.launch {
            repository.deletePreset(name)
            selectedPresetName.value = "Flat"
            selectPreset("Flat")
        }
    }

    // Accessories
    fun updateCompressorConfig(bypass: Boolean? = null, thresh: Float? = null, ratio: Float? = null, attack: Float? = null, release: Float? = null) {
        val current = settingsState.value
        updateSettings(current.copy(
            compBypass = bypass ?: current.compBypass,
            compThreshold = thresh ?: current.compThreshold,
            compRatio = ratio ?: current.compRatio,
            compAttack = attack ?: current.compAttack,
            compRelease = release ?: current.compRelease
        ))
    }

    fun updateLimiterConfig(bypass: Boolean? = null, ceiling: Float? = null) {
        val current = settingsState.value
        updateSettings(current.copy(
            limitBypass = bypass ?: current.limitBypass,
            limitCeiling = ceiling ?: current.limitCeiling
        ))
    }

    fun updateStereoImager(bypass: Boolean? = null, factor: Float? = null) {
        val current = settingsState.value
        updateSettings(current.copy(
            wideBypass = bypass ?: current.wideBypass,
            wideFactor = factor ?: current.wideFactor
        ))
    }

    // Routing Matrix settings
    fun setBypassState(effName: String, bypassed: Boolean) {
        viewModelScope.launch {
            val r = routingState.value
            val updated = when (effName) {
                "EQ" -> r.copy(eqBypass = bypassed)
                "CROSSOVER" -> r.copy(crossoverBypass = bypassed)
                "COMPRESSOR" -> r.copy(compressorBypass = bypassed)
                "WIDENER" -> r.copy(widenerBypass = bypassed)
                "LIMITER" -> r.copy(limiterBypass = bypassed)
                else -> r
            }
            repository.saveRouting(updated)
        }
    }

    fun moveEffectUp(effName: String) {
        viewModelScope.launch {
            val r = routingState.value
            val order = r.chainOrder.split(",").filter { it.isNotEmpty() }.toMutableList()
            val index = order.indexOf(effName)
            if (index > 0) {
                val temp = order[index - 1]
                order[index - 1] = effName
                order[index] = temp
                val updatedStr = order.joinToString(",")
                repository.saveRouting(r.copy(chainOrder = updatedStr))
            }
        }
    }

    fun moveEffectDown(effName: String) {
        viewModelScope.launch {
            val r = routingState.value
            val order = r.chainOrder.split(",").filter { it.isNotEmpty() }.toMutableList()
            val index = order.indexOf(effName)
            if (index in 0 until order.size - 1) {
                val temp = order[index + 1]
                order[index + 1] = effName
                order[index] = temp
                val updatedStr = order.joinToString(",")
                repository.saveRouting(r.copy(chainOrder = updatedStr))
            }
        }
    }

    private fun updateSettings(settings: AudioSettingsEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(settings)
        }
    }
}
