package com.physicslab.app.ui.view

import com.physicslab.app.data.SensorReading
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Bounds and metadata computed for chart scaling.
 */
data class ChartScale(
    val yMin: Double,
    val yMax: Double,
    val yRange: Double,
    val isSinglePoint: Boolean,
    val isEmpty: Boolean
)

/**
 * Pure Kotlin helper for chart data transformations and numerical scaling.
 *
 * Encapsulates:
 * - Chronological sorting and capping to ~50 readings
 * - NaN / Infinity filtering
 * - Flat range (min == max) buffer padding
 * - Single point centering without broken lines
 * - Large number (lux) compact string formatting
 */
object ChartScaleHelper {

    /**
     * Caps a list of readings (ordered newest-first by Room) to at most [limit] items,
     * filters non-finite values, and returns them in chronological order
     * (oldest to newest, left to right for timeline display).
     */
    fun capReadings(readings: List<SensorReading>, limit: Int = 50): List<SensorReading> {
        val valid = readings.filter { it.value.isFinite() }
        return valid.take(limit).reversed()
    }

    /**
     * Computes safe [ChartScale] bounds for a list of values.
     * Guaranteed:
     * - Returns [yRange] > 0 when not empty (preventing divide-by-zero).
     * - [yMin] <= [yMax].
     * - Single points or identical values are padded with a symmetric buffer.
     */
    fun computeScale(values: List<Double>): ChartScale {
        val finiteValues = values.filter { it.isFinite() }
        if (finiteValues.isEmpty()) {
            return ChartScale(
                yMin = 0.0,
                yMax = 1.0,
                yRange = 1.0,
                isSinglePoint = false,
                isEmpty = true
            )
        }

        if (finiteValues.size == 1) {
            val v = finiteValues[0]
            val buffer = max(1.0, abs(v) * 0.15)
            val yMin = if (v >= 0.0) max(0.0, v - buffer) else v - buffer
            val yMax = v + buffer
            return ChartScale(
                yMin = yMin,
                yMax = yMax,
                yRange = yMax - yMin,
                isSinglePoint = true,
                isEmpty = false
            )
        }

        val rawMin = finiteValues.minOrNull() ?: 0.0
        val rawMax = finiteValues.maxOrNull() ?: 1.0

        if (abs(rawMax - rawMin) < 1e-6) {
            // Flat range: all values are identical (e.g. at rest gravity 9.81 m/s²)
            val buffer = max(1.0, abs(rawMin) * 0.15)
            val yMin = if (rawMin >= 0.0) max(0.0, rawMin - buffer) else rawMin - buffer
            val yMax = rawMax + buffer
            return ChartScale(
                yMin = yMin,
                yMax = yMax,
                yRange = yMax - yMin,
                isSinglePoint = false,
                isEmpty = false
            )
        }

        // Standard scaling with 10% vertical padding
        val span = rawMax - rawMin
        val pad = span * 0.10
        val yMin = if (rawMin >= 0.0 && rawMin - pad < 0.0) 0.0 else rawMin - pad
        val yMax = rawMax + pad

        return ChartScale(
            yMin = yMin,
            yMax = yMax,
            yRange = yMax - yMin,
            isSinglePoint = false,
            isEmpty = false
        )
    }

    /**
     * Formats an axis value into a concise label without clutter or clipping:
     * - >= 10,000 -> "10k", "50k", "100k"
     * - >= 1,000 -> "1.2k", "4.5k"
     * - >= 10 -> "12.3", "45.0"
     * - < 10 -> "1.25", "9.81"
     */
    fun formatAxisValue(value: Double): String {
        if (!value.isFinite()) return "--"
        val absVal = abs(value)
        return when {
            absVal >= 10_000.0 -> String.format(Locale.US, "%.0fk", value / 1_000.0)
            absVal >= 1_000.0  -> String.format(Locale.US, "%.1fk", value / 1_000.0)
            absVal >= 10.0     -> String.format(Locale.US, "%.1f", value)
            else               -> String.format(Locale.US, "%.2f", value)
        }
    }
}
