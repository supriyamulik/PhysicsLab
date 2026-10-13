package com.physicslab.app.sensor

import android.hardware.Sensor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorSourceTest {

    @Test
    fun unsupportedSensor_emitsUnsupportedState() = runTest {
        val fakeSource = FakeSensorSource(
            sensorType = Sensor.TYPE_MAGNETIC_FIELD,
            isSupported = false
        )

        assertFalse(fakeSource.isSupported)
        val state = fakeSource.observe().first()
        assertTrue(state is SensorState.Unsupported)
    }

    @Test
    fun activeSensor_emitsIdleThenActiveWithMetadataAndValues() = runTest {
        val fakeSource = FakeSensorSource(
            sensorType = Sensor.TYPE_ACCELEROMETER,
            isSupported = true
        )

        val states = mutableListOf<SensorState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fakeSource.observe().toList(states)
        }

        // Initially emits Idle with metadata
        assertEquals(1, states.size)
        assertTrue(states[0] is SensorState.Idle)
        val idle = states[0] as SensorState.Idle
        assertEquals("Fake Test Sensor", idle.metadata.name)
        assertEquals(1000f, idle.metadata.maxRange, 1e-4f)

        // Emit Active reading
        val rawValues = floatArrayOf(0.0f, 9.8f, 0.0f)
        fakeSource.emitValues(
            values = rawValues,
            accuracy = SensorAccuracy.HIGH,
            timestampNs = 123456789L
        )

        assertEquals(2, states.size)
        assertTrue(states[1] is SensorState.Active)
        val active = states[1] as SensorState.Active
        assertEquals(0.0f, active.x)
        assertEquals(9.8f, active.y)
        assertEquals(0.0f, active.z)
        assertEquals(SensorAccuracy.HIGH, active.accuracy)
        assertEquals(123456789L, active.timestampNs)
        assertEquals("Fake Test Sensor", active.metadata.name)

        job.cancel()
    }

    @Test
    fun listenerRegistration_isLifecycleBound_noLeaksOnCancellation() = runTest {
        val fakeSource = FakeSensorSource(
            sensorType = Sensor.TYPE_MAGNETIC_FIELD,
            isSupported = true
        )

        assertFalse(fakeSource.listenerRegistered)
        assertEquals(0, fakeSource.registrationCount)
        assertEquals(0, fakeSource.unregistrationCount)

        // Begin collecting (screen visible / resumed)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fakeSource.observe().collect {}
        }

        assertTrue("Listener should be registered while collecting", fakeSource.listenerRegistered)
        assertEquals(1, fakeSource.registrationCount)

        // Cancel collection (screen paused / stopped / destroyed)
        job.cancel()

        assertFalse("Listener MUST be unregistered after cancellation", fakeSource.listenerRegistered)
        assertEquals(1, fakeSource.unregistrationCount)
    }

    @Test
    fun valuesArray_isDefensivelyCloned_protectsAgainstArrayReuse() = runTest {
        val fakeSource = FakeSensorSource(
            sensorType = Sensor.TYPE_MAGNETIC_FIELD,
            isSupported = true
        )

        val states = mutableListOf<SensorState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fakeSource.observe().toList(states)
        }

        val reusableBuffer = floatArrayOf(10f, 20f, 30f)
        fakeSource.emitValues(values = reusableBuffer)

        // Simulate Android framework reusing the same float array for the next event
        reusableBuffer[0] = 999f
        reusableBuffer[1] = 999f
        reusableBuffer[2] = 999f

        val active = states.last() as SensorState.Active
        // The emitted values must retain the original 10, 20, 30 values, not the mutated 999
        assertEquals(10f, active.values[0], 1e-4f)
        assertEquals(20f, active.values[1], 1e-4f)
        assertEquals(30f, active.values[2], 1e-4f)

        job.cancel()
    }

    @Test
    fun accuracyChanges_mapToEnumAndHighlightCalibrationNeed() = runTest {
        val fakeSource = FakeSensorSource(
            sensorType = Sensor.TYPE_MAGNETIC_FIELD,
            isSupported = true
        )

        val states = mutableListOf<SensorState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fakeSource.observe().toList(states)
        }

        // High accuracy
        fakeSource.emitValues(
            values = floatArrayOf(30f, 40f, 0f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        val highState = states.last() as SensorState.Active
        assertEquals(SensorAccuracy.HIGH, highState.accuracy)
        assertFalse(highState.accuracy.needsCalibration)

        // Unreliable accuracy (magnetometer disturbed)
        fakeSource.emitValues(
            values = floatArrayOf(30f, 40f, 0f),
            accuracy = SensorAccuracy.UNRELIABLE,
            applyThrottling = false
        )
        val unreliableState = states.last() as SensorState.Active
        assertEquals(SensorAccuracy.UNRELIABLE, unreliableState.accuracy)
        assertTrue(unreliableState.accuracy.needsCalibration)
        assertTrue(unreliableState.accuracy.description.contains("calibrate"))

        job.cancel()
    }
}
