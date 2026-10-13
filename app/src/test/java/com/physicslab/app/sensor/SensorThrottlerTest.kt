package com.physicslab.app.sensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorThrottlerTest {

    @Test
    fun shouldEmit_firstEvent_alwaysEmitted() {
        val throttler = SensorThrottler(intervalMs = 100L)
        assertTrue(throttler.shouldEmit(nowMs = 1000L))
    }

    @Test
    fun shouldEmit_rapidEventsWithinInterval_areThrottled() {
        val throttler = SensorThrottler(intervalMs = 100L)

        // t = 0: first event accepted
        assertTrue(throttler.shouldEmit(nowMs = 0L))

        // t = 20, 40, 60, 99 ms: rapid events throttled
        assertFalse(throttler.shouldEmit(nowMs = 20L))
        assertFalse(throttler.shouldEmit(nowMs = 40L))
        assertFalse(throttler.shouldEmit(nowMs = 60L))
        assertFalse(throttler.shouldEmit(nowMs = 99L))

        // t = 100 ms: full 100ms interval reached -> accepted
        assertTrue(throttler.shouldEmit(nowMs = 100L))

        // t = 150 ms: throttled again
        assertFalse(throttler.shouldEmit(nowMs = 150L))

        // t = 205 ms: accepted
        assertTrue(throttler.shouldEmit(nowMs = 205L))
    }

    @Test
    fun reset_allowsImmediateNextEmission() {
        val throttler = SensorThrottler(intervalMs = 100L)

        assertTrue(throttler.shouldEmit(nowMs = 1000L))
        assertFalse(throttler.shouldEmit(nowMs = 1020L))

        throttler.reset()

        // After reset, immediate emission allowed even at the same timestamp
        assertTrue(throttler.shouldEmit(nowMs = 1020L))
    }
}
