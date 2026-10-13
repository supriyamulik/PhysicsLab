package com.physicslab.app.ui.view

import com.physicslab.app.data.Quantity
import com.physicslab.app.data.SensorReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartScaleHelperTest {

    @Test
    fun computeScale_withEmptyList_returnsEmptyScaleWithPositiveRange() {
        val scale = ChartScaleHelper.computeScale(emptyList())
        assertTrue(scale.isEmpty)
        assertFalse(scale.isSinglePoint)
        assertTrue(scale.yRange > 0.0)
        assertEquals(0.0, scale.yMin, 1e-6)
        assertEquals(1.0, scale.yMax, 1e-6)
    }

    @Test
    fun computeScale_withSinglePoint_returnsSinglePointScaleWithSymmetricBuffer() {
        val value = 48.2
        val scale = ChartScaleHelper.computeScale(listOf(value))

        assertFalse(scale.isEmpty)
        assertTrue(scale.isSinglePoint)
        assertTrue(scale.yRange > 0.0)
        assertTrue(scale.yMin < value)
        assertTrue(scale.yMax > value)
        // Center of yMin and yMax should be close to value
        assertEquals(value, (scale.yMin + scale.yMax) / 2.0, 1e-6)
    }

    @Test
    fun computeScale_withFlatRangeMinEqualsMax_padsWithSymmetricBuffer() {
        // e.g. Phone resting at constant gravity: 9.81 m/s² repeatedly
        val flatValues = listOf(9.81, 9.81, 9.81, 9.81)
        val scale = ChartScaleHelper.computeScale(flatValues)

        assertFalse(scale.isEmpty)
        assertFalse(scale.isSinglePoint)
        assertTrue(scale.yRange > 0.0)
        assertTrue(scale.yMin < 9.81)
        assertTrue(scale.yMax > 9.81)
    }

    @Test
    fun computeScale_filtersNaNAndInfinity_computesOnlyOnFiniteValues() {
        val dirtyValues = listOf(Double.NaN, 10.0, Double.POSITIVE_INFINITY, 20.0, Double.NEGATIVE_INFINITY)
        val scale = ChartScaleHelper.computeScale(dirtyValues)

        assertFalse(scale.isEmpty)
        assertTrue(scale.yRange > 0.0)
        assertTrue(scale.yMin <= 10.0)
        assertTrue(scale.yMax >= 20.0)
    }

    @Test
    fun computeScale_withAllNonFinite_treatsAsEmpty() {
        val allBad = listOf(Double.NaN, Double.POSITIVE_INFINITY)
        val scale = ChartScaleHelper.computeScale(allBad)
        assertTrue(scale.isEmpty)
        assertTrue(scale.yRange > 0.0)
    }

    @Test
    fun computeScale_handlesVeryLargeLuxValues_calculatesSafePositiveRange() {
        // Direct solar exposure: up to 120,000 lux
        val luxValues = listOf(500.0, 25000.0, 105000.0)
        val scale = ChartScaleHelper.computeScale(luxValues)

        assertFalse(scale.isEmpty)
        assertTrue(scale.yRange > 0.0)
        assertTrue(scale.yMin <= 500.0)
        assertTrue(scale.yMax >= 105000.0)
    }

    @Test
    fun capReadings_moreThanLimit_capsToLimitAndReversesToChronological() {
        val readings = (1..70).map { i ->
            SensorReading(
                id = i.toLong(),
                quantity = Quantity.ACCELERATION.name,
                name = "Reading $i",
                timestamp = (1000 + i * 10).toLong(),
                x = 0.0,
                y = 0.0,
                z = 9.8,
                value = 9.8 + (i * 0.01),
                sampleCount = 10,
                sensorName = "Accel",
                accuracy = 3,
                updatedAt = (1000 + i * 10).toLong()
            )
        }.reversed() // Simulating Room newest-first ordering (i=70 down to 1)

        val capped = ChartScaleHelper.capReadings(readings, limit = 50)
        assertEquals(50, capped.size)
        // Capped list should be oldest-to-newest among the latest 50
        // Latest 50 are i=21 to i=70. Oldest of those is i=21.
        assertEquals("Reading 21", capped.first().name)
        assertEquals("Reading 70", capped.last().name)
    }

    @Test
    fun formatAxisValue_largeNumbers_formatsWithKiloSuffix() {
        assertEquals("100k", ChartScaleHelper.formatAxisValue(100000.0))
        assertEquals("45k", ChartScaleHelper.formatAxisValue(45000.0))
        assertEquals("1.2k", ChartScaleHelper.formatAxisValue(1200.0))
        assertEquals("1.3k", ChartScaleHelper.formatAxisValue(1250.0))
    }

    @Test
    fun formatAxisValue_normalAndSmallNumbers_formatsWithSensibleDecimals() {
        assertEquals("48.2", ChartScaleHelper.formatAxisValue(48.23))
        assertEquals("9.81", ChartScaleHelper.formatAxisValue(9.81))
        assertEquals("0.45", ChartScaleHelper.formatAxisValue(0.45))
        assertEquals("--", ChartScaleHelper.formatAxisValue(Double.NaN))
    }
}
