package com.physicslab.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.physicslab.app.R
import com.physicslab.app.data.Quantity
import com.physicslab.app.databinding.BottomSheetPhysicsInfoBinding
import com.physicslab.app.domain.PhysicsExplanation
import com.physicslab.app.domain.PhysicsExplanations
import com.physicslab.app.sensor.SensorAccuracy
import com.physicslab.app.sensor.SensorMetadata
import java.util.Locale

/**
 * Bottom sheet displaying concise, accurate physics explanations and device sensor telemetry.
 *
 * Requirements satisfied:
 * - Magnetic field: tesla and µT, formula B = √(Bx²+By²+Bz²), Earth's field 25–65 µT, sensor disturbances.
 * - Acceleration: m/s², formula a = √(ax²+ay²+az²), gravity ≈ 9.81 m/s² included, free fall near 0.
 * - Light: illuminance in lux (lm/m²), approximate everyday ranges, uncalibrated indicative note.
 * - Conservative claims; ranges marked as approximate.
 */
class PhysicsInfoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPhysicsInfoBinding? = null
    private val binding get() = _binding!!

    private var quantity: Quantity = Quantity.MAGNETIC_FIELD
    private var metadata: SensorMetadata? = null
    private var accuracy: SensorAccuracy? = null

    companion object {
        private const val ARG_QUANTITY = "arg_quantity"

        fun newInstance(
            quantity: Quantity,
            metadata: SensorMetadata? = null,
            accuracy: SensorAccuracy? = null
        ): PhysicsInfoBottomSheet {
            return PhysicsInfoBottomSheet().apply {
                this.quantity = quantity
                this.metadata = metadata
                this.accuracy = accuracy
                arguments = Bundle().apply {
                    putString(ARG_QUANTITY, quantity.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_QUANTITY)?.let {
            try {
                quantity = Quantity.valueOf(it)
            } catch (_: Exception) {}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPhysicsInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val explanation: PhysicsExplanation = PhysicsExplanations.getFor(quantity)

        // 1. Header
        binding.tvPhysicsTitle.text = explanation.title
        binding.tvPhysicsSiUnit.text = "SI Unit: ${explanation.siUnit}"
        binding.tvPhysicsUnitBadge.text = explanation.unitSymbol

        // 2. Formula
        binding.tvPhysicsFormula.text = explanation.formulaText

        // 3. Definition
        binding.tvPhysicsDefinition.text = explanation.definition

        // 4. Typical reference ranges
        val container = binding.layoutReferenceRangesContainer
        container.removeAllViews()
        val density = resources.displayMetrics.density

        for ((label, description) in explanation.referenceRanges) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, (4 * density).toInt(), 0, (6 * density).toInt())
            }

            val tvLabel = TextView(requireContext()).apply {
                text = "• $label"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.lab_text_primary))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val tvDesc = TextView(requireContext()).apply {
                text = description
                setTextColor(ContextCompat.getColor(requireContext(), R.color.lab_text_secondary))
                textSize = 12f
                setPadding((12 * density).toInt(), (1 * density).toInt(), 0, 0)
                setLineSpacing(0f, 1.25f)
            }

            row.addView(tvLabel)
            row.addView(tvDesc)
            container.addView(row)
        }

        // 5. Disturbances and limitations
        binding.tvPhysicsDisturbances.text = explanation.disturbanceAndLimitations

        // 6. Device hardware sensor specs
        val meta = metadata
        if (meta != null) {
            binding.cardHardwareSpecs.visibility = View.VISIBLE
            binding.tvHardwareName.text = meta.name
            binding.tvHardwareVendor.text = meta.vendor
            binding.tvHardwareMaxRange.text =
                String.format(Locale.US, "%.1f %s", meta.maxRange, explanation.unitSymbol)
            binding.tvHardwareResolution.text =
                String.format(Locale.US, "%.4f %s", meta.resolution, explanation.unitSymbol)
        } else {
            binding.cardHardwareSpecs.visibility = View.GONE
        }

        // Done button
        binding.btnPhysicsClose.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
