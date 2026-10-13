package com.physicslab.app.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.physicslab.app.data.AppDatabase
import com.physicslab.app.data.Quantity
import com.physicslab.app.data.SensorReading
import com.physicslab.app.data.StatsResult
import com.physicslab.app.domain.CaptureWindow
import com.physicslab.app.domain.SensorRepository
import com.physicslab.app.domain.SensorRepositoryImpl
import com.physicslab.app.domain.VectorMath
import com.physicslab.app.sensor.DefaultSensorSourceProvider
import com.physicslab.app.sensor.SensorAccuracy
import com.physicslab.app.sensor.SensorMetadata
import com.physicslab.app.sensor.SensorSource
import com.physicslab.app.sensor.SensorSourceProvider
import com.physicslab.app.sensor.SensorState
import com.physicslab.app.ui.view.BarChartView
import com.physicslab.app.ui.view.ChartScaleHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LiveQuantityUiState(
    val quantity: Quantity,
    val currentValue: Double? = null,
    val formattedHeroValue: String = "--",
    val unit: String = "",
    val x: Float? = null,
    val y: Float? = null,
    val z: Float? = null,
    val minSession: Double? = null,
    val avgSession: Double? = null,
    val maxSession: Double? = null,
    val trendPoints: List<Double> = emptyList(),
    val accuracy: SensorAccuracy? = null,
    val metadata: SensorMetadata? = null,
    val isUnsupported: Boolean = false,
    val calibrationHint: String? = null
)

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    data class InProgress(val percent: Int) : CaptureUiState()
    data class Success(val reading: SensorReading) : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}

/**
 * Represents the state of the history list, allowing the UI to distinguish between
 * two different empty states without requiring separate flows.
 */
sealed class HistoryUiState {
    /** The list (possibly empty) when there is no active search. An empty list = no readings saved. */
    data class NoSearch(val readings: List<SensorReading>) : HistoryUiState()
    /** The list (possibly empty) when an active search is running. An empty list = no results. */
    data class Searching(val readings: List<SensorReading>, val query: String) : HistoryUiState()
}

data class ReadingExtremum(
    val value: Double,
    val name: String,
    val timestamp: Long
)

sealed class QuantityDetailedStats {
    object Empty : QuantityDetailedStats()
    data class Populated(
        val count: Int,
        val min: ReadingExtremum,
        val max: ReadingExtremum,
        val average: Double,
        val latest: ReadingExtremum,
        val chartReadings: List<SensorReading>,
        val totalCount: Int
    ) : QuantityDetailedStats()
}

