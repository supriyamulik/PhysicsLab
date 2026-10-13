package com.physicslab.app.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.physicslab.app.databinding.DialogSensorInfoBinding
import com.physicslab.app.sensor.SensorAccuracy
import com.physicslab.app.sensor.SensorMetadata
import java.util.Locale

class SensorInfoDialogFragment : DialogFragment() {

    private var metadata: SensorMetadata? = null
    private var accuracy: SensorAccuracy? = null
    private var unit: String = ""

    companion object {
        fun newInstance(
            metadata: SensorMetadata?,
            accuracy: SensorAccuracy?,
            unit: String
        ): SensorInfoDialogFragment {
            return SensorInfoDialogFragment().apply {
                this.metadata = metadata
                this.accuracy = accuracy
                this.unit = unit
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSensorInfoBinding.inflate(LayoutInflater.from(requireContext()))

        val meta = metadata
        if (meta != null) {
            binding.tvInfoSensorName.text = meta.name
            binding.tvInfoVendor.text = meta.vendor
            binding.tvInfoMaxRange.text = String.format(Locale.US, "%.2f %s", meta.maxRange, unit)
            binding.tvInfoResolution.text = String.format(Locale.US, "%.4f %s", meta.resolution, unit)
        } else {
            binding.tvInfoSensorName.text = "Hardware Sensor"
            binding.tvInfoVendor.text = "Device Manufacturer"
            binding.tvInfoMaxRange.text = "N/A"
            binding.tvInfoResolution.text = "N/A"
        }

        val acc = accuracy
        if (acc != null) {
            binding.tvInfoAccuracy.text = "${acc.name} (${acc.statusInt})"
        } else {
            binding.tvInfoAccuracy.text = "Active"
        }

        binding.btnInfoClose.setOnClickListener { dismiss() }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
    }
}
