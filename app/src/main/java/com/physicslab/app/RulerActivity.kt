package com.physicslab.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.physicslab.app.data.ReadingLogger
import com.physicslab.app.databinding.ActivityRulerBinding

/**
 * Camera-calibrated ruler. This is a 2D pixel-ratio tool, NOT a depth-sensing
 * AR ruler: it assumes the object being measured is roughly the same distance
 * from the camera as the calibration reference object was.
 *
 * Flow:
 *  1. User places a reference object (credit card, 8.56 cm) against the camera
 *     and taps its two ends -> we store pixels-per-cm.
 *  2. User taps two new points on any other object -> pixel distance is
 *     converted to cm using the stored ratio.
 */
class RulerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRulerBinding
    private var lastPixelDistance: Float? = null

    companion object {
        private const val REQ_CAMERA = 701
        private const val CREDIT_CARD_WIDTH_CM = 8.56f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRulerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }

        binding.overlayView.onPointsChanged = { pixelDistance ->
            lastPixelDistance = pixelDistance
            updateResultText()
        }

        binding.btnReset.setOnClickListener { binding.overlayView.reset() }
        binding.btnCalibrate.setOnClickListener { calibrateUsingCurrentPoints() }

        if (hasCameraPermission()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                binding.txtUnavailable.visibility = View.VISIBLE
            }
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (e: Exception) {
                binding.txtUnavailable.text = "⚠ Could not start the camera: ${e.message}"
                binding.txtUnavailable.visibility = View.VISIBLE
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun calibrateUsingCurrentPoints() {
        val px = lastPixelDistance
        if (px == null || px <= 0f) {
            binding.txtInstruction.text = "Tap both ends of the reference object first, then tap Calibrate."
            return
        }
        val pxPerCm = px / CREDIT_CARD_WIDTH_CM
        ThemeManager.setRulerCalibration(this, pxPerCm)
        binding.txtInstruction.text = "Calibrated: %.1f px/cm. Now tap two points on the object you want to measure.".format(pxPerCm)
        binding.overlayView.reset()
        lastPixelDistance = null
        updateResultText()
    }

    private fun updateResultText() {
        val px = lastPixelDistance
        val pxPerCm = ThemeManager.getRulerCalibration(this)
        if (px == null) {
            binding.txtResultant.text = "Tap two points to measure"
            return
        }
        if (pxPerCm <= 0f) {
            binding.txtResultant.text = "Not calibrated yet - calibrate with the reference object first"
            return
        }
        val cm = px / pxPerCm
        binding.txtResultant.text = "%.2f cm".format(cm)
        ReadingLogger.log(this, lifecycleScope, "DIGITAL_RULER", "Measured length", cm.toDouble(), "cm")
    }
}
