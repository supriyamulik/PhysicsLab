package com.physicslab.app.sensor

import android.hardware.Sensor
import com.physicslab.app.data.Quantity
import com.physicslab.app.domain.CaptureWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureWindowSensorIntegrationTest {

    @Test
    fun captureWindow_aggregatesActiveSensorStates_andComputesVectorResultant() = runTest {
        val fakeMagnetometer = FakeSensorSource(
            sensorType = Sensor.TYPE_MAGNETIC_FIELD,
            metadata = SensorMetadata("Test Mag", "Vendor", 500f, 0.1f)
        )

        val window = CaptureWindow(Quantity.MAGNETIC_FIELD, "Test Mag")

        val states = mutableListOf<SensorState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fakeMagnetometer.observe().toList(states)
        }

        // Emit sample 1: (2.0, 3.0, 0.0) at t=1000
        fakeMagnetometer.emitValues(
            values = floatArrayOf(2.0f, 3.0f, 0.0f),
            accuracy = SensorAccuracy.MEDIUM,
            nowMs = 1000L,
            applyThrottling = false
        )
        val state1 = states.last() as SensorState.Active
        window.addActiveState(state1, timestampEpochMillis = 1000L)

        // Emit sample 2: (4.0, 5.0, 0.0) at t=1050
        fakeMagnetometer.emitValues(
            values = floatArrayOf(4.0f, 5.0f, 0.0f),
            accuracy = SensorAccuracy.HIGH,
            nowMs = 1050L,
            applyThrottling = false
        )
        val state2 = states.last() as SensorState.Active
        window.addActiveState(state2, timestampEpochMillis = 1050L)

        // Window completes:
        // Mean X = (2+4)/2 = 3.0
        // Mean Y = (3+5)/2 = 4.0
        // Mean Z = (0+0)/2 = 0.0
        // Resultant B = sqrt(3² + 4² + 0²) = 5.0 µT
        val reading = window.buildReading(customName = "Magnetic Lab Run")

        assertEquals(2, reading.sampleCount)
        assertEquals(3.0, reading.x!!, 1e-6)
        assertEquals(4.0, reading.y!!, 1e-6)
        assertEquals(0.0, reading.z!!, 1e-6)
        assertEquals(5.0, reading.value, 1e-6)
        assertEquals(1000L, reading.timestamp) // Preserves first sample timestamp
        assertEquals(SensorAccuracy.HIGH.statusInt, reading.accuracy) // Preserves latest accuracy
        assertEquals("Magnetic Lab Run", reading.name)

        job.cancel()
    }

    @Test
    fun captureWindow_aggregatesLightSensorStates_andComputesScalarMean() = runTest {
        val fakeLight = FakeSensorSource(
            sensorType = Sensor.TYPE_LIGHT,
            metadata = SensorMetadata("Test Light", "Vendor", 100000f, 1f)
        )

        val window = CaptureWindow(Quantity.LIGHT, "Test Light")

        val states = mutableListOf<SensorState>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fakeLight.observe().toList(states)
        }

        // Emit lux 200
        fakeLight.emitValues(
            values = floatArrayOf(200f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        window.addActiveState(states.last() as SensorState.Active, timestampEpochMillis = 2000L)

        // Emit lux 400
        fakeLight.emitValues(
            values = floatArrayOf(400f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        window.addActiveState(states.last() as SensorState.Active, timestampEpochMillis = 2500L)

        val reading = window.buildReading()

        assertEquals(2, reading.sampleCount)
        assertNull(reading.x)
        assertNull(reading.y)
        assertNull(reading.z)
        assertEquals(300.0, reading.value, 1e-6) // Mean lux (200 + 400) / 2
        assertEquals(2000L, reading.timestamp)

        job.cancel()
    }
}
