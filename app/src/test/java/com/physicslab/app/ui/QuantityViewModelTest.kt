package com.physicslab.app.ui

import com.physicslab.app.data.Quantity
import com.physicslab.app.data.StatsResult
import com.physicslab.app.sensor.SensorAccuracy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuantityViewModelTest {

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
    fun quantityViewModel_computesVectorResultant_andSessionStats() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = QuantityViewModel(
            quantity = Quantity.MAGNETIC_FIELD,
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        val magSource = fakeProvider.getFake(Quantity.MAGNETIC_FIELD)

        // Subscribe to liveState (simulating UI repeatOnLifecycle STARTED)
        backgroundScope.launch { viewModel.liveState.collect {} }
        advanceUntilIdle()

        // Sample 1: (0, 3, 4) -> Resultant 5.0
        magSource.emitValues(
            values = floatArrayOf(0f, 3f, 4f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        advanceUntilIdle()

        val state1 = viewModel.liveState.value
        assertEquals(5.0, state1.currentValue!!, 1e-4)
        assertEquals("5.0", state1.formattedHeroValue)
        assertEquals(5.0, state1.minSession!!, 1e-4)
        assertEquals(5.0, state1.maxSession!!, 1e-4)
        assertEquals(5.0, state1.avgSession!!, 1e-4)

        // Sample 2: (0, 6, 8) -> Resultant 10.0
        magSource.emitValues(
            values = floatArrayOf(0f, 6f, 8f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        advanceUntilIdle()

        val state2 = viewModel.liveState.value
        assertEquals(10.0, state2.currentValue!!, 1e-4)
        assertEquals("10.0", state2.formattedHeroValue)
        assertEquals(5.0, state2.minSession!!, 1e-4)
        assertEquals(10.0, state2.maxSession!!, 1e-4)
        assertEquals(7.5, state2.avgSession!!, 1e-4)
    }

    @Test
    fun quantityViewModel_lightSensor_sensibleFormattingAcrossHugeRange() {
        // Values < 10.0: 2 decimals
        assertEquals("0.45", QuantityViewModel.formatMeasurementValue(Quantity.LIGHT, 0.453))
        assertEquals("7.20", QuantityViewModel.formatMeasurementValue(Quantity.LIGHT, 7.20))

        // Values 10.0 .. < 1000.0: 1 decimal
        assertEquals("120.5", QuantityViewModel.formatMeasurementValue(Quantity.LIGHT, 120.54))
        assertEquals("999.9", QuantityViewModel.formatMeasurementValue(Quantity.LIGHT, 999.91))

        // Values >= 1000.0: formatted with grouping separators
        assertEquals("1,250", QuantityViewModel.formatMeasurementValue(Quantity.LIGHT, 1250.0))
        assertEquals("45,000", QuantityViewModel.formatMeasurementValue(Quantity.LIGHT, 45000.0))
        assertEquals("100,500", QuantityViewModel.formatMeasurementValue(Quantity.LIGHT, 100500.0))
    }

    @Test
    fun quantityViewModel_triggersCalibrationHint_whenMagnetometerUnreliable() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = QuantityViewModel(
            quantity = Quantity.MAGNETIC_FIELD,
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        val magSource = fakeProvider.getFake(Quantity.MAGNETIC_FIELD)
        backgroundScope.launch { viewModel.liveState.collect {} }
        advanceUntilIdle()

        magSource.emitValues(
            values = floatArrayOf(10f, 10f, 10f),
            accuracy = SensorAccuracy.UNRELIABLE,
            applyThrottling = false
        )
        advanceUntilIdle()

        val state = viewModel.liveState.value
        assertNotNull(state.calibrationHint)
        assertTrue(state.calibrationHint!!.contains("figure-8", ignoreCase = true))
    }

    @Test
    fun quantityViewModel_executesCaptureWindowWithProgress_andPreventsDoubleSaves() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = QuantityViewModel(
            quantity = Quantity.ACCELERATION,
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        val accelSource = fakeProvider.getFake(Quantity.ACCELERATION)
        backgroundScope.launch { viewModel.liveState.collect {} }
        advanceUntilIdle()

        accelSource.emitValues(
            values = floatArrayOf(0f, 9.81f, 0f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        advanceUntilIdle()

        // 1. Start capture
        viewModel.startCapture("Drop Test 1")

        // First step (at t=100ms)
        advanceTimeBy(101L)
        runCurrent()
        val inProgress = viewModel.captureState.value
        assertTrue(inProgress is CaptureUiState.InProgress)
        assertEquals(10, (inProgress as CaptureUiState.InProgress).percent)

        // Rapid tap test: attempting a second capture while InProgress is ignored
        viewModel.startCapture("Rapid Tap Attempt")

        // Complete the 1s window (1000ms total)
        advanceTimeBy(1000L)
        advanceUntilIdle()

        val completed = viewModel.captureState.value
        assertTrue(completed is CaptureUiState.Success)
        val reading = (completed as CaptureUiState.Success).reading
        assertEquals("Drop Test 1", reading.name)
        assertEquals(9.81, reading.value, 1e-2)

        // Verify saved in repository
        val list = viewModel.historyReadings.value
        assertEquals(1, list.size)
        assertEquals("Drop Test 1", list[0].name)
    }

    @Test
    fun quantityViewModel_saveRenameDelete_crudOperations() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = QuantityViewModel(
            quantity = Quantity.ACCELERATION,
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        val accelSource = fakeProvider.getFake(Quantity.ACCELERATION)
        backgroundScope.launch { viewModel.liveState.collect {} }
        advanceUntilIdle()

        accelSource.emitValues(
            values = floatArrayOf(0f, 9.8f, 0f),
            accuracy = SensorAccuracy.HIGH,
            applyThrottling = false
        )
        advanceUntilIdle()

        // 1. Save reading
        viewModel.startCapture("Drop Test 1")
        advanceTimeBy(1100L)
        advanceUntilIdle()

        val readings = viewModel.historyReadings.value
        assertEquals(1, readings.size)
        val readingId = readings[0].id

        // 2. Rename reading
        val renameSuccess = viewModel.renameReading(readingId, "Drop Test 1 - Calibrated")
        advanceUntilIdle()
        assertTrue(renameSuccess)
        assertEquals("Drop Test 1 - Calibrated", viewModel.historyReadings.value[0].name)

        // 3. Stats verification
        val stats = viewModel.stats.value
        assertTrue(stats is StatsResult.Populated)
        val populated = stats as StatsResult.Populated
        assertEquals(1, populated.count)
        assertEquals(9.8, populated.average, 1e-1)

        // 4. Delete reading
        viewModel.deleteReading(readingId)
        advanceUntilIdle()
        assertEquals(0, viewModel.historyReadings.value.size)
    }

    @Test
    fun quantityViewModel_historyState_distinguishesNoSearchVsSearchingEmptyState() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = QuantityViewModel(
            quantity = Quantity.LIGHT,
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        backgroundScope.launch { viewModel.historyState.collect {} }
        advanceUntilIdle()

        // 1. Initial empty state without search -> NoSearch(emptyList)
        val initial = viewModel.historyState.value
        assertTrue(initial is HistoryUiState.NoSearch)
        assertTrue((initial as HistoryUiState.NoSearch).readings.isEmpty())

        // 2. Add two readings to repository
        val r1 = com.physicslab.app.data.SensorReading(
            name = "Office Desk",
            quantity = Quantity.LIGHT.name,
            timestamp = 1000L,
            x = null,
            y = null,
            z = null,
            value = 450.0,
            sampleCount = 10,
            sensorName = "Test Light Sensor",
            accuracy = null,
            updatedAt = 1000L
        )
        val r2 = com.physicslab.app.data.SensorReading(
            name = "Dark Room",
            quantity = Quantity.LIGHT.name,
            timestamp = 2000L,
            x = null,
            y = null,
            z = null,
            value = 5.0,
            sampleCount = 10,
            sensorName = "Test Light Sensor",
            accuracy = null,
            updatedAt = 2000L
        )
        fakeRepo.saveReading(r1)
        fakeRepo.saveReading(r2)
        advanceUntilIdle()

        val populated = viewModel.historyState.value
        assertTrue(populated is HistoryUiState.NoSearch)
        assertEquals(2, (populated as HistoryUiState.NoSearch).readings.size)

        // 3. Search matching "Office" -> Searching(1 reading, "Office")
        viewModel.setSearchQuery("Office")
        advanceUntilIdle()

        val searchMatched = viewModel.historyState.value
        assertTrue(searchMatched is HistoryUiState.Searching)
        val matchState = searchMatched as HistoryUiState.Searching
        assertEquals("Office", matchState.query)
        assertEquals(1, matchState.readings.size)
        assertEquals("Office Desk", matchState.readings[0].name)

        // 4. Search with no matches -> Searching(0 readings, "Corridor") - distinct from NoSearch!
        viewModel.setSearchQuery("Corridor")
        advanceUntilIdle()

        val searchEmpty = viewModel.historyState.value
        assertTrue(searchEmpty is HistoryUiState.Searching)
        val emptyState = searchEmpty as HistoryUiState.Searching
        assertEquals("Corridor", emptyState.query)
        assertEquals(0, emptyState.readings.size)

        // 5. Clear search query -> returns to NoSearch
        viewModel.setSearchQuery("")
        advanceUntilIdle()

        val cleared = viewModel.historyState.value
        assertTrue(cleared is HistoryUiState.NoSearch)
        assertEquals(2, (cleared as HistoryUiState.NoSearch).readings.size)
    }

    @Test
    fun quantityViewModel_deleteAndRestore_undoFunctionality() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = QuantityViewModel(
            quantity = Quantity.MAGNETIC_FIELD,
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        backgroundScope.launch { viewModel.historyState.collect {} }
        advanceUntilIdle()

        val reading = com.physicslab.app.data.SensorReading(
            name = "Earth Field Bench",
            quantity = Quantity.MAGNETIC_FIELD.name,
            timestamp = 5000L,
            x = 0.0,
            y = 20.0,
            z = 41.5,
            value = 46.2,
            sampleCount = 10,
            sensorName = "Test Magnetometer",
            accuracy = 3,
            updatedAt = 5000L
        )
        val originalId = fakeRepo.saveReading(reading)
        advanceUntilIdle()

        val saved = fakeRepo.getById(originalId)!!
        assertEquals(1, (viewModel.historyState.value as HistoryUiState.NoSearch).readings.size)

        // Delete reading
        viewModel.deleteReading(originalId)
        advanceUntilIdle()
        assertEquals(0, (viewModel.historyState.value as HistoryUiState.NoSearch).readings.size)

        // Undo action: restore reading
        val restoredId = viewModel.restoreReading(saved)
        advanceUntilIdle()

        assertTrue(restoredId > 0)
        val restoredList = (viewModel.historyState.value as HistoryUiState.NoSearch).readings
        assertEquals(1, restoredList.size)
        assertEquals("Earth Field Bench", restoredList[0].name)
        assertEquals(46.2, restoredList[0].value, 1e-4)
    }

    @Test
    fun quantityViewModel_detailedStats_updatesOnInsertRenameDeleteAndUndo() = runTest {
        val fakeRepo = FakeSensorRepository()
        val fakeProvider = FakeSensorSourceProvider()

        val viewModel = QuantityViewModel(
            quantity = Quantity.ACCELERATION,
            repository = fakeRepo,
            sensorProvider = fakeProvider
        )

        backgroundScope.launch { viewModel.detailedStats.collect {} }
        advanceUntilIdle()

        // 1. Initial state: Empty
        val initial = viewModel.detailedStats.value
        assertTrue(initial is QuantityDetailedStats.Empty)

        // 2. Insert two readings
        val r1 = com.physicslab.app.data.SensorReading(
            name = "Rest Gravity",
            quantity = Quantity.ACCELERATION.name,
            timestamp = 1000L,
            x = 0.0,
            y = 0.0,
            z = 9.8,
            value = 9.80,
            sampleCount = 10,
            sensorName = "Accel",
            accuracy = 3,
            updatedAt = 1000L
        )
        val r2 = com.physicslab.app.data.SensorReading(
            name = "Vigorous Shake",
            quantity = Quantity.ACCELERATION.name,
            timestamp = 2000L,
            x = 5.0,
            y = 10.0,
            z = 15.0,
            value = 18.70,
            sampleCount = 10,
            sensorName = "Accel",
            accuracy = 3,
            updatedAt = 2000L
        )

        val id1 = fakeRepo.saveReading(r1)
        val id2 = fakeRepo.saveReading(r2)
        advanceUntilIdle()

        // 3. Verify populated statistics
        val populated = viewModel.detailedStats.value
        assertTrue(populated is QuantityDetailedStats.Populated)
        val stats = populated as QuantityDetailedStats.Populated

        assertEquals(2, stats.count)
        assertEquals(2, stats.totalCount)
        assertEquals(9.80, stats.min.value, 1e-4)
        assertEquals("Rest Gravity", stats.min.name)
        assertEquals(1000L, stats.min.timestamp)

        assertEquals(18.70, stats.max.value, 1e-4)
        assertEquals("Vigorous Shake", stats.max.name)
        assertEquals(2000L, stats.max.timestamp)

        assertEquals((9.80 + 18.70) / 2.0, stats.average, 1e-4)

        // Latest reading is r2 (most recent timestamp 2000L)
        assertEquals(18.70, stats.latest.value, 1e-4)
        assertEquals("Vigorous Shake", stats.latest.name)
        assertEquals(2000L, stats.latest.timestamp)

        assertEquals(2, stats.chartReadings.size)

        // 4. Test Rename: update name of r1
        val renameSuccess = viewModel.renameReading(id1, "Calibrated Gravity")
        advanceUntilIdle()
        assertTrue(renameSuccess)

        val afterRename = viewModel.detailedStats.value as QuantityDetailedStats.Populated
        assertEquals("Calibrated Gravity", afterRename.min.name)

        // 5. Test Delete: remove r2 (the max reading)
        val savedR2 = fakeRepo.getById(id2)!!
        viewModel.deleteReading(id2)
        advanceUntilIdle()

        val afterDelete = viewModel.detailedStats.value as QuantityDetailedStats.Populated
        assertEquals(1, afterDelete.count)
        assertEquals(9.80, afterDelete.max.value, 1e-4)
        assertEquals(9.80, afterDelete.latest.value, 1e-4)
        assertEquals("Calibrated Gravity", afterDelete.latest.name)

        // 6. Test Undo: restore r2
        val restoredId = viewModel.restoreReading(savedR2)
        advanceUntilIdle()
        assertTrue(restoredId > 0)

        val afterUndo = viewModel.detailedStats.value as QuantityDetailedStats.Populated
        assertEquals(2, afterUndo.count)
        assertEquals(18.70, afterUndo.max.value, 1e-4)
        assertEquals(18.70, afterUndo.latest.value, 1e-4)
        assertEquals("Vigorous Shake", afterUndo.latest.name)
        assertEquals((9.80 + 18.70) / 2.0, afterUndo.average, 1e-4)
    }
}
