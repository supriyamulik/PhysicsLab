package com.physicslab.app.domain

import com.physicslab.app.data.Quantity
import com.physicslab.app.data.SensorReading
import com.physicslab.app.data.SensorReadingDao
import com.physicslab.app.data.StatsResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Domain-layer repository interface for all sensor reading persistence and statistics operations.
 */
interface SensorRepository {

    /** Inserts a new reading and returns the auto-generated row ID. */
    suspend fun saveReading(reading: SensorReading): Long

    /**
     * Renames an existing reading.
     * Validates name according to [NameValidator] (trimmed, non-empty, max 60 chars).
     * Returns true if a row was updated, false if the ID was not found.
     */
    suspend fun renameReading(
        id: Long,
        newName: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Boolean

    /** Deletes a reading by its ID. */
    suspend fun deleteReading(id: Long)

    /** Deletes all readings for the given quantity. */
    suspend fun deleteAllByQuantity(quantity: Quantity)

    /** Fetches a reading by ID, or null if not found. */
    suspend fun getById(id: Long): SensorReading?

    /** Observes all readings across all quantities ordered by timestamp descending. */
    fun observeAll(): Flow<List<SensorReading>>

    /** Observes all readings for a specific quantity ordered by timestamp descending. */
    fun observeByQuantity(quantity: Quantity): Flow<List<SensorReading>>

    /**
     * Searches readings for a specific quantity by name (case-insensitive substring).
     * Automatically escapes LIKE special characters (%, _, \).
     */
    fun searchByName(quantity: Quantity, query: String): Flow<List<SensorReading>>

    /**
     * Observes readings matching optional filters: quantity, timestamp date range, and name search.
     */
    fun filterReadings(
        quantity: Quantity? = null,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        searchQuery: String? = null
    ): Flow<List<SensorReading>>

    /**
     * Observes aggregate statistics (count, min, max, average, latest) for a quantity.
     * Emits [StatsResult.Empty] if no data exists, or [StatsResult.Populated].
     */
    fun observeStats(quantity: Quantity): Flow<StatsResult>
}

/**
 * Default implementation of [SensorRepository] backed by Room's [SensorReadingDao].
 *
 * Ensures all DB operations execute off the main thread using [ioDispatcher].
 */
class SensorRepositoryImpl(
    private val dao: SensorReadingDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SensorRepository {

    override suspend fun saveReading(reading: SensorReading): Long =
        withContext(ioDispatcher) {
            val sanitizedName = NameValidator.sanitizeOrGenerate(
                inputName = reading.name,
                quantity = Quantity.valueOf(reading.quantity),
                timestampEpochMillis = reading.timestamp
            )
            val toInsert = if (sanitizedName != reading.name) {
                reading.copy(name = sanitizedName)
            } else {
                reading
            }
            dao.insert(toInsert)
        }

    override suspend fun renameReading(
        id: Long,
        newName: String,
        updatedAt: Long
    ): Boolean = withContext(ioDispatcher) {
        val validatedName = NameValidator.validateForRename(newName)
        val rowsUpdated = dao.updateName(id, validatedName, updatedAt)
        rowsUpdated > 0
    }

    override suspend fun deleteReading(id: Long): Unit =
        withContext(ioDispatcher) {
            dao.deleteById(id)
        }

    override suspend fun deleteAllByQuantity(quantity: Quantity): Unit =
        withContext(ioDispatcher) {
            dao.deleteAllByQuantity(quantity.name)
        }

    override suspend fun getById(id: Long): SensorReading? =
        withContext(ioDispatcher) {
            dao.getById(id)
        }

    override fun observeAll(): Flow<List<SensorReading>> =
        dao.observeAll().flowOn(ioDispatcher)

    override fun observeByQuantity(quantity: Quantity): Flow<List<SensorReading>> =
        dao.observeByQuantity(quantity.name).flowOn(ioDispatcher)

    override fun searchByName(quantity: Quantity, query: String): Flow<List<SensorReading>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            observeByQuantity(quantity)
        } else {
            val pattern = LikeEscaper.buildContainsPattern(trimmed)
            dao.searchByName(quantity.name, pattern).flowOn(ioDispatcher)
        }
    }

    override fun filterReadings(
        quantity: Quantity?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        searchQuery: String?
    ): Flow<List<SensorReading>> {
        val pattern = if (searchQuery.isNullOrBlank()) {
            null
        } else {
            LikeEscaper.buildContainsPattern(searchQuery.trim())
        }
        return dao.filter(
            quantity = quantity?.name,
            fromTimestamp = fromTimestamp,
            toTimestamp = toTimestamp,
            pattern = pattern
        ).flowOn(ioDispatcher)
    }

    override fun observeStats(quantity: Quantity): Flow<StatsResult> =
        dao.observeStats(quantity.name)
            .map { rawRow -> StatsResult.from(rawRow) }
            .flowOn(ioDispatcher)
}
