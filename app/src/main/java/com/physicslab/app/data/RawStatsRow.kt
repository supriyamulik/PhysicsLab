package com.physicslab.app.data

import androidx.room.ColumnInfo

/**
 * Projection returned by the raw statistics SQL query in [SensorReadingDao.observeStats].
 *
 * SQLite aggregate functions (MIN, MAX, AVG) return NULL when the result set is empty,
 * so all aggregate columns are declared nullable here. COUNT(*) always returns an
 * integer (0 for an empty set), so [count] is non-nullable.
 *
 * Consumers should never read these fields directly; convert via [StatsResult.from].
 */
data class RawStatsRow(
    @ColumnInfo(name = "count")       val count: Int,
    @ColumnInfo(name = "min")         val min: Double?,
    @ColumnInfo(name = "max")         val max: Double?,
    @ColumnInfo(name = "avg")         val avg: Double?,
    @ColumnInfo(name = "latestValue") val latestValue: Double?
)
