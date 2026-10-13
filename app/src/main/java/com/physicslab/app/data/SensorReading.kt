package com.physicslab.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One persisted sensor reading in the new domain layer.
 *
 * Field semantics:
 *
 * | Field        | Meaning                                                             |
 * |-------------|---------------------------------------------------------------------|
 * | id           | Auto-generated primary key.                                         |
 * | quantity     | [Quantity.name] string — "MAGNETIC_FIELD", "ACCELERATION", "LIGHT". |
 * | name         | User-given or auto-generated label; max 60 chars, pre-trimmed.      |
 * | timestamp    | UTC epoch millis of the *first* sample in the capture window.       |
 * | x, y, z      | Mean axis components over the capture window; **null for LIGHT**.   |
 * | value        | Resultant = sqrt(x²+y²+z²) for vectors; mean lux for LIGHT.        |
 * | sampleCount  | Number of sensor callbacks averaged in the capture window.          |
 * | sensorName   | [android.hardware.Sensor.getName] at capture time.                  |
 * | accuracy     | [android.hardware.SensorManager].SENSOR_STATUS_* at capture end;   |
 * |              | null if the quantity does not report accuracy (e.g. TYPE_LIGHT).   |
 * | updatedAt    | UTC epoch millis when last written; equals [timestamp] on insert,   |
 * |              | updated to rename time on [SensorRepository.rename].               |
 *
 * Timestamps are stored as UTC epoch millis and must only be converted to
 * locale-specific strings at the display layer (never in the domain layer).
 */
@Entity(tableName = "sensor_readings")
data class SensorReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val quantity: String,
    val name: String,
    val timestamp: Long,
    val x: Double?,
    val y: Double?,
    val z: Double?,
    val value: Double,
    val sampleCount: Int,
    val sensorName: String,
    val accuracy: Int?,
    val updatedAt: Long
)
