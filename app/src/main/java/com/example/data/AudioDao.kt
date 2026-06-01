package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {
    // Settings
    @Query("SELECT * FROM audio_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AudioSettingsEntity?>

    @Query("SELECT * FROM audio_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AudioSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AudioSettingsEntity)

    // Presets
    @Query("SELECT * FROM audio_presets")
    fun getAllPresetsFlow(): Flow<List<AudioPresetEntity>>

    @Query("SELECT * FROM audio_presets WHERE name = :name LIMIT 1")
    suspend fun getPreset(name: String): AudioPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreset(preset: AudioPresetEntity)

    @Query("DELETE FROM audio_presets WHERE name = :name")
    suspend fun deletePreset(name: String)

    // Routing
    @Query("SELECT * FROM routing_config WHERE id = 1 LIMIT 1")
    fun getRoutingFlow(): Flow<RoutingEntity?>

    @Query("SELECT * FROM routing_config WHERE id = 1 LIMIT 1")
    suspend fun getRouting(): RoutingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRouting(routing: RoutingEntity)
}
