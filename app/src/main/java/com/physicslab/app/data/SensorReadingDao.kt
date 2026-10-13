package com.physicslab.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [SensorReading].
 *
 * Threading contract: all suspend functions must be called from a non-main
 * coroutine dispatcher (Dispatchers.IO). All [Flow] streams are emitted by
 * Room's internal transaction thread and are safe to collect on any dispatcher.
 *
 * LIKE-search note: [searchByName] uses `ESCAPE '\'` so the backslash is the
 * escape character inside LIKE patterns. Callers MUST escape %, _ and \ in the
 * [pattern] argument before passing it in. Use [com.physicslab.app.domain.LikeEscaper]
 * for this — do not bypass it.
 */
@Dao
interface SensorReadingDao {

    // ── Writes ──────────────────────────────────────────────────────────────────

    /** Inserts a new reading and returns the auto-generated row ID. */
    @Insert
    suspend fun insert(reading: SensorReading): Long

    /**
     * Updates the [SensorReading.name] and [SensorReading.updatedAt] fields for a
     * given [id]. Returns the number of rows updated (0 if no row with that id exists).
     */
    @Query("""
        UPDATE sensor_readings
        SET    name      = :name,
               updatedAt = :updatedAt
        WHERE  id        = :id
    """)
    suspend fun updateName(id: Long, name: String, updatedAt: Long): Int

    /** Hard-deletes the reading with the given [id]. No-op if not found. */
    @Query("DELETE FROM sensor_readings WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Hard-deletes all readings for a given [quantity] string (e.g. "MAGNETIC_FIELD").
     * Statistics for that quantity will immediately emit [StatsResult.Empty].
     */
    @Query("DELETE FROM sensor_readings WHERE quantity = :quantity")
    suspend fun deleteAllByQuantity(quantity: String)

    // ── Point reads ──────────────────────────────────────────────────────────────

    /** Returns the reading with the given [id], or null if not found. */
    @Query("SELECT * FROM sensor_readings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SensorReading?

    // ── Reactive streams ─────────────────────────────────────────────────────────

    /**
     * Emits all readings across all quantities, newest first.
     */
    @Query("""
        SELECT * FROM sensor_readings
        ORDER  BY timestamp DESC
    """)
    fun observeAll(): Flow<List<SensorReading>>

    /**
     * Emits the full list of readings for [quantity], newest first, and re-emits
     * on every insert, rename, or delete that touches the table.
     */
    @Query("""
        SELECT * FROM sensor_readings
        WHERE  quantity = :quantity
        ORDER  BY timestamp DESC
    """)
    fun observeByQuantity(quantity: String): Flow<List<SensorReading>>

    /**
     * Filters readings by optional criteria:
     * - [quantity]: exact match if non-null
     * - [fromTimestamp]: minimum timestamp inclusive if non-null
     * - [toTimestamp]: maximum timestamp inclusive if non-null
     * - [pattern]: escaped LIKE pattern for name substring if non-null
     */
    @Query("""
        SELECT * FROM sensor_readings
        WHERE (:quantity IS NULL OR quantity = :quantity)
          AND (:fromTimestamp IS NULL OR timestamp >= :fromTimestamp)
          AND (:toTimestamp IS NULL OR timestamp <= :toTimestamp)
          AND (:pattern IS NULL OR name LIKE :pattern ESCAPE '\')
        ORDER BY timestamp DESC
    """)
    fun filter(
        quantity: String?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        pattern: String?
    ): Flow<List<SensorReading>>

    /**
     * Case-insensitive substring search on the [SensorReading.name] column.
     *
     * [pattern] must be a fully-escaped LIKE pattern, e.g. "%foo\%bar%".
     * Use [com.physicslab.app.domain.LikeEscaper.buildContainsPattern] to produce it.
     *
     * The `ESCAPE '\'` clause means: inside [pattern], a backslash followed by
     * %, _, or \ treats those characters as literals rather than LIKE wildcards.
     * SQLite LIKE is case-insensitive for ASCII letters by default.
     */
    @Query("""
        SELECT * FROM sensor_readings
        WHERE  quantity = :quantity
          AND  name     LIKE :pattern ESCAPE '\'
        ORDER  BY timestamp DESC
    """)
    fun searchByName(quantity: String, pattern: String): Flow<List<SensorReading>>

    /**
     * Emits a single aggregate row for [quantity]. The row always exists (SQLite
     * always returns one row from COUNT/MIN/MAX/AVG even for an empty set).
     *
     * When the table has no rows for [quantity]:
     *   count = 0, min = null, max = null, avg = null, latestValue = null
     *
     * Callers convert the raw row to [StatsResult] via [StatsResult.from].
     * This Flow re-emits after every table write, so statistics update automatically
     * after insert, rename, delete, and delete-all-of-a-type.
     */
    @Query("""
        SELECT
            COUNT(*)                                       AS count,
            MIN(value)                                     AS min,
            MAX(value)                                     AS max,
            AVG(value)                                     AS avg,
            ( SELECT value
              FROM   sensor_readings
              WHERE  quantity = :quantity
              ORDER  BY timestamp DESC
              LIMIT  1 )                                   AS latestValue
        FROM sensor_readings
        WHERE quantity = :quantity
    """)
    fun observeStats(quantity: String): Flow<RawStatsRow>
}
