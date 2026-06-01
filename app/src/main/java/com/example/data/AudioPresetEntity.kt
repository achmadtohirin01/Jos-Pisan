package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_presets")
data class AudioPresetEntity(
    @PrimaryKey val name: String, // e.g. "Flat", "Dangdut", "Sholawat", "Vocal", "Rock", "Pop", "Electronic", "EDM", "Cinema", or Custom names
    val isSystem: Boolean = false,
    val gains7: String = "0.0,0.0,0.0,0.0,0.0,0.0,0.0", // Comma separated gains for 7 bands (-12 to 12)
    val gains15: String = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0", // Comma separated gains for 15 bands
    val gains31: String = "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0" // Comma separated gains for 31 bands
)
