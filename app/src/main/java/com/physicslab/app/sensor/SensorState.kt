package com.physicslab.app.sensor

/**
 * Sealed hierarchy representing the reactive lifecycle states of a hardware sensor stream.
 */
sealed class SensorState {

    /**
     * Emitted when the device does not possess the requested sensor hardware
     * (`getDefaultSensor()` returned null). UI can gracefully degrade or show an
     * "unavailable on this device" banner.
     */
    object Unsupported : SensorState()

    /**
     * Emitted when the sensor is available and listener registration has begun,
     * but before the very first hardware event is received.
     */
    data class Idle(val metadata: SensorMetadata) : SensorState()

    /**
     * Emitted when live sensor readings are actively being received.
     *
     * @param values Raw sensor values copied defensively from `SensorEvent.values` (no smoothing, no fake data).
     * @param accuracy Current hardware calibration accuracy mapped to [SensorAccuracy].
     * @param timestampNs Hardware event timestamp in nanoseconds from `SensorEvent.timestamp`.
     * @param metadata Hardware specifications of the reporting sensor.
     */
    data class Active(
        val values: FloatArray,
        val accuracy: SensorAccuracy,
        val timestampNs: Long,
        val metadata: SensorMetadata
    ) : SensorState() {

        /** First component (X for vectors, lux for light). */
        val x: Float?
            get() = if (values.isNotEmpty()) values[0] else null

        /** Second component (Y for vectors, null for scalars). */
        val y: Float?
            get() = if (values.size > 1) values[1] else null

        /** Third component (Z for vectors, null for scalars). */
        val z: Float?
            get() = if (values.size > 2) values[2] else null

        /** Scalar reading value (lux for light). */
        val scalar: Float?
            get() = if (values.isNotEmpty()) values[0] else null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Active) return false
            return values.contentEquals(other.values) &&
                    accuracy == other.accuracy &&
                    timestampNs == other.timestampNs &&
                    metadata == other.metadata
        }

        override fun hashCode(): Int {
            var result = values.contentHashCode()
            result = 31 * result + accuracy.hashCode()
            result = 31 * result + timestampNs.hashCode()
            result = 31 * result + metadata.hashCode()
            return result
        }

        override fun toString(): String {
            return "Active(values=${values.contentToString()}, accuracy=$accuracy, timestampNs=$timestampNs, metadata=$metadata)"
        }
    }
}
