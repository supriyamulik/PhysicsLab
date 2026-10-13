package com.physicslab.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.physicslab.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }

        binding.radioLight.isChecked = true
        binding.radioDark.isEnabled = false
        binding.radioSystem.isEnabled = false
        binding.radioDark.text = "Dark (Disabled - Light theme only)"
        binding.radioSystem.text = "Follow System (Disabled)"

        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            ThemeManager.setMode(this, ThemeManager.MODE_LIGHT)
        }

        refreshCalibrationText()
        binding.btnResetCalibration.setOnClickListener {
            ThemeManager.setRulerCalibration(this, -1f)
            refreshCalibrationText()
        }
    }

    private fun refreshCalibrationText() {
        val pxPerCm = ThemeManager.getRulerCalibration(this)
        binding.txtRulerCalibration.text = if (pxPerCm > 0f) {
            "Digital Ruler: calibrated at %.1f px/cm".format(pxPerCm)
        } else {
            "Digital Ruler: not calibrated"
        }
    }
}
