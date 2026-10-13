package com.physicslab.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.physicslab.app.data.AppDatabase
import com.physicslab.app.data.Quantity
import com.physicslab.app.domain.SensorRepository
import com.physicslab.app.domain.SensorRepositoryImpl
import com.physicslab.app.domain.VectorMath
import com.physicslab.app.sensor.DefaultSensorSourceProvider
import com.physicslab.app.sensor.SensorSourceProvider
import com.physicslab.app.sensor.SensorState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuantityDashboardSummary(
    val quantity: Quantity,
    val isSupported: Boolean,
    val savedCount: Int = 0,
    val latestValue: Double? = null,
    val latestFormattedValue: String = "--",
    val latestDate: String? = null,
    val latestName: String? = null
)

data class DashboardCardState(
    val title: String,
    val currentValue: Double? = null,
    val unit: String = "",
    val componentsText: String = "",
    val trendPoints: List<Double> = emptyList(),
    val isAvailable: Boolean = true
)

class DashboardViewModel(
    private val repository: SensorRepository,
    private val sensorProvider: SensorSourceProvider
) : ViewModel() {

    private val magHistory = mutableListOf<Double>()
    private val accelHistory = mutableListOf<Double>()
    private val lightHistory = mutableListOf<Double>()

    // Magnetic field stream: active only while Dashboard is subscribed (+1s grace for rotation)
    val magneticState: StateFlow<DashboardCardState> = sensorProvider.getSource(Quantity.MAGNETIC_FIELD).observe()
        .map { state ->
            when (state) {
                is SensorState.Active -> {
                    val resultant = VectorMath.computeResultant(
                        state.values.getOrNull(0)?.toDouble() ?: 0.0,
                        state.values.getOrNull(1)?.toDouble() ?: 0.0,
                        state.values.getOrNull(2)?.toDouble() ?: 0.0
                    )
                    magHistory.add(resultant)
                    if (magHistory.size > 25) magHistory.removeAt(0)
                    val x = state.values.getOrNull(0) ?: 0f
                    val y = state.values.getOrNull(1) ?: 0f
                    val z = state.values.getOrNull(2) ?: 0f
                    DashboardCardState(
                        title = "Magnetic Field",
                        currentValue = resultant,
                        unit = "µT",
                        componentsText = String.format(Locale.US, "X: %.1f · Y: %.1f · Z: %.1f", x, y, z),
                        trendPoints = magHistory.toList(),
                        isAvailable = true
                    )
                }
                is SensorState.Unsupported -> {
                    DashboardCardState(
                        title = "Magnetic Field",
                        unit = "µT",
                        componentsText = "Sensor not available on device",
                        isAvailable = false
                    )
                }
                else -> DashboardCardState(title = "Magnetic Field", unit = "µT")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000L),
            initialValue = DashboardCardState(title = "Magnetic Field", unit = "µT")
        )

    // Acceleration stream: active only while Dashboard is subscribed (+1s grace for rotation)
    val accelState: StateFlow<DashboardCardState> = sensorProvider.getSource(Quantity.ACCELERATION).observe()
        .map { state ->
            when (state) {
                is SensorState.Active -> {
                    val resultant = VectorMath.computeResultant(
                        state.values.getOrNull(0)?.toDouble() ?: 0.0,
                        state.values.getOrNull(1)?.toDouble() ?: 0.0,
                        state.values.getOrNull(2)?.toDouble() ?: 0.0
                    )
                    accelHistory.add(resultant)
                    if (accelHistory.size > 25) accelHistory.removeAt(0)
                    val x = state.values.getOrNull(0) ?: 0f
                    val y = state.values.getOrNull(1) ?: 0f
                    val z = state.values.getOrNull(2) ?: 0f
                    DashboardCardState(
                        title = "Acceleration",
                        currentValue = resultant,
                        unit = "m/s²",
                        componentsText = String.format(Locale.US, "X: %.2f · Y: %.2f · Z: %.2f", x, y, z),
                        trendPoints = accelHistory.toList(),
                        isAvailable = true
                    )
                }
                is SensorState.Unsupported -> {
                    DashboardCardState(
                        title = "Acceleration",
                        unit = "m/s²",
                        componentsText = "Sensor not available on device",
                        isAvailable = false
                    )
                }
                else -> DashboardCardState(title = "Acceleration", unit = "m/s²")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000L),
            initialValue = DashboardCardState(title = "Acceleration", unit = "m/s²")
        )

    // Light stream: active only while Dashboard is subscribed (+1s grace for rotation)
    val lightState: StateFlow<DashboardCardState> = sensorProvider.getSource(Quantity.LIGHT).observe()
        .map { state ->
            when (state) {
                is SensorState.Active -> {
                    val lux = (state.values.getOrNull(0) ?: 0f).toDouble()
                    lightHistory.add(lux)
                    if (lightHistory.size > 25) lightHistory.removeAt(0)
                    DashboardCardState(
                        title = "Light",
                        currentValue = lux,
                        unit = "lx",
                        componentsText = "Ambient Illuminance",
                        trendPoints = lightHistory.toList(),
                        isAvailable = true
                    )
                }
                is SensorState.Unsupported -> {
                    DashboardCardState(
                        title = "Light",
                        unit = "lx",
                        componentsText = "Sensor not available on device",
                        isAvailable = false
                    )
                }
                else -> DashboardCardState(title = "Light", unit = "lx")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000L),
            initialValue = DashboardCardState(title = "Light", unit = "lx")
        )

    val totalSavedCaptures: StateFlow<Int> = repository.observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val cardDateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

    val magneticSummary: StateFlow<QuantityDashboardSummary> = repository.observeByQuantity(Quantity.MAGNETIC_FIELD)
        .map { list ->
            val isSupported = sensorProvider.getSource(Quantity.MAGNETIC_FIELD).isSupported
            val latest = list.firstOrNull()
            QuantityDashboardSummary(
                quantity = Quantity.MAGNETIC_FIELD,
                isSupported = isSupported,
                savedCount = list.size,
                latestValue = latest?.value,
                latestFormattedValue = latest?.let { QuantityViewModel.formatMeasurementValue(Quantity.MAGNETIC_FIELD, it.value) } ?: "--",
                latestDate = latest?.let { cardDateFormat.format(Date(it.timestamp)) },
                latestName = latest?.name
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            QuantityDashboardSummary(
                quantity = Quantity.MAGNETIC_FIELD,
                isSupported = sensorProvider.getSource(Quantity.MAGNETIC_FIELD).isSupported
            )
        )

    val accelSummary: StateFlow<QuantityDashboardSummary> = repository.observeByQuantity(Quantity.ACCELERATION)
        .map { list ->
            val isSupported = sensorProvider.getSource(Quantity.ACCELERATION).isSupported
            val latest = list.firstOrNull()
            QuantityDashboardSummary(
                quantity = Quantity.ACCELERATION,
                isSupported = isSupported,
                savedCount = list.size,
                latestValue = latest?.value,
                latestFormattedValue = latest?.let { QuantityViewModel.formatMeasurementValue(Quantity.ACCELERATION, it.value) } ?: "--",
                latestDate = latest?.let { cardDateFormat.format(Date(it.timestamp)) },
                latestName = latest?.name
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            QuantityDashboardSummary(
                quantity = Quantity.ACCELERATION,
                isSupported = sensorProvider.getSource(Quantity.ACCELERATION).isSupported
            )
        )

    val lightSummary: StateFlow<QuantityDashboardSummary> = repository.observeByQuantity(Quantity.LIGHT)
        .map { list ->
            val isSupported = sensorProvider.getSource(Quantity.LIGHT).isSupported
            val latest = list.firstOrNull()
            QuantityDashboardSummary(
                quantity = Quantity.LIGHT,
                isSupported = isSupported,
                savedCount = list.size,
                latestValue = latest?.value,
                latestFormattedValue = latest?.let { QuantityViewModel.formatMeasurementValue(Quantity.LIGHT, it.value) } ?: "--",
                latestDate = latest?.let { cardDateFormat.format(Date(it.timestamp)) },
                latestName = latest?.name
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            QuantityDashboardSummary(
                quantity = Quantity.LIGHT,
                isSupported = sensorProvider.getSource(Quantity.LIGHT).isSupported
            )
        )

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                repository = SensorRepositoryImpl(
                    AppDatabase.getInstance(application).sensorReadingDao()
                ),
                sensorProvider = DefaultSensorSourceProvider(application)
            ) as T
        }
    }
}
