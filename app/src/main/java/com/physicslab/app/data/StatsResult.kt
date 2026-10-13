package com.physicslab.app.data

/**
 * Sealed result of a per-quantity statistics query.
 *
 * Two possible states:
 *  - [Empty]     — no readings exist for this quantity; never returns 0, NaN, or crashes.
 *  - [Populated] — at least one reading exists; all fields are well-defined doubles.
 *
 * The companion [from] factory converts a [RawStatsRow] from Room into the appropriate
 * subtype and is the only intended way to construct these objects outside of tests.
 *
 * Statistics are computed from persisted data (via SQL aggregates), never cached
 * separately. They therefore update automatically after any insert, rename, delete,
 * or delete-all-of-a-type operation because the [SensorReadingDao.observeStats] Flow
 * is invalidated by Room on every table write.
 */
sealed class StatsResult {

    /** No readings exist for this quantity. UI should show an explicit "No data" state. */
    object Empty : StatsResult()

    /**
     * At least one reading exists for this quantity.
     *
     * @param count    Total number of persisted readings.
     * @param min      Minimum [SensorReading.value] across all readings.
     * @param max      Maximum [SensorReading.value].
     * @param average  Arithmetic mean of [SensorReading.value] (SQL AVG).
     * @param latest   The [SensorReading.value] of the most-recently-timestamped reading.
     */
    data class Populated(
        val count: Int,
        val min: Double,
        val max: Double,
        val average: Double,
        val latest: Double
    ) : StatsResult()

    companion object {

        /**
         * Converts a [RawStatsRow] returned by the DAO into the correct sealed subtype.
         *
         * Decision rule: if [RawStatsRow.count] == 0 (or [RawStatsRow.min] is null, which
         * Room can sometimes produce for a COUNT=0 row), return [Empty]. Otherwise, all
         * non-null aggregate values are safe to force-unwrap.
         */
        fun from(row: RawStatsRow): StatsResult =
            if (row.count == 0 || row.min == null) {
                Empty
            } else {
                Populated(
                    count   = row.count,
                    min     = row.min,
                    max     = row.max!!,        // non-null when count > 0
                    average = row.avg!!,        // non-null when count > 0
                    latest  = row.latestValue!! // non-null when count > 0
                )
            }
    }
}