@OptIn(ExperimentalCoroutinesApi::class)
class QuantityViewModel(
    val quantity: Quantity,
    private val repository: SensorRepository,
    private val sensorProvider: SensorSourceProvider,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val sensorSource: SensorSource = sensorProvider.getSource(quantity)
    private var lastMetadata: SensorMetadata? = null

    private val sessionValues = mutableListOf<Double>()

    // Expose hardware metadata even before active reading arrives
    init {
        lastMetadata = sensorSource.metadata
    }

    // Active live stream: stops when UI is not subscribed (grace period of 1000ms for rotation)
    val liveState: StateFlow<LiveQuantityUiState> = sensorSource.observe()
        .map { state -> processSensorState(state) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000L),
            initialValue = LiveQuantityUiState(
                quantity = quantity,
                unit = quantity.unit,
                metadata = sensorSource.metadata,
                isUnsupported = !sensorSource.isSupported
            )
        )

    // Capture state for the ~1s window
    private val _captureState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val captureState: StateFlow<CaptureUiState> = _captureState.asStateFlow()

    // History search query persisted across process death
    private val _searchQuery = MutableStateFlow(savedStateHandle?.get<String>("search_query") ?: "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Reactive history list with empty-state discrimination. */
    val historyState: StateFlow<HistoryUiState> = _searchQuery
        .flatMapLatest { query ->
            val trimmed = query.trim()
            repository.searchByName(quantity, query).map { list ->
                if (trimmed.isEmpty()) {
                    HistoryUiState.NoSearch(list)
                } else {
                    HistoryUiState.Searching(list, trimmed)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState.NoSearch(emptyList()))

    // Keep the old historyReadings for any backward-compat callers (stats, bar chart etc.)
    val historyReadings: StateFlow<List<SensorReading>> = _searchQuery
        .flatMapLatest { query ->
            repository.searchByName(quantity, query)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Statistics from Room DB
    val stats: StateFlow<StatsResult> = repository.observeStats(quantity)
        .stateIn(viewModelScope, SharingStarted.Eagerly, StatsResult.Empty)

    /**
     * Detailed statistics and timeline chart points computed directly from persisted data.
     * Automatically invalidates and updates on insert, rename, delete, and undo.
     */
    val detailedStats: StateFlow<QuantityDetailedStats> = repository.observeByQuantity(quantity)
        .map { allReadings ->
            val valid = allReadings.filter { it.value.isFinite() }
            if (valid.isEmpty()) {
                QuantityDetailedStats.Empty
            } else {
                val latestReading = valid.first()
                val minReading = valid.minByOrNull { it.value }!!
                val maxReading = valid.maxByOrNull { it.value }!!
                val avg = valid.map { it.value }.average()
                val chartList = ChartScaleHelper.capReadings(valid, limit = 50)
                QuantityDetailedStats.Populated(
                    count = valid.size,
                    min = ReadingExtremum(minReading.value, minReading.name, minReading.timestamp),
                    max = ReadingExtremum(maxReading.value, maxReading.name, maxReading.timestamp),
                    average = avg,
                    latest = ReadingExtremum(latestReading.value, latestReading.name, latestReading.timestamp),
                    chartReadings = chartList,
                    totalCount = valid.size
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuantityDetailedStats.Empty)

    // Bar chart data derived from recent saved readings
    val barChartData: StateFlow<List<BarChartView.BarData>> = repository.observeByQuantity(quantity)
        .map { list ->
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            list.take(7).reversed().map { reading ->
                BarChartView.BarData(
                    label = timeFmt.format(Date(reading.timestamp)),
                    value = reading.value
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun processSensorState(state: SensorState): LiveQuantityUiState {
        return when (state) {
            is SensorState.Active -> {
                lastMetadata = state.metadata

                val value = if (quantity.hasComponents) {
                    VectorMath.computeResultant(
                        state.values.getOrNull(0)?.toDouble() ?: 0.0,
                        state.values.getOrNull(1)?.toDouble() ?: 0.0,
                        state.values.getOrNull(2)?.toDouble() ?: 0.0
                    )
                } else {
                    (state.values.getOrNull(0) ?: 0f).toDouble()
                }

                sessionValues.add(value)
                if (sessionValues.size > 100) sessionValues.removeAt(0)

                val minVal = sessionValues.minOrNull()
                val maxVal = sessionValues.maxOrNull()
                val avgVal = if (sessionValues.isNotEmpty()) sessionValues.average() else null

                val calHint = if (quantity == Quantity.MAGNETIC_FIELD && state.accuracy.needsCalibration) {
                    "Move away from magnets or metal objects and gently wave phone in a figure-8 to calibrate."
                } else null

                LiveQuantityUiState(
                    quantity = quantity,
                    currentValue = value,
                    formattedHeroValue = formatMeasurementValue(quantity, value),
                    unit = quantity.unit,
                    x = state.values.getOrNull(0),
                    y = state.values.getOrNull(1),
                    z = state.values.getOrNull(2),
                    minSession = minVal,
                    avgSession = avgVal,
                    maxSession = maxVal,
                    trendPoints = sessionValues.takeLast(40),
                    accuracy = state.accuracy,
                    metadata = state.metadata,
                    isUnsupported = false,
                    calibrationHint = calHint
                )
            }
            is SensorState.Idle -> {
                lastMetadata = state.metadata
                LiveQuantityUiState(
                    quantity = quantity,
                    unit = quantity.unit,
                    metadata = state.metadata,
                    isUnsupported = false
                )
            }
            is SensorState.Unsupported -> {
                LiveQuantityUiState(
                    quantity = quantity,
                    unit = quantity.unit,
                    isUnsupported = true
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        savedStateHandle?.set("search_query", query)
    }

    /**
     * Executes the ~1-second capture window with visible progress.
     * Prevents double-saves from rapid taps by checking state.
     */
    fun startCapture(customName: String) {
        if (_captureState.value is CaptureUiState.InProgress) return // Rapid tap protection

        viewModelScope.launch {
            _captureState.value = CaptureUiState.InProgress(0)
            val window = CaptureWindow(quantity, lastMetadata?.name ?: "Hardware Sensor")

            val totalSteps = 10
            val stepDelayMs = 100L

            for (step in 1..totalSteps) {
                delay(stepDelayMs)
                val currentLive = liveState.value
                if (currentLive.currentValue != null) {
                    if (quantity.hasComponents) {
                        window.addVectorSample(
                            currentLive.x?.toDouble() ?: 0.0,
                            currentLive.y?.toDouble() ?: 0.0,
                            currentLive.z?.toDouble() ?: 0.0,
                            currentLive.accuracy?.statusInt
                        )
                    } else {
                        window.addScalarSample(
                            currentLive.currentValue,
                            currentLive.accuracy?.statusInt
                        )
                    }
                }
                _captureState.value = CaptureUiState.InProgress((step * 100) / totalSteps)
            }

            try {
                val reading = window.buildReading(customName)
                val id = repository.saveReading(reading)
                val savedReading = reading.copy(id = id)
                _captureState.value = CaptureUiState.Success(savedReading)
            } catch (e: Exception) {
                _captureState.value = CaptureUiState.Error(e.localizedMessage ?: "Failed to save reading")
            }
        }
    }

    fun resetCaptureState() {
        _captureState.value = CaptureUiState.Idle
    }

    suspend fun renameReading(id: Long, newName: String): Boolean {
        return repository.renameReading(id, newName)
    }

    suspend fun deleteReading(id: Long) {
        repository.deleteReading(id)
    }

    /**
     * Restores a previously-deleted reading by re-inserting it with the same data.
     * The re-inserted row will get a new auto-generated ID; the old ID is not reused.
     * Returns the new row ID, or -1 on failure.
     */
    suspend fun restoreReading(reading: SensorReading): Long {
        return try {
            // Insert with id=0 so Room auto-generates a new primary key
            repository.saveReading(reading.copy(id = 0L))
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun deleteAllHistory() {
        repository.deleteAllByQuantity(quantity)
    }

    companion object {
        /**
         * Formats measurement value according to physical quantity specifications:
         * - LIGHT: sensible number formatting across huge range (0.0x to 100,000+)
         * - MAGNETIC_FIELD: 1–2 decimals
         * - ACCELERATION: 2 decimals
         */
        fun formatMeasurementValue(quantity: Quantity, value: Double?): String {
            if (value == null || value.isNaN() || value.isInfinite()) return "--"
            return when (quantity) {
                Quantity.LIGHT -> {
                    when {
                        value < 10.0 -> String.format(Locale.US, "%.2f", value)
                        value < 1000.0 -> String.format(Locale.US, "%.1f", value)
                        else -> String.format(Locale.US, "%,d", value.toLong())
                    }
                }
                Quantity.MAGNETIC_FIELD -> {
                    String.format(Locale.US, "%.1f", value)
                }
                Quantity.ACCELERATION -> {
                    String.format(Locale.US, "%.2f", value)
                }
            }
        }
    }

    class Factory(
        private val application: Application,
        private val quantity: Quantity
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return QuantityViewModel(
                quantity = quantity,
                repository = SensorRepositoryImpl(
                    AppDatabase.getInstance(application).sensorReadingDao()
                ),
                sensorProvider = DefaultSensorSourceProvider(application),
                savedStateHandle = savedStateHandle
            ) as T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuantityViewModel(
                quantity = quantity,
                repository = SensorRepositoryImpl(
                    AppDatabase.getInstance(application).sensorReadingDao()
                ),
                sensorProvider = DefaultSensorSourceProvider(application)
            ) as T
        }
    }
}
