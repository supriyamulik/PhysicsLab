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
import com.physicslab.app.databinding.ActivityGyroscopeBinding
import kotlin.math.sqrt

class GyroscopeActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityGyroscopeBinding
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGyroscopeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (gyroscope == null) {
            binding.txtUnavailable.visibility = View.VISIBLE
            binding.txtSensorInfo.text = "Sensor: not found on this device"
        } else {
            binding.txtSensorInfo.text = "Sensor: ${gyroscope!!.name}"
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val resultant = sqrt((x * x + y * y + z * z).toDouble())

        binding.txtX.text = String.format("%.2f", x)
        binding.txtY.text = String.format("%.2f", y)
        binding.txtZ.text = String.format("%.2f", z)
        binding.txtResultant.text = String.format("%.2f", resultant)

        ReadingLogger.logThrottled(this, lifecycleScope, "GYROSCOPE", "Angular velocity", resultant, "rad/s")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
