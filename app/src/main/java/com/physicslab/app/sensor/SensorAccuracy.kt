package com.physicslab.app.sensor

import android.hardware.SensorManager

/**
 * Clean enum representation of hardware sensor accuracy reported by
 * [android.hardware.SensorEventListener.onAccuracyChanged].
 *
 * Mapped from Android's `SensorManager.SENSOR_STATUS_*` constants.
 */
enum class SensorAccuracy(val statusInt: Int, val description: String) {
    /** Sensor reading is unreliable; data should not be trusted without calibration. */
    UNRELIABLE(SensorManager.SENSOR_STATUS_UNRELIABLE, "Unreliable — calibrate device away from magnetic sources"),

    /** Low accuracy; device needs calibration movement (e.g. figure-8 motion). */
    LOW(SensorManager.SENSOR_STATUS_ACCURACY_LOW, "Low accuracy — wave phone in a figure-8 to calibrate"),

    /** Medium accuracy; suitable for standard measurements. */
    MEDIUM(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM, "Medium accuracy"),

    /** High accuracy; sensor is fully calibrated. */
    HIGH(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, "High accuracy");

    /**
     * True if the sensor (especially the magnetometer) needs calibration or is subject
     * to magnetic distortion.
     */
    val needsCalibration: Boolean
        get() = this == UNRELIABLE || this == LOW

    companion object {
        /**
         * Maps an Android sensor accuracy integer to [SensorAccuracy].
         */
        fun fromStatus(status: Int): SensorAccuracy = when (status) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HIGH
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> LOW
            SensorManager.SENSOR_STATUS_UNRELIABLE -> UNRELIABLE
            else -> UNRELIABLE
        }
    }
}
