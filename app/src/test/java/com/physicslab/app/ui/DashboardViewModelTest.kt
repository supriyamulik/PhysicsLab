package com.physicslab.app.ui

import com.physicslab.app.data.Quantity
import com.physicslab.app.data.SensorReading
import com.physicslab.app.sensor.SensorAccuracy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dashboardViewModel_updatesSensorStates_andComputesVectorMagnitudes() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        // Subscribe to Dashboard flows (simulating active UI observation)
        backgroundScope.launch { viewModel.magneticState.collect {} }
        backgroundScope.launch { viewModel.accelState.collect {} }
        backgroundScope.launch { viewModel.lightState.collect {} }
        advanceUntilIdle()

        // Emit magnetic field: (3.0, 4.0, 0.0) -> Resultant 5.0 µT
        val magSource = fakeProvider.getFake(Quantity.MAGNETIC_FIELD)
        magSource.emitValues(
            values = floatArrayOf(3.0f, 4.0f, 0.0f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        advanceUntilIdle()

        val magState = viewModel.magneticState.value
        assertEquals("Magnetic Field", magState.title)
        assertNotNull(magState.currentValue)
        assertEquals(5.0, magState.currentValue!!, 1e-4)
        assertTrue(magState.componentsText.contains("X: 3.0"))
        assertTrue(magState.componentsText.contains("Y: 4.0"))
        assertEquals(1, magState.trendPoints.size)

        // Emit acceleration: (0.0, 9.8f, 0.0)
        val accelSource = fakeProvider.getFake(Quantity.ACCELERATION)
        accelSource.emitValues(
            values = floatArrayOf(0.0f, 9.8f, 0.0f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        advanceUntilIdle()

        val accelState = viewModel.accelState.value
        assertEquals("Acceleration", accelState.title)
        assertEquals(9.8, accelState.currentValue!!, 1e-2)

        // Emit light: 150 lx
        val lightSource = fakeProvider.getFake(Quantity.LIGHT)
        lightSource.emitValues(
            values = floatArrayOf(150f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        advanceUntilIdle()

        val lightState = viewModel.lightState.value
        assertEquals("Light", lightState.title)
        assertEquals(150.0, lightState.currentValue!!, 1e-4)
    }

    @Test
    fun dashboardViewModel_reflectsSavedCapturesCount() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        fakeRepo.saveReading(
            SensorReading(
                id = 0,
                quantity = Quantity.MAGNETIC_FIELD.name,
                name = "Reading 1",
                timestamp = 1000L,
                x = 1.0, y = 2.0, z = 3.0,
                value = 3.74,
                sampleCount = 10,
                sensorName = "Test",
                accuracy = 3,
                updatedAt = 1000L
            )
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.totalSavedCaptures.value)
    }

    @Test
    fun dashboardViewModel_persistedSummaries_emptyInitially_andUpdatesOnSave() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        backgroundScope.launch { viewModel.magneticSummary.collect {} }
        backgroundScope.launch { viewModel.accelSummary.collect {} }
        backgroundScope.launch { viewModel.lightSummary.collect {} }
        advanceUntilIdle()

        // Verify initial empty state
        val magSummaryEmpty = viewModel.magneticSummary.value
        assertEquals(0, magSummaryEmpty.savedCount)
        assertNull(magSummaryEmpty.latestValue)
        assertEquals("--", magSummaryEmpty.latestFormattedValue)
        assertTrue(magSummaryEmpty.isSupported)

        // Save a reading for Magnetic Field
        fakeRepo.saveReading(
            SensorReading(
                id = 0,
                quantity = Quantity.MAGNETIC_FIELD.name,
                name = "Desk Test",
                timestamp = 1600000000000L,
                x = 10.0, y = 20.0, z = 30.0,
                value = 37.416,
                sampleCount = 100,
                sensorName = "Magnetometer",
                accuracy = 3,
                updatedAt = 1600000000000L
            )
        )
        advanceUntilIdle()

        val magSummaryAfter = viewModel.magneticSummary.value
        assertEquals(1, magSummaryAfter.savedCount)
        assertNotNull(magSummaryAfter.latestValue)
        assertEquals(37.416, magSummaryAfter.latestValue!!, 1e-3)
        assertEquals("Desk Test", magSummaryAfter.latestName)
        assertNotNull(magSummaryAfter.latestDate)
    }

    @Test
    fun dashboardViewModel_unsupportedSensorReflectedInSummary() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()
        fakeProvider.getFake(Quantity.LIGHT).isSupported = false

        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        backgroundScope.launch { viewModel.lightSummary.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.lightSummary.value.isSupported)
    }
}
