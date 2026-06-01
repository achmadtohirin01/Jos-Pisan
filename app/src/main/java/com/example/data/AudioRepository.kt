package com.example.data

import kotlinx.coroutines.flow.Flow

class AudioRepository(private val audioDao: AudioDao) {
    
    // Settings Flow and Operations
    val settingsFlow: Flow<AudioSettingsEntity?> = audioDao.getSettingsFlow()

    suspend fun getSettings(): AudioSettingsEntity? {
        return audioDao.getSettings()
    }

    suspend fun saveSettings(settings: AudioSettingsEntity) {
        audioDao.saveSettings(settings)
    }

    // Presets Flow and Operations
    val allPresetsFlow: Flow<List<AudioPresetEntity>> = audioDao.getAllPresetsFlow()

    suspend fun getPreset(name: String): AudioPresetEntity? {
        return audioDao.getPreset(name)
    }

    suspend fun savePreset(preset: AudioPresetEntity) {
        audioDao.savePreset(preset)
    }

    suspend fun deletePreset(name: String) {
        audioDao.deletePreset(name)
    }

    // Routing Flow and Operations
    val routingFlow: Flow<RoutingEntity?> = audioDao.getRoutingFlow()

    suspend fun getRouting(): RoutingEntity? {
        return audioDao.getRouting()
    }

    suspend fun saveRouting(routing: RoutingEntity) {
        audioDao.saveRouting(routing)
    }
}
