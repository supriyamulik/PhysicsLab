package com.physicslab.app.domain

import com.physicslab.app.data.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureWindowTest {

    @Test
    fun vectorAveraging_computesMeanOfComponents_andResultantFromMeans() {
        val window = CaptureWindow(Quantity.MAGNETIC_FIELD, "Test Magnetometer")

        // Sample 1: (2.0, 3.0, 0.0)
        window.addVectorSample(2.0, 3.0, 0.0, accuracy = 2, timestampEpochMillis = 1000L)
        // Sample 2: (4.0, 5.0, 0.0)
        window.addVectorSample(4.0, 5.0, 0.0, accuracy = 3, timestampEpochMillis = 2000L)

        // Means: X = 3.0, Y = 4.0, Z = 0.0 -> resultant sqrt(3² + 4²) = 5.0
        val reading = window.buildReading(customName = "Vector Test")

        assertEquals(2, reading.sampleCount)
        assertEquals(3.0, reading.x!!, 1e-9)
        assertEquals(4.0, reading.y!!, 1e-9)
        assertEquals(0.0, reading.z!!, 1e-9)
        assertEquals(5.0, reading.value, 1e-9)
        assertEquals(1000L, reading.timestamp) // Preserves first sample timestamp
        assertEquals(3, reading.accuracy) // Preserves last accuracy
        assertEquals("Vector Test", reading.name)
    }

    @Test
    fun scalarAveraging_computesMeanLux_andNullComponents() {
        val window = CaptureWindow(Quantity.LIGHT, "Test Light Sensor")

        window.addScalarSample(120.0, accuracy = 3, timestampEpochMillis = 5000L)
        window.addScalarSample(180.0, accuracy = 3, timestampEpochMillis = 5500L)

        val reading = window.buildReading()

        assertEquals(2, reading.sampleCount)
        assertNull(reading.x)
        assertNull(reading.y)
        assertNull(reading.z)
        assertEquals(150.0, reading.value, 1e-9)
        assertEquals(5000L, reading.timestamp)
        assertNotNull(reading.name)
    }

    @Test
    fun lowSampleCount_stillSavesWithTrueCount() {
        val window = CaptureWindow(Quantity.ACCELERATION, "Accelerometer", minRequiredSamples = 10)

        // Only 1 sample arrives in window
        window.addVectorSample(0.0, 9.8, 0.0, timestampEpochMillis = 12345L)

        val reading = window.buildReading()

        assertEquals(1, reading.sampleCount)
        assertEquals(0.0, reading.x!!, 1e-9)
        assertEquals(9.8, reading.y!!, 1e-9)
        assertEquals(0.0, reading.z!!, 1e-9)
        assertEquals(9.8, reading.value, 1e-9)
    }

    @Test
    fun zeroSamples_handledGracefullyWithoutCrashing() {
        val window = CaptureWindow(Quantity.MAGNETIC_FIELD, "Magnetometer")
        val reading = window.buildReading(fallbackTimestamp = 9999L)

        assertEquals(0, reading.sampleCount)
        assertEquals(0.0, reading.value, 1e-9)
        assertEquals(9999L, reading.timestamp)
    }

    @Test
    fun reset_clearsAccumulatedSamples() {
        val window = CaptureWindow(Quantity.LIGHT, "Light")
        window.addScalarSample(50.0)
        assertEquals(1, window.sampleCount)

        window.reset()
        assertEquals(0, window.sampleCount)
        val reading = window.buildReading(fallbackTimestamp = 100L)
        assertEquals(0, reading.sampleCount)
    }
}
