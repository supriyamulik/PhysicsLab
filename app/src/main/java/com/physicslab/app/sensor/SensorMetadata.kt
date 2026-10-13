package com.physicslab.app.sensor

/**
 * Immutable hardware sensor metadata exposed alongside live sensor states.
 *
 * Extracted directly from [android.hardware.Sensor]:
 * - [name]: Human-readable sensor name, e.g. "LIS3DH 3-axis Accelerometer"
 * - [vendor]: Hardware manufacturer / vendor, e.g. "STMicroelectronics"
 * - [maxRange]: Maximum range of the sensor in the sensor's unit
 * - [resolution]: Smallest difference between two values the sensor can measure
 */
data class SensorMetadata(
    val name: String,
    val vendor: String,
    val maxRange: Float,
    val resolution: Float
)
