package com.physicslab.app

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.physicslab.app.data.Quantity
import com.physicslab.app.databinding.ActivityMainBinding
import com.physicslab.app.ui.DashboardFragment
import com.physicslab.app.ui.QuantityFragment

/**
 * Modern PhysicsLab Main Activity implementing bottom navigation with 4 destinations:
 * Dashboard, Magnetic Field, Acceleration, and Light.
 *
 * Requirements satisfied:
 * 1. Strictly light theme only, forceDarkAllowed=false, system night mode disabled.
 * 2. Bottom navigation with Dashboard, Magnetic Field, Acceleration, Light.
 * 3. Preserves state across tab switches and device rotation via ViewPager2 offscreenPageLimit.
 * 4. Correct back button behavior: sub-tabs return to Live first, then to Dashboard, then exit.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val KEY_SELECTED_TAB = "key_selected_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce light theme only
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ViewPager2 for bottom navigation
        val adapter = MainTabsPagerAdapter(this)
        binding.mainViewPager.adapter = adapter
        binding.mainViewPager.offscreenPageLimit = 3
        binding.mainViewPager.isUserInputEnabled = false // bottom bar taps perform smooth transitions

        // Restore selected tab across process death / configuration change
        val initialTab = savedInstanceState?.getInt(KEY_SELECTED_TAB, 0) ?: 0
        if (initialTab != 0) {
            binding.mainViewPager.setCurrentItem(initialTab, false)
            val initialItemId = when (initialTab) {
                1 -> R.id.nav_magnetic
                2 -> R.id.nav_acceleration
                3 -> R.id.nav_light
                else -> R.id.nav_dashboard
            }
            binding.bottomNavigation.selectedItemId = initialItemId
        }

        // Bottom Navigation Item Selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    binding.mainViewPager.setCurrentItem(0, true)
                    true
                }
                R.id.nav_magnetic -> {
                    binding.mainViewPager.setCurrentItem(1, true)
                    true
                }
                R.id.nav_acceleration -> {
                    binding.mainViewPager.setCurrentItem(2, true)
                    true
                }
                R.id.nav_light -> {
                    binding.mainViewPager.setCurrentItem(3, true)
                    true
                }
                else -> false
            }
        }

        // Keep BottomNavigationView selected tab in sync with ViewPager2
        binding.mainViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val itemId = when (position) {
                    0 -> R.id.nav_dashboard
                    1 -> R.id.nav_magnetic
                    2 -> R.id.nav_acceleration
                    3 -> R.id.nav_light
                    else -> R.id.nav_dashboard
                }
                if (binding.bottomNavigation.selectedItemId != itemId) {
                    binding.bottomNavigation.selectedItemId = itemId
                }
            }
        })

        // Intuitive Back Button Handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTab = binding.mainViewPager.currentItem
                if (currentTab != 0) {
                    // Try letting the active QuantityFragment handle inner section navigation (e.g. History -> Live)
                    val fragment = supportFragmentManager.findFragmentByTag("f$currentTab") as? QuantityFragment
                    if (fragment != null && fragment.handleBackPress()) {
                        return
                    }
                    // Return to Dashboard from any quantity screen
                    binding.mainViewPager.setCurrentItem(0, true)
                } else {
                    // On Dashboard: finish app
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, binding.mainViewPager.currentItem)
    }

    /** Programmatically switch bottom navigation tabs (e.g. from Dashboard sensor cards) */
    fun navigateToTab(tabIndex: Int) {
        if (tabIndex in 0..3) {
            binding.mainViewPager.setCurrentItem(tabIndex, true)
        }
    }

    private class MainTabsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DashboardFragment()
                1 -> QuantityFragment.newInstance(Quantity.MAGNETIC_FIELD)
                2 -> QuantityFragment.newInstance(Quantity.ACCELERATION)
                3 -> QuantityFragment.newInstance(Quantity.LIGHT)
                else -> DashboardFragment()
            }
        }
    }
}
