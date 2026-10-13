package com.physicslab.app.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.physicslab.app.R
import com.physicslab.app.data.Quantity
import com.physicslab.app.databinding.FragmentQuantityLiveBinding
import com.physicslab.app.domain.NameValidator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

class QuantityLiveFragment : Fragment() {

    private var _binding: FragmentQuantityLiveBinding? = null
    private val binding get() = _binding!!

    // Scoped to parent QuantityFragment
    private val viewModel: QuantityViewModel by lazy {
        (requireParentFragment() as QuantityFragment).viewModel
    }

    private var captureDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuantityLiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val quantity = viewModel.quantity

        // 1. Static quantity-specific explanatory notices
        when (quantity) {
            Quantity.ACCELERATION -> {
                binding.cardAccelGravityNotice.visibility = View.VISIBLE
                binding.cardLightIndicativeNotice.visibility = View.GONE
                binding.cardAxisValues.visibility = View.VISIBLE
            }
            Quantity.LIGHT -> {
                binding.cardAccelGravityNotice.visibility = View.GONE
                binding.cardLightIndicativeNotice.visibility = View.VISIBLE
                binding.cardAxisValues.visibility = View.GONE // Scalar: NO X/Y/Z
            }
            Quantity.MAGNETIC_FIELD -> {
                binding.cardAccelGravityNotice.visibility = View.GONE
                binding.cardLightIndicativeNotice.visibility = View.GONE
                binding.cardAxisValues.visibility = View.VISIBLE
            }
        }

        val heroSubtitle = when (quantity) {
            Quantity.MAGNETIC_FIELD -> "Resultant Field Strength"
            Quantity.ACCELERATION -> "Resultant Acceleration"
            Quantity.LIGHT -> "Ambient Illuminance"
        }

        // 2. Save reading button click with rapid-tap protection
        binding.btnSaveReading.setOnClickListener {
            if (!binding.btnSaveReading.isEnabled) return@setOnClickListener
            showNameInputDialog(quantity)
        }

