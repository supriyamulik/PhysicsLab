package com.physicslab.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.physicslab.app.data.ReadingLogger
import com.physicslab.app.databinding.ActivityAccelerationBinding
import kotlin.math.sqrt

/**
 * Reads the phone's accelerometer and displays the resultant acceleration.
 *
 * Two modes, switchable at runtime with the on-screen toggle:
 *  - TYPE_ACCELEROMETER (gravity INCLUDED): when the phone is lying still,
 *    the resultant will read close to 9.8 m/s^2 because the sensor also
 *    measures the reaction force that supports the phone against gravity.
 *  - TYPE_LINEAR_ACCELERATION (gravity REMOVED, software-filtered by the
 *    phone's sensor fusion): at rest this reads close to 0 m/s^2, and only
 *    responds to acceleration caused by actually moving the phone.
 *
 * Resultant magnitude: a = sqrt(ax^2 + ay^2 + az^2)
 */
class AccelerationActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityAccelerationBinding
    private lateinit var sensorManager: SensorManager

    private var accelerometer: Sensor? = null
    private var linearAccelerometer: Sensor? = null
    private var includeGravity = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccelerationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtBack.setOnClickListener { finish() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        if (accelerometer == null) {
            binding.txtUnavailable.visibility = android.view.View.VISIBLE
            binding.txtSensorInfo.text = "Sensor: not found on this device"
            binding.switchGravity.isEnabled = false
        } else {
            binding.txtSensorInfo.text = "Sensor: ${accelerometer!!.name}"
        }

        updateExplanation()

        binding.switchGravity.isChecked = true
        binding.switchGravity.setOnCheckedChangeListener { _, isChecked ->
            includeGravity = isChecked
            registerActiveSensor()
            updateExplanation()
        }
    }

    override fun onResume() {
        super.onResume()
        registerActiveSensor()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun registerActiveSensor() {
        sensorManager.unregisterListener(this)
        val target = if (includeGravity) accelerometer else (linearAccelerometer ?: accelerometer)
        target?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun updateExplanation() {
        binding.txtExplanation.text = if (includeGravity) {
            "The raw accelerometer (TYPE_ACCELEROMETER) measures the total specific force acting on the phone, which includes the reaction to Earth's gravitational pull (g ≈ 9.8 m/s²) even when the phone is perfectly still. That is why, at rest, the resultant a = √(ax² + ay² + az²) reads close to 9.8 m/s² instead of zero — you are measuring the normal force holding the phone up against gravity, exactly like a weighing scale. Tilt or shake the phone to see the resultant change."
        } else {
            "This mode uses TYPE_LINEAR_ACCELERATION, where the phone's sensor fusion (accelerometer + gyroscope) mathematically subtracts the constant gravity vector. At rest the resultant a = √(ax² + ay² + az²) now reads close to 0 m/s², because only acceleration caused by actual motion of the phone is being shown - not the reaction to gravity."
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER &&
            event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION
        ) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val resultant = sqrt((x * x + y * y + z * z).toDouble())

        binding.txtX.text = String.format("%.2f", x)
        binding.txtY.text = String.format("%.2f", y)
        binding.txtZ.text = String.format("%.2f", z)
        binding.txtResultant.text = String.format("%.2f", resultant)

        val label = if (includeGravity) "Acceleration (with gravity)" else "Linear acceleration"
        ReadingLogger.logThrottled(this, lifecycleScope, "ACCELERATION", label, resultant, "m/s²")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this demonstration, but required by the interface.
    }
}
