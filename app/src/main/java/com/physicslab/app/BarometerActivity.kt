package com.physicslab.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.physicslab.app.data.ReadingLogger
import com.physicslab.app.databinding.ActivityBarometerBinding
import kotlin.math.pow

class BarometerActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityBarometerBinding
    private lateinit var sensorManager: SensorManager
    private var barometer: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarometerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (barometer == null) {
            binding.txtUnavailable.visibility = View.VISIBLE
            binding.txtSensorInfo.text = "Sensor: not found on this device"
        } else {
            binding.txtSensorInfo.text = "Sensor: ${barometer!!.name}"
        }
    }

    override fun onResume() {
        super.onResume()
        barometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_PRESSURE) return
        val hPa = event.values[0]
        binding.txtResultant.text = String.format("%.2f", hPa)

        // Barometric formula assuming standard sea-level pressure of 1013.25 hPa.
        val seaLevel = 1013.25
        val altitude = 44330.0 * (1.0 - (hPa / seaLevel).toDouble().pow(1.0 / 5.255))
        binding.txtAltitude.text = String.format("%.1f m above sea level (est.)", altitude)

        ReadingLogger.logThrottled(this, lifecycleScope, "BAROMETER", "Atmospheric pressure", hPa.toDouble(), "hPa")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
