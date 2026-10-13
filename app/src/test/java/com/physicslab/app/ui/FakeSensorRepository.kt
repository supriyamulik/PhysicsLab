package com.physicslab.app.ui

import com.physicslab.app.data.Quantity
import com.physicslab.app.data.SensorReading
import com.physicslab.app.data.StatsResult
import com.physicslab.app.domain.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory test fake for [SensorRepository].
 * Kept strictly inside the test source set per Prompt 0 non-negotiable rules.
 */
class FakeSensorRepository : SensorRepository {

    private val idCounter = AtomicLong(1L)
    private val readings = MutableStateFlow<List<SensorReading>>(emptyList())

    override suspend fun saveReading(reading: SensorReading): Long {
        val id = idCounter.getAndIncrement()
        val assigned = reading.copy(id = id)
        readings.value = listOf(assigned) + readings.value
        return id
    }

    override suspend fun renameReading(id: Long, newName: String, updatedAt: Long): Boolean {
        val current = readings.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return false
        val updated = current[index].copy(name = newName, updatedAt = updatedAt)
        readings.value = current.toMutableList().apply { set(index, updated) }
        return true
    }

    override suspend fun deleteReading(id: Long) {
        readings.value = readings.value.filterNot { it.id == id }
    }

    override suspend fun deleteAllByQuantity(quantity: Quantity) {
        readings.value = readings.value.filterNot { it.quantity == quantity.name }
    }

    override suspend fun getById(id: Long): SensorReading? {
        return readings.value.firstOrNull { it.id == id }
    }

    override fun observeAll(): Flow<List<SensorReading>> = readings

    override fun observeByQuantity(quantity: Quantity): Flow<List<SensorReading>> =
        readings.map { list -> list.filter { it.quantity == quantity.name } }

    override fun searchByName(quantity: Quantity, query: String): Flow<List<SensorReading>> =
        readings.map { list ->
            list.filter { it.quantity == quantity.name && it.name.contains(query, ignoreCase = true) }
        }

    override fun filterReadings(
        quantity: Quantity?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        searchQuery: String?
    ): Flow<List<SensorReading>> = readings.map { list ->
        list.filter { item ->
            (quantity == null || item.quantity == quantity.name) &&
            (fromTimestamp == null || item.timestamp >= fromTimestamp) &&
            (toTimestamp == null || item.timestamp <= toTimestamp) &&
            (searchQuery == null || item.name.contains(searchQuery, ignoreCase = true))
        }
    }

    override fun observeStats(quantity: Quantity): Flow<StatsResult> =
        readings.map { list ->
            val matching = list.filter { it.quantity == quantity.name }
            if (matching.isEmpty()) {
                StatsResult.Empty
            } else {
                StatsResult.Populated(
                    count = matching.size,
                    min = matching.minOf { it.value },
                    max = matching.maxOf { it.value },
                    average = matching.map { it.value }.average(),
                    latest = matching.maxByOrNull { it.timestamp }?.value ?: 0.0
                )
            }
        }
}
