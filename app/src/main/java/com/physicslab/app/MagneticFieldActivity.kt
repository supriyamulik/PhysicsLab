package com.physicslab.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.physicslab.app.data.ReadingLogger
import com.physicslab.app.databinding.ActivityMagneticBinding
import kotlin.math.sqrt

/**
 * Reads the phone's magnetometer (TYPE_MAGNETIC_FIELD) and displays:
 *  - the raw X, Y, Z components in microtesla (uT)
 *  - the resultant magnetic field magnitude B = sqrt(Bx^2 + By^2 + Bz^2)
 *
 * This is real-time sensor data - the SensorEventListener callback fires
 * whenever the hardware reports a new reading. No values are hardcoded.
 */
class MagneticFieldActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMagneticBinding
    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMagneticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtBack.setOnClickListener { finish() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magnetometer == null) {
            binding.txtUnavailable.visibility = android.view.View.VISIBLE
            binding.txtSensorInfo.text = "Sensor: not found on this device"
        } else {
            binding.txtSensorInfo.text = "Sensor: ${magnetometer!!.name}"
        }
    }

    override fun onResume() {
        super.onResume()
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Resultant magnetic field magnitude: vector sum of the 3 orthogonal components.
        val resultant = sqrt((x * x + y * y + z * z).toDouble())

        binding.txtX.text = String.format("%.2f", x)
        binding.txtY.text = String.format("%.2f", y)
        binding.txtZ.text = String.format("%.2f", z)
        binding.txtResultant.text = String.format("%.2f", resultant)

        ReadingLogger.logThrottled(this, lifecycleScope, "MAGNETIC_FIELD", "Magnetic field (B)", resultant, "µT")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this demonstration, but required by the interface.
    }
}
