package com.physicslab.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.physicslab.app.data.ReadingLogger
import com.physicslab.app.databinding.ActivityLightBinding

/**
 * Reads the phone's ambient light sensor (TYPE_LIGHT) and displays the
 * illuminance in lux, along with a simple qualitative description of the
 * lighting condition so the reading is easy to relate to during a viva.
 */
class LightActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityLightBinding
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtBack.setOnClickListener { finish() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor == null) {
            binding.txtUnavailable.visibility = android.view.View.VISIBLE
            binding.txtSensorInfo.text = "Sensor: not found on this device"
        } else {
            binding.txtSensorInfo.text = "Sensor: ${lightSensor!!.name}"
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_LIGHT) return

        val lux = event.values[0]
        binding.txtLux.text = String.format("%.1f", lux)
        binding.txtCondition.text = "Condition: ${describeCondition(lux)}"

        ReadingLogger.logThrottled(this, lifecycleScope, "LIGHT", "Illuminance", lux.toDouble(), "lux")
    }

    private fun describeCondition(lux: Float): String = when {
        lux < 1 -> "Almost total darkness"
        lux < 10 -> "Very dim (moonlight-like)"
        lux < 50 -> "Dim indoor lighting"
        lux < 200 -> "Normal indoor room"
        lux < 500 -> "Well-lit indoor / office"
        lux < 1000 -> "Bright indoor / overcast outdoor"
        lux < 10000 -> "Daylight (shade)"
        else -> "Direct sunlight"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this demonstration, but required by the interface.
    }
}
