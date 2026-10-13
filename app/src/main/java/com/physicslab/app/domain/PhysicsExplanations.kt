package com.physicslab.app.domain

import com.physicslab.app.data.Quantity

/**
 * Factual, conservative physics concepts and sensor context.
 *
 * Rules:
 * - Formulas and SI units clearly stated.
 * - Reference values marked as approximate.
 * - Hardware disturbances and measurement limitations explicitly documented.
 * - No unsubstantiated precision claims.
 */
data class PhysicsExplanation(
    val quantity: Quantity,
    val title: String,
    val siUnit: String,
    val unitSymbol: String,
    val formulaText: String,
    val definition: String,
    val referenceRanges: List<Pair<String, String>>,
    val disturbanceAndLimitations: String,
    val sensorNote: String
)

object PhysicsExplanations {

    val MAGNETIC_FIELD = PhysicsExplanation(
        quantity = Quantity.MAGNETIC_FIELD,
        title = "Magnetic Field",
        siUnit = "Tesla (smartphone measures in microtesla, µT)",
        unitSymbol = "µT",
        formulaText = "B = √(Bx² + By² + Bz²)",
        definition = "Magnetic field is a vector quantity that describes the magnetic force experienced by moving electric charges, electric currents, and magnetic materials. Smartphones use a 3-axis Hall-effect or magnetoresistive magnetometer to measure field strength along the device's X, Y, and Z coordinate axes.",
        referenceRanges = listOf(
            "Earth's Natural Field" to "Roughly 25 to 65 µT at the surface, depending on geomagnetic latitude and local crustal geology.",
            "Indoor Electronics" to "Typically 65 to 150 µT near operating laptops, power adapters, or wiring.",
            "Permanent Magnets / Speakers" to "Often exceeding 200 to 1,000+ µT within a few centimeters of magnetic phone mounts, speaker magnets, or closures."
        ),
        disturbanceAndLimitations = "Smartphone magnetometers are highly sensitive to hard-iron and soft-iron distortions. Permanent magnets, steel desks, building structural rebar, smartphone speakers, vibration motors, magnetic case latches, and high-current electrical cables distort readings. Waving the device in a figure-8 motion helps recalibrate the sensor.",
        sensorNote = "Smartphones measure ambient magnetic flux density. Readings reflect both Earth's ambient field and all nearby magnetic materials combined."
    )

    val ACCELERATION = PhysicsExplanation(
        quantity = Quantity.ACCELERATION,
        title = "Acceleration",
        siUnit = "Meters per second squared",
        unitSymbol = "m/s²",
        formulaText = "a = √(ax² + ay² + az²)",
        definition = "Acceleration is the rate of change of velocity over time. Smartphone accelerometers measure 'proper acceleration' (the acceleration relative to a free-fall frame), which includes the mechanical reaction force countering Earth's gravity.",
        referenceRanges = listOf(
            "Device at Rest" to "Approximately 9.81 m/s² (Earth's standard gravitational acceleration, g ≈ 9.80665 m/s²). The sensor measures the upward normal force supporting the phone.",
            "Free Fall" to "Near 0 m/s². In true free fall or weightlessness, all axes drop to approximately zero because the device is falling freely under gravity.",
            "Active Motion / Walking" to "Typically 11 to 20 m/s² during human gait, arm gestures, or gentle vibration.",
            "Vigorous Shaking / Impact" to "Can exceed 25 to 50+ m/s² depending on the intensity of motion."
        ),
        disturbanceAndLimitations = "Because gravity is permanently measured alongside linear acceleration, the reading does not separate device motion from gravity without sensor fusion (gyroscope integration). Device orientation determines how the ~9.81 m/s² vector distributes across X, Y, and Z axes.",
        sensorNote = "At rest face-up on a flat table, the Z-axis typically reads ~+9.8 m/s² while X and Y read near 0 m/s²."
    )

    val LIGHT = PhysicsExplanation(
        quantity = Quantity.LIGHT,
        title = "Ambient Light",
        siUnit = "Lux (lumens per square meter, lm/m²)",
        unitSymbol = "lux",
        formulaText = "E = Φ / A  (1 lux = 1 lumen / m²)",
        definition = "Illuminance is a photometric measure of the total luminous flux incident on a surface per unit area, weighted by the human eye's spectral sensitivity curve (photopic vision).",
        referenceRanges = listOf(
            "Dark Room / Night Ambient" to "Approximately 0.1 to 10 lux.",
            "Dim Indoor / Living Room" to "Approximately 10 to 100 lux.",
            "Office / Classroom Lighting" to "Approximately 300 to 500 lux.",
            "Overcast Day Outdoors" to "Approximately 1,000 to 10,000 lux.",
            "Direct Sunlight" to "Approximately 30,000 to 100,000+ lux."
        ),
        disturbanceAndLimitations = "Smartphone light sensors are uncalibrated consumer photodiodes located near the top display bezel, positioned primarily for automatic screen brightness. They are directional, easily shadowed by hands or phone cases, and uncorrected for color spectrum. Readings are indicative approximations and not equivalent to a calibrated laboratory photometer.",
        sensorNote = "Phone ambient light sensors provide scalar illuminance. Orientation toward or away from light sources drastically alters measured values."
    )

    fun getFor(quantity: Quantity): PhysicsExplanation = when (quantity) {
        Quantity.MAGNETIC_FIELD -> MAGNETIC_FIELD
        Quantity.ACCELERATION   -> ACCELERATION
        Quantity.LIGHT          -> LIGHT
    }

    val ALL = listOf(MAGNETIC_FIELD, ACCELERATION, LIGHT)
}
