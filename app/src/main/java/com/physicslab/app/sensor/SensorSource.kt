package com.physicslab.app.sensor

import kotlinx.coroutines.flow.Flow

/**
 * Clean hardware abstraction hiding Android [android.hardware.SensorManager] behind an interface.
 *
 * Allows unit tests to inject fakes without Android framework dependencies.
 */
interface SensorSource {

    /** Android sensor type constant (e.g. [android.hardware.Sensor.TYPE_MAGNETIC_FIELD]). */
    val sensorType: Int

    /** True if the sensor hardware is present on the current device. */
    val isSupported: Boolean

    /** Hardware metadata (name, vendor, maxRange, resolution), or null if unsupported. */
    val metadata: SensorMetadata?

    /**
     * Exposes a reactive stream of [SensorState] transitions.
     *
     * Behavior:
     * - Emits [SensorState.Unsupported] immediately if the device lacks this sensor.
     * - Emits [SensorState.Active] with raw sensor data copied defensively and throttled to [throttleIntervalMs].
     * - Listener registration is bound to Flow collection: registering on collect and unregistering on cancellation.
     *
     * @param throttleIntervalMs Minimum interval between updates in milliseconds (~100 ms for ~10 updates/sec).
     */
    fun observe(throttleIntervalMs: Long = SensorThrottler.DEFAULT_THROTTLE_INTERVAL_MS): Flow<SensorState>
}
