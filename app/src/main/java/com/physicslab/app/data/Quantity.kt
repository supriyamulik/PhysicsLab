package com.physicslab.app.data

/**
 * The three physical quantities supported by the new domain layer.
 *
 * Stored in the DB column `quantity` as the enum's [name] string
 * ("MAGNETIC_FIELD", "ACCELERATION", "LIGHT") so that:
 *  - SQL queries can filter by quantity with a simple string equality.
 *  - Adding a new quantity in future only requires a new enum value;
 *    no schema migration is needed.
 */
enum class Quantity {

    /** Magnetic flux density — sensor TYPE_MAGNETIC_FIELD, unit µT, three axis components. */
    MAGNETIC_FIELD,

    /** Linear / total acceleration — TYPE_ACCELEROMETER or TYPE_LINEAR_ACCELERATION, m/s², three axes. */
    ACCELERATION,

    /** Illuminance — TYPE_LIGHT, lux, scalar (no X/Y/Z components). */
    LIGHT;

    /** Human-readable label used in auto-generated reading names and UI. */
    val displayName: String
        get() = when (this) {
            MAGNETIC_FIELD -> "Magnetic Field"
            ACCELERATION   -> "Acceleration"
            LIGHT          -> "Light"
        }

    /** Physical unit string, shown after numeric values. */
    val unit: String
        get() = when (this) {
            MAGNETIC_FIELD -> "µT"
            ACCELERATION   -> "m/s²"
            LIGHT          -> "lux"
        }

    /**
     * True for vector quantities that have X/Y/Z components; false for scalars.
     * [CaptureWindow] uses this to decide whether to compute a resultant or
     * just average the scalar values.
     */
    val hasComponents: Boolean
        get() = when (this) {
            MAGNETIC_FIELD, ACCELERATION -> true
            LIGHT                        -> false
        }

    /**
     * Android [android.hardware.Sensor] type constant for this quantity.
     * Guaranteed not to substitute other sensor types:
     * - MAGNETIC_FIELD -> TYPE_MAGNETIC_FIELD
     * - ACCELERATION   -> TYPE_ACCELEROMETER (includes gravity)
     * - LIGHT          -> TYPE_LIGHT
     */
    val sensorType: Int
        get() = when (this) {
            MAGNETIC_FIELD -> android.hardware.Sensor.TYPE_MAGNETIC_FIELD
            ACCELERATION   -> android.hardware.Sensor.TYPE_ACCELEROMETER
            LIGHT          -> android.hardware.Sensor.TYPE_LIGHT
        }
}
