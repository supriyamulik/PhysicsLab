package com.physicslab.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.physicslab.app.MainActivity
import com.physicslab.app.R
import com.physicslab.app.data.Quantity
import com.physicslab.app.databinding.FragmentDashboardBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by activityViewModels {
        DashboardViewModel.Factory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Card navigation clicks: tab 1 = Magnetic Field, tab 2 = Acceleration, tab 3 = Light
        binding.cardMagneticField.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }
        binding.cardAcceleration.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(2)
        }
        binding.cardLight.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(3)
        }
        binding.cardSavedCaptures.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        // Info button in header
        binding.btnDashboardInfo.setOnClickListener {
            PhysicsInfoBottomSheet.newInstance(Quantity.MAGNETIC_FIELD)
                .show(childFragmentManager, "physics_info")
        }

        // Summarized Physics Guide cards
        binding.cardPhysicsGuideMag.setOnClickListener {
            PhysicsInfoBottomSheet.newInstance(Quantity.MAGNETIC_FIELD)
                .show(childFragmentManager, "physics_info_mag")
        }
        binding.cardPhysicsGuideAccel.setOnClickListener {
            PhysicsInfoBottomSheet.newInstance(Quantity.ACCELERATION)
                .show(childFragmentManager, "physics_info_accel")
        }
        binding.cardPhysicsGuideLight.setOnClickListener {
            PhysicsInfoBottomSheet.newInstance(Quantity.LIGHT)
                .show(childFragmentManager, "physics_info_light")
        }

        // Collect persisted summaries and total count
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.magneticSummary.collectLatest { summary ->
                        bindQuantityCard(
                            summary = summary,
                            chipAvailability = binding.chipMagAvailability,
                            tvLatestValue = binding.tvMagLatestValue,
                            tvLatestDate = binding.tvMagLatestDate,
                            tvSavedCount = binding.tvMagSavedCount,
                            unit = "µT"
                        )
                    }
                }

                launch {
                    viewModel.accelSummary.collectLatest { summary ->
                        bindQuantityCard(
                            summary = summary,
                            chipAvailability = binding.chipAccelAvailability,
                            tvLatestValue = binding.tvAccelLatestValue,
                            tvLatestDate = binding.tvAccelLatestDate,
                            tvSavedCount = binding.tvAccelSavedCount,
                            unit = "m/s²"
                        )
                    }
                }

                launch {
                    viewModel.lightSummary.collectLatest { summary ->
                        bindQuantityCard(
                            summary = summary,
                            chipAvailability = binding.chipLightAvailability,
                            tvLatestValue = binding.tvLightLatestValue,
                            tvLatestDate = binding.tvLightLatestDate,
                            tvSavedCount = binding.tvLightSavedCount,
                            unit = "lux"
                        )
                    }
                }

                launch {
                    viewModel.totalSavedCaptures.collectLatest { count ->
                        binding.tvCapturesCount.text = when (count) {
                            0 -> "0 readings recorded"
                            1 -> "1 reading recorded across sensors"
                            else -> "$count readings recorded across sensors"
                        }
                        binding.layoutDashboardEmptyState.visibility =
                            if (count == 0) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun bindQuantityCard(
        summary: QuantityDashboardSummary,
        chipAvailability: TextView,
        tvLatestValue: TextView,
        tvLatestDate: TextView,
        tvSavedCount: TextView,
        unit: String
    ) {
        val context = requireContext()

        // 1. Hardware Sensor Availability Badge
        if (summary.isSupported) {
            chipAvailability.text = "Supported"
            chipAvailability.setTextColor(ContextCompat.getColor(context, R.color.lab_positive))
        } else {
            chipAvailability.text = "Not Supported"
            chipAvailability.setTextColor(ContextCompat.getColor(context, R.color.lab_danger))
        }

        // 2. Saved reading count
        tvSavedCount.text = when (summary.savedCount) {
            0 -> "0 saved readings"
            1 -> "1 saved reading"
            else -> "${summary.savedCount} saved readings"
        }

        // 3. Latest saved value + date
        if (summary.savedCount > 0 && summary.latestValue != null) {
            tvLatestValue.text = "${summary.latestFormattedValue} $unit"
            val namePrefix = summary.latestName?.let { "$it · " } ?: ""
            tvLatestDate.text = "$namePrefix${summary.latestDate ?: ""}"
        } else {
            tvLatestValue.text = "-- $unit"
            tvLatestDate.text = "No saved readings yet"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
