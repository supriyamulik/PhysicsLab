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
import com.physicslab.app.databinding.ActivityCompassBinding

class CompassActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityCompassBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var haveGravity = false
    private var haveGeomagnetic = false
    private var currentHeading = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null || magnetometer == null) {
            binding.txtUnavailable.visibility = View.VISIBLE
            binding.txtSensorInfo.text = "Sensor: missing accelerometer or magnetometer"
        } else {
            binding.txtSensorInfo.text = "Sensors: ${accelerometer!!.name} + ${magnetometer!!.name}"
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> { System.arraycopy(event.values, 0, gravity, 0, 3); haveGravity = true }
            Sensor.TYPE_MAGNETIC_FIELD -> { System.arraycopy(event.values, 0, geomagnetic, 0, 3); haveGeomagnetic = true }
            else -> return
        }
        if (!haveGravity || !haveGeomagnetic) return

        val rotationMatrix = FloatArray(9)
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        if (!success) return

        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f
        currentHeading = azimuthDeg

        binding.txtResultant.text = String.format("%.0f°", azimuthDeg)
        binding.txtDirection.text = headingLabel(azimuthDeg)
        binding.imgNeedle.rotation = -azimuthDeg

        ReadingLogger.logThrottled(this, lifecycleScope, "COMPASS", "Heading", azimuthDeg.toDouble(), "°")
    }

    private fun headingLabel(deg: Float): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        return dirs[(((deg + 22.5f) / 45f).toInt()) % 8]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
