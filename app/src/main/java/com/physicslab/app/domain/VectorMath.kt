package com.physicslab.app.domain

import kotlin.math.sqrt

/**
 * Pure mathematical utilities for vector resultant calculations.
 *
 * All calculations use [Double] precision to prevent floating-point underflow/overflow
 * or precision loss when summing squared sensor components.
 */
object VectorMath {

    /**
     * Calculates the 3D Euclidean norm (resultant magnitude):
     * `R = sqrt(x² + y² + z²)`
     *
     * Used for:
     * - Magnetic Field resultant: B = sqrt(x² + y² + z²) in µT
     * - Acceleration resultant:   a = sqrt(x² + y² + z²) in m/s²
     */
    fun computeResultant(x: Double, y: Double, z: Double): Double {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * Overload accepting Float values (as emitted by Android Sensor events),
     * converted to Double before squaring to preserve precision.
     */
    fun computeResultant(x: Float, y: Float, z: Float): Double {
        val dx = x.toDouble()
        val dy = y.toDouble()
        val dz = z.toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
