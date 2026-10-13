package com.physicslab.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.physicslab.app.databinding.FragmentQuantityStatsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Statistics section for a single physical quantity.
 *
 * Requirements satisfied:
 * - Data sourced directly from persisted SQLite database (via [QuantityViewModel.detailedStats]).
 * - Shows: number of saved readings, min, max, average, latest value (with units, names, and timestamps).
 * - Interactive Native Canvas chart ([com.physicslab.app.ui.view.TimelineChartView]) showing saved values over time.
 * - For vector quantities: charts the resultant magnitude.
 * - Edge cases handled:
 *   - 0 readings: Shows attractive empty state, hides chart.
 *   - 1 reading: Renders distinct single point without broken line.
 *   - 2+ readings: Smooth continuous line with points and area fill.
 *   - Capped to latest ~50 readings with clear indicator badge.
 * - Point inspection: Tapping/scrubbing a point updates the on-chart callout and inspection banner.
 * - Immediate updates on save, rename, delete, and undo via Room's reactive Flow.
 */
class QuantityStatsFragment : Fragment() {

    private var _binding: FragmentQuantityStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuantityViewModel by lazy {
        (requireParentFragment() as QuantityFragment).viewModel
    }

    private val shortDateFormat = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuantityStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val quantity = viewModel.quantity
        val unit = quantity.unit

        // "Go to Live" button in empty state
        binding.btnGoToLiveFromStats.setOnClickListener {
            (parentFragment as? QuantityFragment)?.selectTab(0)
        }

        // Point selection inspection listener
        binding.timelineChartView.onPointSelectedListener = { selectedReading ->
            if (selectedReading != null) {
                val formattedVal = QuantityViewModel.formatMeasurementValue(quantity, selectedReading.value)
                val dateStr = fullDateFormat.format(Date(selectedReading.timestamp))
                binding.tvSelectedPointInfo.text =
                    "Selected: ${selectedReading.name} · $formattedVal $unit · $dateStr"
            } else {
                binding.tvSelectedPointInfo.text =
                    "Tap or scrub along the chart to inspect saved readings."
            }
        }

        // Reactive observation of detailed statistics directly from Room DB
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detailedStats.collectLatest { state ->
                    when (state) {
                        is QuantityDetailedStats.Empty -> {
                            binding.layoutStatsContent.visibility = View.GONE
                            binding.layoutStatsEmpty.visibility = View.VISIBLE
                        }
                        is QuantityDetailedStats.Populated -> {
                            binding.layoutStatsEmpty.visibility = View.GONE
                            binding.layoutStatsContent.visibility = View.VISIBLE

                            // 1. Average
                            val formattedAvg = QuantityViewModel.formatMeasurementValue(quantity, state.average)
                            binding.tvStatsAvg.text = "$formattedAvg $unit"
                            binding.tvStatsSubtitle.text =
                                if (state.count == 1) "Computed from 1 saved measurement"
                                else "Computed from ${state.count} saved measurements"

                            // 2. Latest
                            val formattedLatest = QuantityViewModel.formatMeasurementValue(quantity, state.latest.value)
                            binding.tvStatsLatest.text = "$formattedLatest $unit"
                            binding.tvStatsLatestSubtitle.text =
                                "${state.latest.name} · ${shortDateFormat.format(Date(state.latest.timestamp))}"

                            // 3. Min
                            val formattedMin = QuantityViewModel.formatMeasurementValue(quantity, state.min.value)
                            binding.tvStatsMin.text = "$formattedMin $unit"
                            binding.tvStatsMinSubtitle.text =
                                "${state.min.name}\n${shortDateFormat.format(Date(state.min.timestamp))}"

                            // 4. Max
                            val formattedMax = QuantityViewModel.formatMeasurementValue(quantity, state.max.value)
                            binding.tvStatsMax.text = "$formattedMax $unit"
                            binding.tvStatsMaxSubtitle.text =
                                "${state.max.name}\n${shortDateFormat.format(Date(state.max.timestamp))}"

                            // 5. Total Count
                            binding.tvStatsCount.text = state.count.toString()

                            // 6. Chart Cap Indicator
                            binding.tvChartCapIndicator.text = when {
                                state.totalCount > 50 -> "Latest 50 of ${state.totalCount}"
                                state.totalCount == 1 -> "1 reading"
                                else                  -> "${state.totalCount} readings"
                            }

                            // 7. Supply readings to native Canvas chart
                            binding.timelineChartView.setReadings(
                                items = state.chartReadings,
                                physicalUnit = unit
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
