package com.physicslab.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class VectorMathTest {

    @Test
    fun computeResultant_known3_4_0_vector_equals_5() {
        val result = VectorMath.computeResultant(3.0, 4.0, 0.0)
        assertEquals(5.0, result, 1e-9)
    }

    @Test
    fun computeResultant_known1_2_2_vector_equals_3() {
        val result = VectorMath.computeResultant(1.0, 2.0, 2.0)
        assertEquals(3.0, result, 1e-9)
    }

    @Test
    fun computeResultant_zeroVector_equals_zero() {
        val result = VectorMath.computeResultant(0.0, 0.0, 0.0)
        assertEquals(0.0, result, 1e-9)
    }

    @Test
    fun computeResultant_negativeComponents_squaredCorrectly() {
        val result = VectorMath.computeResultant(-3.0, -4.0, 0.0)
        assertEquals(5.0, result, 1e-9)
    }

    @Test
    fun computeResultant_floatOverload_preservesPrecision() {
        val result = VectorMath.computeResultant(3.0f, 4.0f, 0.0f)
        assertEquals(5.0, result, 1e-9)
    }
}
