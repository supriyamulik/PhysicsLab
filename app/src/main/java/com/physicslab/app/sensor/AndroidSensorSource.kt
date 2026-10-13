package com.physicslab.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Android framework implementation of [SensorSource] backed by [SensorManager].
 *
 * Guarantees:
 * 1. Safe registration: registers via `callbackFlow` using [SensorManager.SENSOR_DELAY_UI].
 * 2. Lifecycle safety: listener is unregistered immediately when collection terminates via [awaitClose].
 * 3. Array isolation: `event.values.clone()` is performed on every event so reused buffer mutation
 *    cannot affect downstream collectors.
 * 4. Off-main-thread processing: incoming sensor events are dispatched to a dedicated background
 *    [HandlerThread], keeping the Main thread completely free of sensor math and rate-limiting.
 * 5. Throttled UI rate: rate-limited to ~10 updates/sec (~100ms) by [SensorThrottler].
 * 6. Graceful degradation: emits [SensorState.Unsupported] when hardware is missing.
 */
class AndroidSensorSource(
    context: Context,
    override val sensorType: Int,
    private val sensorDelay: Int = SensorManager.SENSOR_DELAY_UI,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : SensorSource {

    private val appContext = context.applicationContext

    private val sensorManager: SensorManager? by lazy {
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    private val sensor: Sensor? by lazy {
        sensorManager?.getDefaultSensor(sensorType)
    }

    override val isSupported: Boolean
        get() = sensor != null

    override val metadata: SensorMetadata? by lazy {
        sensor?.let {
            SensorMetadata(
                name = it.name,
                vendor = it.vendor,
                maxRange = it.maximumRange,
                resolution = it.resolution
            )
        }
    }

    override fun observe(throttleIntervalMs: Long): Flow<SensorState> = callbackFlow {
        val currentSm = sensorManager
        val currentSensor = sensor
        val currentMetadata = metadata

        if (currentSm == null || currentSensor == null || currentMetadata == null) {
            trySend(SensorState.Unsupported)
            close()
            return@callbackFlow
        }

        // Emit Idle state with hardware metadata initially
        trySend(SensorState.Idle(currentMetadata))

        val throttler = SensorThrottler(throttleIntervalMs)
        var currentAccuracy = SensorAccuracy.HIGH

        // Dedicated background HandlerThread ensures sensor event dispatch is off the Main thread
        val handlerThread = HandlerThread("SensorDispatchThread-$sensorType").apply { start() }
        val handler = Handler(handlerThread.looper)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != sensorType) return

                val now = System.currentTimeMillis()
                if (throttler.shouldEmit(now)) {
                    // Critical: clone values array as Android reuses the array instance
                    val copiedValues = event.values.clone()
                    val activeState = SensorState.Active(
                        values = copiedValues,
                        accuracy = currentAccuracy,
                        timestampNs = event.timestamp,
                        metadata = currentMetadata
                    )
                    trySend(activeState)
                }
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {
                if (s?.type == sensorType) {
                    currentAccuracy = SensorAccuracy.fromStatus(accuracy)
                }
            }
        }

        val registered = currentSm.registerListener(listener, currentSensor, sensorDelay, handler)
        if (!registered) {
            trySend(SensorState.Unsupported)
            handlerThread.quitSafely()
            close()
            return@callbackFlow
        }

        // Clean up: unregister listener and stop background looper as soon as collection stops
        awaitClose {
            currentSm.unregisterListener(listener)
            handlerThread.quitSafely()
        }
    }.flowOn(backgroundDispatcher)
}
