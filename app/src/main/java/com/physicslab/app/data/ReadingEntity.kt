package com.physicslab.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged sensor reading. sensorType is a short machine key (e.g. "GYROSCOPE"),
 * label is the human-readable quantity name (e.g. "Angular velocity"), value is the
 * numeric reading, unit is its physical unit, and timestamp is System.currentTimeMillis().
 */
@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sensorType: String,
    val label: String,
    val value: Double,
    val unit: String,
    val timestamp: Long
)
