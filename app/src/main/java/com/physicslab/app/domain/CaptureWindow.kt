package com.physicslab.app.domain

import com.physicslab.app.data.Quantity
import com.physicslab.app.data.SensorReading
import com.physicslab.app.sensor.SensorState

/**
 * Accumulator for sensor samples collected over a capture window (~1 second).
 *
 * Requirements:
 * - Accumulates live samples.
 * - Computes the arithmetic mean of each component across all samples in the window.
 * - For vector quantities (MAGNETIC_FIELD, ACCELERATION):
 *   Resultant is computed from the mean components: `R = sqrt(meanX² + meanY² + meanZ²)`
 *   using [Double] precision.
 * - For scalar quantities (LIGHT):
 *   Components x, y, z are null. Value is the arithmetic mean of lux readings.
 * - Stores the true [sampleCount]. If fewer than minimal samples arrive in the window,
 *   it still saves with the actual count.
 * - Applies name sanitization and default naming rules via [NameValidator].
 * - Timestamps are UTC epoch millis.
 */
class CaptureWindow(
    val quantity: Quantity,
    val sensorName: String,
    val minRequiredSamples: Int = 1
) {

    private var count = 0
    private var sumX = 0.0
    private var sumY = 0.0
    private var sumZ = 0.0
    private var sumScalar = 0.0

    private var firstTimestamp: Long? = null
    private var lastAccuracy: Int? = null

    /** Current number of recorded samples in this window. */
    val sampleCount: Int
        get() = count

    /**
     * Records a 3-component vector sample (for MAGNETIC_FIELD and ACCELERATION).
     */
    fun addVectorSample(
        x: Double,
        y: Double,
        z: Double,
        accuracy: Int? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ) {
        if (firstTimestamp == null) {
            firstTimestamp = timestampEpochMillis
        }
        sumX += x
        sumY += y
        sumZ += z
        if (accuracy != null) {
            lastAccuracy = accuracy
        }
        count++
    }

    /**
     * Overload for Float sensor event values.
     */
    fun addVectorSample(
        x: Float,
        y: Float,
        z: Float,
        accuracy: Int? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ) {
        addVectorSample(x.toDouble(), y.toDouble(), z.toDouble(), accuracy, timestampEpochMillis)
    }

    /**
     * Records a scalar sample (for LIGHT).
     */
    fun addScalarSample(
        value: Double,
        accuracy: Int? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ) {
        if (firstTimestamp == null) {
            firstTimestamp = timestampEpochMillis
        }
        sumScalar += value
        if (accuracy != null) {
            lastAccuracy = accuracy
        }
        count++
    }

    /**
     * Overload for Float scalar sensor event values.
     */
    fun addScalarSample(
        value: Float,
        accuracy: Int? = null,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ) {
        addScalarSample(value.toDouble(), accuracy, timestampEpochMillis)
    }

    /**
     * Convenience method to ingest a [SensorState.Active] state emitted by the sensor layer.
     */
    fun addActiveState(
        active: SensorState.Active,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ) {
        if (quantity.hasComponents) {
            val x = active.x ?: 0f
            val y = active.y ?: 0f
            val z = active.z ?: 0f
            addVectorSample(x, y, z, active.accuracy.statusInt, timestampEpochMillis)
        } else {
            val scalar = active.scalar ?: 0f
            addScalarSample(scalar, active.accuracy.statusInt, timestampEpochMillis)
        }
    }

    /**
     * Computes the mean components, resultant, and builds a persisted [SensorReading] entity.
     *
     * @param customName User-given name, or null/blank to generate default name.
     * @param fallbackTimestamp Used if 0 samples were recorded before finishing.
     */
    fun buildReading(
        customName: String? = null,
        fallbackTimestamp: Long = System.currentTimeMillis()
    ): SensorReading {
        val timestamp = firstTimestamp ?: fallbackTimestamp
        val name = NameValidator.sanitizeOrGenerate(
            inputName = customName,
            quantity = quantity,
            timestampEpochMillis = timestamp
        )

        return if (quantity.hasComponents) {
            val (meanX, meanY, meanZ, resultant) = if (count > 0) {
                val mx = sumX / count
                val my = sumY / count
                val mz = sumZ / count
                val res = VectorMath.computeResultant(mx, my, mz)
                Tuple4(mx, my, mz, res)
            } else {
                Tuple4(0.0, 0.0, 0.0, 0.0)
            }

            SensorReading(
                id = 0L,
                quantity = quantity.name,
                name = name,
                timestamp = timestamp,
                x = meanX,
                y = meanY,
                z = meanZ,
                value = resultant,
                sampleCount = count,
                sensorName = sensorName,
                accuracy = lastAccuracy,
                updatedAt = timestamp
            )
        } else {
            val meanValue = if (count > 0) sumScalar / count else 0.0

            SensorReading(
                id = 0L,
                quantity = quantity.name,
                name = name,
                timestamp = timestamp,
                x = null,
                y = null,
                z = null,
                value = meanValue,
                sampleCount = count,
                sensorName = sensorName,
                accuracy = lastAccuracy,
                updatedAt = timestamp
            )
        }
    }

    /** Resets the window accumulator for reuse. */
    fun reset() {
        count = 0
        sumX = 0.0
        sumY = 0.0
        sumZ = 0.0
        sumScalar = 0.0
        firstTimestamp = null
        lastAccuracy = null
    }

    private data class Tuple4(
        val x: Double,
        val y: Double,
        val z: Double,
        val resultant: Double
    )
}
