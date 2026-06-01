package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routing_config")
data class RoutingEntity(
    @PrimaryKey val id: Int = 1,
    val chainOrder: String = "EQ,CROSSOVER,COMPRESSOR,WIDENER,LIMITER", // comma-separated draggable list
    val eqBypass: Boolean = false,
    val crossoverBypass: Boolean = false,
    val compressorBypass: Boolean = false,
    val widenerBypass: Boolean = false,
    val limiterBypass: Boolean = false
)
