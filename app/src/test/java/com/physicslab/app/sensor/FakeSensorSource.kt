package com.physicslab.app.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation of [SensorSource] for JVM unit tests.
 *
 * Lives ONLY inside test source sets (per Non-Negotiable Rules).
 */
class FakeSensorSource(
    override val sensorType: Int,
    override var isSupported: Boolean = true,
    override val metadata: SensorMetadata? = if (isSupported) SensorMetadata(
        name = "Fake Test Sensor",
        vendor = "PhysicsLab Test",
        maxRange = 1000f,
        resolution = 0.01f
    ) else null
) : SensorSource {

    val sharedEvents = MutableSharedFlow<SensorState>(replay = 1)
    private val throttler = SensorThrottler()

    /** Tracks whether an active subscriber is currently observing this source. */
    var listenerRegistered: Boolean = false
        private set

    /** Total number of times listeners were registered during test runs. */
    var registrationCount: Int = 0
        private set

    /** Total number of times listeners were released during test runs. */
    var unregistrationCount: Int = 0
        private set

    fun emitState(state: SensorState) {
        sharedEvents.tryEmit(state)
    }

    fun emitValues(
        values: FloatArray,
        accuracy: SensorAccuracy = SensorAccuracy.HIGH,
        timestampNs: Long = System.nanoTime(),
        nowMs: Long = System.currentTimeMillis(),
        applyThrottling: Boolean = true
    ): Boolean {
        if (!isSupported || metadata == null) {
            emitState(SensorState.Unsupported)
            return false
        }
        if (!applyThrottling || throttler.shouldEmit(nowMs)) {
            emitState(
                SensorState.Active(
                    values = values.clone(),
                    accuracy = accuracy,
                    timestampNs = timestampNs,
                    metadata = metadata
                )
            )
            return true
        }
        return false
    }

    override fun observe(throttleIntervalMs: Long): Flow<SensorState> = flow {
        listenerRegistered = true
        registrationCount++
        try {
            if (!isSupported || metadata == null) {
                emit(SensorState.Unsupported)
            } else {
                emit(SensorState.Idle(metadata))
                emitAll(sharedEvents)
            }
        } finally {
            listenerRegistered = false
            unregistrationCount++
        }
    }
}