        // 3. Collect live sensor telemetry only while Fragment is visible (STARTED)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.liveState.collectLatest { state ->
                        updateLiveUi(state, heroSubtitle)
                    }
                }

                launch {
                    viewModel.captureState.collectLatest { captureState ->
                        handleCaptureState(captureState)
                    }
                }
            }
        }
    }

    private fun updateLiveUi(state: LiveQuantityUiState, heroSubtitle: String) {
        val quantity = state.quantity

        // Hardware sensor name
        if (state.metadata != null) {
            binding.tvSensorHardwareName.text = "Sensor: ${state.metadata.name}"
        } else {
            binding.tvSensorHardwareName.text = "Sensor: ${quantity.displayName}"
        }

        // Unsupported sensor handling
        if (state.isUnsupported) {
            binding.cardUnsupported.visibility = View.VISIBLE
            binding.tvUnsupportedReason.text = "The ${quantity.displayName} hardware sensor is not available on this device."
            binding.cardGauge.visibility = View.GONE
            binding.cardMetrics.visibility = View.GONE
            binding.cardAxisValues.visibility = View.GONE
            binding.cardLiveTrend.visibility = View.GONE
            binding.cardMagCalibrationNotice.visibility = View.GONE

            // Disable save button safely
            binding.btnSaveReading.isEnabled = false
            binding.btnSaveReading.alpha = 0.45f
            binding.btnSaveReading.text = "Sensor Not Available"
            return
        } else {
            binding.cardUnsupported.visibility = View.GONE
            binding.cardGauge.visibility = View.VISIBLE
            binding.cardMetrics.visibility = View.VISIBLE
            binding.cardLiveTrend.visibility = View.VISIBLE
            if (viewModel.captureState.value !is CaptureUiState.InProgress) {
                binding.btnSaveReading.isEnabled = true
                binding.btnSaveReading.alpha = 1.0f
                binding.btnSaveReading.text = "+ Save Reading"
            }
        }

        // Calibration notice for magnetometer
        if (state.calibrationHint != null) {
            binding.cardMagCalibrationNotice.visibility = View.VISIBLE
            binding.tvCalibrationHint.text = state.calibrationHint
        } else {
            binding.cardMagCalibrationNotice.visibility = View.GONE
        }

        // Circular Gauge update
        val expectedMax = when (quantity) {
            Quantity.MAGNETIC_FIELD -> 100.0
            Quantity.ACCELERATION -> 20.0
            Quantity.LIGHT -> (state.currentValue?.takeIf { it > 1000.0 } ?: 1000.0)
        }

        binding.gaugeView.setMeasurement(
            value = state.currentValue,
            unit = quantity.unit,
            subtitle = heroSubtitle,
            expectedMin = 0.0,
            expectedMax = expectedMax,
            customValueFormatted = state.formattedHeroValue
        )

        // 3 Metrics (Min / Avg / Max) with smart formatting
        val unit = quantity.unit
        binding.tvLiveMin.text = state.minSession?.let {
            "${QuantityViewModel.formatMeasurementValue(quantity, it)} $unit"
        } ?: "--"

        binding.tvLiveAvg.text = state.avgSession?.let {
            "${QuantityViewModel.formatMeasurementValue(quantity, it)} $unit"
        } ?: "--"

        binding.tvLiveMax.text = state.maxSession?.let {
            "${QuantityViewModel.formatMeasurementValue(quantity, it)} $unit"
        } ?: "--"

        // Axis values (for vector quantities)
        if (quantity.hasComponents) {
            val x = state.x ?: 0f
            val y = state.y ?: 0f
            val z = state.z ?: 0f

            val decimalFormat = if (quantity == Quantity.ACCELERATION) "%.2f %s" else "%.1f %s"
            binding.tvAxisX.text = String.format(Locale.US, decimalFormat, x, unit)
            binding.tvAxisY.text = String.format(Locale.US, decimalFormat, y, unit)
            binding.tvAxisZ.text = String.format(Locale.US, decimalFormat, z, unit)

            val axisMax = if (quantity == Quantity.ACCELERATION) 20f else 100f
            binding.pbAxisX.progress = (abs(x) / axisMax * 100f).toInt().coerceIn(0, 100)
            binding.pbAxisY.progress = (abs(y) / axisMax * 100f).toInt().coerceIn(0, 100)
            binding.pbAxisZ.progress = (abs(z) / axisMax * 100f).toInt().coerceIn(0, 100)
        }

        // Live trend chart update
        binding.liveTrendView.setPoints(state.trendPoints)
    }

    private fun showNameInputDialog(quantity: Quantity) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_reading, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etReadingName)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDialogCancel)
        val btnConfirm = dialogView.findViewById<View>(R.id.btnDialogConfirm)

        val defaultName = NameValidator.generateDefaultName(quantity, System.currentTimeMillis())
        etName.setText(defaultName)
        etName.selectAll()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            val entered = etName.text?.toString()?.trim().orEmpty()
            val finalName = if (entered.isBlank()) defaultName else entered
            dialog.dismiss()

            // Trigger the ~1-second capture window with visible progress
            viewModel.startCapture(finalName)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun handleCaptureState(state: CaptureUiState) {
        when (state) {
            is CaptureUiState.Idle -> {
                dismissCaptureDialog()
                if (!viewModel.liveState.value.isUnsupported) {
                    binding.btnSaveReading.isEnabled = true
                    binding.btnSaveReading.alpha = 1.0f
                }
            }
            is CaptureUiState.InProgress -> {
                // Prevent double saves
                binding.btnSaveReading.isEnabled = false
                binding.btnSaveReading.alpha = 0.5f

                showOrUpdateCaptureProgress(state.percent)
            }
            is CaptureUiState.Success -> {
                dismissCaptureDialog()
                binding.btnSaveReading.isEnabled = true
                binding.btnSaveReading.alpha = 1.0f

                val reading = state.reading
                val formattedVal = QuantityViewModel.formatMeasurementValue(
                    viewModel.quantity,
                    reading.value
                )
                Snackbar.make(
                    binding.root,
                    "Saved: \"${reading.name}\" ($formattedVal ${viewModel.quantity.unit})",
                    Snackbar.LENGTH_LONG
                ).show()

                viewModel.resetCaptureState()
            }
            is CaptureUiState.Error -> {
                dismissCaptureDialog()
                binding.btnSaveReading.isEnabled = true
                binding.btnSaveReading.alpha = 1.0f

                Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.resetCaptureState()
            }
        }
    }

    private fun showOrUpdateCaptureProgress(percent: Int) {
        if (captureDialog == null || !captureDialog!!.isShowing) {
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_capture_progress, null)
            captureDialog = AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(false)
                .create().apply {
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    show()
                }
        }

        captureDialog?.findViewById<ProgressBar>(R.id.pbCaptureWindow)?.progress = percent
        captureDialog?.findViewById<TextView>(R.id.tvCaptureProgressPercent)?.text = "$percent%"
    }

    private fun dismissCaptureDialog() {
        captureDialog?.dismiss()
        captureDialog = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissCaptureDialog()
        _binding = null
    }
}
