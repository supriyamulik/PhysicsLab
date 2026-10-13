package com.physicslab.app.sensor

/**
 * Thread-safe rate limiter that limits emissions to approximately [intervalMs]
 * (default 100 ms = ~10 updates/sec).
 *
 * Incoming hardware sensor callbacks (often firing at 60Hz or higher with SENSOR_DELAY_UI)
 * are downsampled so that the UI thread only performs cheap updates ~10 times per second,
 * preventing UI jank and excessive frame redraws while preserving the latest raw data.
 *
 * @param intervalMs Minimum interval between allowed emissions in milliseconds.
 * @param clock Pluggable time provider for deterministic unit testing.
 */
class SensorThrottler(
    val intervalMs: Long = DEFAULT_THROTTLE_INTERVAL_MS,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private var lastEmittedTimeMs: Long? = null

    /**
     * Checks whether an event arriving at [nowMs] should be forwarded or dropped.
     *
     * Returns true if this is the first event or if at least [intervalMs] has elapsed
     * since the last emitted event.
     */
    @Synchronized
    fun shouldEmit(nowMs: Long = clock()): Boolean {
        val lastTime = lastEmittedTimeMs
        if (lastTime == null || (nowMs - lastTime) >= intervalMs) {
            lastEmittedTimeMs = nowMs
            return true
        }
        return false
    }

    /** Resets the throttler state. */
    @Synchronized
    fun reset() {
        lastEmittedTimeMs = null
    }

    companion object {
        /** Default interval: 100 ms (~10 updates per second). */
        const val DEFAULT_THROTTLE_INTERVAL_MS = 100L
    }
}
