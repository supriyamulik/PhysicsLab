package com.physicslab.app.domain

import com.physicslab.app.data.RawStatsRow
import com.physicslab.app.data.StatsResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsResultTest {

    @Test
    fun from_emptyRowWithZeroCount_returnsEmptyState() {
        val row = RawStatsRow(count = 0, min = null, max = null, avg = null, latestValue = null)
        val result = StatsResult.from(row)

        assertTrue(result is StatsResult.Empty)
    }

    @Test
    fun from_nullMinAggregate_returnsEmptyState() {
        // SQLite aggregate behavior on empty tables can yield count=0 with null min
        val row = RawStatsRow(count = 0, min = null, max = null, avg = null, latestValue = null)
        val result = StatsResult.from(row)

        assertEquals(StatsResult.Empty, result)
    }

    @Test
    fun from_singleReading_returnsPopulatedStateWithMatchingMinMaxAvg() {
        val row = RawStatsRow(
            count = 1,
            min = 45.2,
            max = 45.2,
            avg = 45.2,
            latestValue = 45.2
        )
        val result = StatsResult.from(row)

        assertTrue(result is StatsResult.Populated)
        val populated = result as StatsResult.Populated
        assertEquals(1, populated.count)
        assertEquals(45.2, populated.min, 1e-9)
        assertEquals(45.2, populated.max, 1e-9)
        assertEquals(45.2, populated.average, 1e-9)
        assertEquals(45.2, populated.latest, 1e-9)
    }

    @Test
    fun from_multipleReadings_returnsCorrectAggregates() {
        val row = RawStatsRow(
            count = 5,
            min = 12.0,
            max = 98.5,
            avg = 55.25,
            latestValue = 73.1
        )
        val result = StatsResult.from(row)

        assertTrue(result is StatsResult.Populated)
        val populated = result as StatsResult.Populated
        assertEquals(5, populated.count)
        assertEquals(12.0, populated.min, 1e-9)
        assertEquals(98.5, populated.max, 1e-9)
        assertEquals(55.25, populated.average, 1e-9)
        assertEquals(73.1, populated.latest, 1e-9)
    }

    @Test
    fun from_afterAllReadingsDeleted_transitionsCleanlyToEmpty() {
        // Simulates query result after deleteAllByQuantity
        val rowAfterDelete = RawStatsRow(
            count = 0,
            min = null,
            max = null,
            avg = null,
            latestValue = null
        )
        val result = StatsResult.from(rowAfterDelete)

        assertEquals(StatsResult.Empty, result)
    }
}
