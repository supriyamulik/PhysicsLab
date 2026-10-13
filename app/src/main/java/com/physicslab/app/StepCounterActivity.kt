package com.physicslab.app

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.physicslab.app.data.ReadingLogger
import com.physicslab.app.databinding.ActivityStepcounterBinding

class StepCounterActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityStepcounterBinding
    private lateinit var sensorManager: SensorManager
    private var stepCounter: Sensor? = null
    private var baseline: Float? = null

    companion object { private const val REQ_ACTIVITY_RECOGNITION = 501 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStepcounterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepCounter == null) {
            binding.txtUnavailable.visibility = View.VISIBLE
            binding.txtSensorInfo.text = "Sensor: not found on this device"
        } else {
            binding.txtSensorInfo.text = "Sensor: ${stepCounter!!.name}"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), REQ_ACTIVITY_RECOGNITION)
        }
    }

    override fun onResume() {
        super.onResume()
        stepCounter?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values[0]
        if (baseline == null) baseline = total
        val stepsThisSession = (total - (baseline ?: total)).toInt()
        binding.txtResultant.text = stepsThisSession.toString()

        ReadingLogger.logThrottled(this, lifecycleScope, "STEP_COUNTER", "Steps this session", stepsThisSession.toDouble(), "steps")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
