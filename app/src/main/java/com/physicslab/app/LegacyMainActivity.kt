package com.physicslab.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.physicslab.app.databinding.ActivityMainLegacyBinding

/**
 * Legacy landing screen preserved for backward compatibility and test verification.
 */
class LegacyMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainLegacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainLegacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardMagnetic.setOnClickListener {
            startActivity(Intent(this, MagneticFieldActivity::class.java))
        }
        binding.cardAcceleration.setOnClickListener {
            startActivity(Intent(this, AccelerationActivity::class.java))
        }
        binding.cardLight.setOnClickListener {
            startActivity(Intent(this, LightActivity::class.java))
        }
        binding.cardGyroscope.setOnClickListener {
            startActivity(Intent(this, GyroscopeActivity::class.java))
        }
        binding.cardCompass.setOnClickListener {
            startActivity(Intent(this, CompassActivity::class.java))
        }
        binding.cardBarometer.setOnClickListener {
            startActivity(Intent(this, BarometerActivity::class.java))
        }
        binding.cardStepCounter.setOnClickListener {
            startActivity(Intent(this, StepCounterActivity::class.java))
        }
        binding.cardSoundFrequency.setOnClickListener {
            startActivity(Intent(this, SoundFrequencyActivity::class.java))
        }
        binding.cardDecibel.setOnClickListener {
            startActivity(Intent(this, DecibelActivity::class.java))
        }
        binding.cardRuler.setOnClickListener {
            startActivity(Intent(this, RulerActivity::class.java))
        }
        binding.cardReactionTime.setOnClickListener {
            startActivity(Intent(this, ReactionTimeActivity::class.java))
        }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
