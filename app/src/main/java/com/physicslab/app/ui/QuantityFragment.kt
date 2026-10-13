package com.physicslab.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.physicslab.app.R
import com.physicslab.app.data.Quantity
import com.physicslab.app.databinding.FragmentQuantityBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuantityFragment : Fragment() {

    private var _binding: FragmentQuantityBinding? = null
    private val binding get() = _binding!!

    lateinit var quantity: Quantity

    val viewModel: QuantityViewModel by viewModels {
        QuantityViewModel.Factory(requireActivity().application, quantity)
    }

    companion object {
        private const val ARG_QUANTITY = "arg_quantity"
        private const val KEY_SECTION_TAB = "key_section_tab"

        fun newInstance(quantity: Quantity): QuantityFragment {
            return QuantityFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUANTITY, quantity.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val qName = arguments?.getString(ARG_QUANTITY) ?: Quantity.MAGNETIC_FIELD.name
        quantity = Quantity.valueOf(qName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuantityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvQuantityTitle.text = quantity.displayName

        // ViewPager2 for 3 sections
        val pagerAdapter = QuantitySectionsPagerAdapter(this)
        binding.quantityViewPager.adapter = pagerAdapter
        binding.quantityViewPager.offscreenPageLimit = 2

        val savedTab = savedInstanceState?.getInt(KEY_SECTION_TAB, 0) ?: 0
        if (savedTab != 0) {
            binding.quantityViewPager.setCurrentItem(savedTab, false)
            updateTabStyles(savedTab)
        }

        // Segment button clicks
        binding.btnTabLive.setOnClickListener { binding.quantityViewPager.setCurrentItem(0, true) }
        binding.btnTabHistory.setOnClickListener { binding.quantityViewPager.setCurrentItem(1, true) }
        binding.btnTabStats.setOnClickListener { binding.quantityViewPager.setCurrentItem(2, true) }

        // Sync tab indicator on page change
        binding.quantityViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabStyles(position)
            }
        })

        // Physics & Sensor Info button
        binding.btnQuantityInfo.setOnClickListener {
            val live = viewModel.liveState.value
            PhysicsInfoBottomSheet.newInstance(
                quantity = quantity,
                metadata = live.metadata,
                accuracy = live.accuracy
            ).show(childFragmentManager, "physics_info")
        }

        // Collect accuracy and calibration hint
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.liveState.collectLatest { state ->
                    // Accuracy badge
                    val acc = state.accuracy
                    if (acc != null) {
                        binding.chipAccuracy.visibility = View.VISIBLE
                        binding.chipAccuracy.text = "${acc.name} Accuracy"
                    } else {
                        binding.chipAccuracy.visibility = View.GONE
                    }

                    // Calibration hint
                    if (state.calibrationHint != null) {
                        binding.bannerCalibration.visibility = View.VISIBLE
                        binding.tvCalibrationHint.text = state.calibrationHint
                    } else {
                        binding.bannerCalibration.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun updateTabStyles(selectedPosition: Int) {
        val context = requireContext()
        binding.btnTabLive.setBackgroundResource(
            if (selectedPosition == 0) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
        )
        binding.btnTabLive.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                context,
                if (selectedPosition == 0) R.color.lab_surface else R.color.lab_text_secondary
            )
        )

        binding.btnTabHistory.setBackgroundResource(
            if (selectedPosition == 1) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
        )
        binding.btnTabHistory.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                context,
                if (selectedPosition == 1) R.color.lab_surface else R.color.lab_text_secondary
            )
        )

        binding.btnTabStats.setBackgroundResource(
            if (selectedPosition == 2) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
        )
        binding.btnTabStats.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                context,
                if (selectedPosition == 2) R.color.lab_surface else R.color.lab_text_secondary
            )
        )
    }

    fun selectTab(position: Int) {
        binding.quantityViewPager.setCurrentItem(position, true)
    }

    /** Returns true if handled by switching back to Live tab, false otherwise */
    fun handleBackPress(): Boolean {
        return if (binding.quantityViewPager.currentItem != 0) {
            binding.quantityViewPager.setCurrentItem(0, true)
            true
        } else {
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.let {
            outState.putInt(KEY_SECTION_TAB, it.quantityViewPager.currentItem)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class QuantitySectionsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> QuantityLiveFragment()
                1 -> QuantityHistoryFragment()
                2 -> QuantityStatsFragment()
                else -> QuantityLiveFragment()
            }
        }
    }
}
