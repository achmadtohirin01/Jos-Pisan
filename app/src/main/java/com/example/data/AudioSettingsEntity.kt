package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_settings")
data class AudioSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val selectedTheme: String = "Glow in the Dark", // "Gradient", "Glow in the Dark", "Blue Green", "Purple Neon"
    val bufferSize: Int = 256,
    val audioDriver: String = "AAudio", // "AAudio", "OpenSL ES"
    val eqMode: Int = 7, // 7, 15, 31
    val masterVolumeL: Float = 0.8f,
    val masterVolumeR: Float = 0.8f,
    val mixerSub: Float = 0.8f,
    val mixerLow: Float = 0.8f,
    val mixerMid: Float = 0.8f,
    val mixerHigh: Float = 0.8f,
    
    // Compressor
    val compBypass: Boolean = false,
    val compThreshold: Float = -20f, // dB (-60 to 0)
    val compRatio: Float = 4.0f, // 1 to 20
    val compAttack: Float = 10f, // ms
    val compRelease: Float = 100f, // ms
    
    // Stereo imager (3D Wide)
    val wideBypass: Boolean = false,
    val wideFactor: Float = 1.0f, // 0.0 to 2.0
    
    // Limiter
    val limitBypass: Boolean = false,
    val limitCeiling: Float = -1.0f, // dB (-10 to 0)
    
    // Crossover Outputs
    val crossSubOut: Float = 0.8f,
    val crossLowOut: Float = 0.8f,
    val crossMidOut: Float = 0.8f,
    val crossHighOut: Float = 0.8f,
    
    // Crossover cutoffs
    val crossSubLowCutoff: Float = 120f,
    val crossLowMidCutoff: Float = 400f,
    val crossMidHighCutoff: Float = 4000f
)
