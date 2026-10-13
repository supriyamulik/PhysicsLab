package com.physicslab.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.physicslab.app.data.ReadingLogger
import com.physicslab.app.databinding.ActivitySoundFrequencyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Detects the dominant frequency of live microphone audio using the Goertzel
 * algorithm evaluated across a bank of candidate frequencies (50 Hz - 2000 Hz,
 * 10 Hz steps). Goertzel is efficient when you only care about a finite set of
 * target frequencies, unlike a full FFT which computes every bin.
 */
class SoundFrequencyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySoundFrequencyBinding
    private var audioRecord: AudioRecord? = null
    private var listenJob: Job? = null
    private var listening = false

    companion object {
        private const val REQ_RECORD_AUDIO = 601
        private const val SAMPLE_RATE = 44100
        private const val FRAME_SIZE = 4096
        private const val MIN_FREQ = 60.0
        private const val MAX_FREQ = 2000.0
        private const val STEP_FREQ = 10.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoundFrequencyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }
        binding.txtSensorInfo.text = "Microphone: system default, ${SAMPLE_RATE} Hz PCM"

        binding.btnListen.setOnClickListener {
            if (!hasPermission()) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_RECORD_AUDIO)
                return@setOnClickListener
            }
            toggleListening()
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.txtUnavailable.visibility = View.GONE
                toggleListening()
            } else {
                binding.txtUnavailable.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleListening() {
        if (listening) stopListening() else startListening()
    }

    @Suppress("MissingPermission")
    private fun startListening() {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) {
            binding.txtUnavailable.text = "⚠ This device does not support the required audio configuration."
            binding.txtUnavailable.visibility = View.VISIBLE
            return
        }
        val bufferSize = maxOf(minBuf, FRAME_SIZE * 2)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            binding.txtUnavailable.text = "⚠ Could not initialize the microphone."
            binding.txtUnavailable.visibility = View.VISIBLE
            return
        }
        audioRecord = record
        listening = true
        binding.btnListen.text = "Stop Listening"
        record.startRecording()

        listenJob = lifecycleScope.launch(Dispatchers.Default) {
            val buffer = ShortArray(FRAME_SIZE)
            while (listening) {
                val read = record.read(buffer, 0, FRAME_SIZE)
                if (read <= 0) continue

                val samples = DoubleArray(read) { buffer[it].toDouble() }
                val freq = dominantFrequency(samples)

                withContext(Dispatchers.Main) {
                    if (freq > 0) {
                        binding.txtResultant.text = String.format("%.0f", freq)
                        binding.txtNoteLabel.text = "Hz"
                        ReadingLogger.logThrottled(
                            this@SoundFrequencyActivity, lifecycleScope,
                            "SOUND_FREQUENCY", "Dominant frequency", freq, "Hz"
                        )
                    }
                }
            }
        }
    }

    private fun stopListening() {
        listening = false
        listenJob?.cancel()
        audioRecord?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }
        audioRecord = null
        binding.btnListen.text = "Start Listening"
    }

    /** Goertzel algorithm: returns the energy of [samples] at [targetFreq] Hz. */
    private fun goertzelEnergy(samples: DoubleArray, targetFreq: Double): Double {
        val n = samples.size
        val k = (0.5 + (n * targetFreq) / SAMPLE_RATE).toInt()
        val omega = (2.0 * PI * k) / n
        val coeff = 2.0 * cos(omega)
        var q0: Double; var q1 = 0.0; var q2 = 0.0
        for (sample in samples) {
            q0 = coeff * q1 - q2 + sample
            q2 = q1
            q1 = q0
        }
        return q1 * q1 + q2 * q2 - q1 * q2 * coeff
    }

    private fun dominantFrequency(samples: DoubleArray): Double {
        // Skip near-silent buffers so we don't report noise as a "frequency".
        val rms = sqrt(samples.sumOf { it * it } / samples.size)
        if (rms < 300.0) return -1.0

        var bestFreq = -1.0
        var bestEnergy = 0.0
        var f = MIN_FREQ
        while (f <= MAX_FREQ) {
            val e = goertzelEnergy(samples, f)
            if (e > bestEnergy) { bestEnergy = e; bestFreq = f }
            f += STEP_FREQ
        }
        return bestFreq
    }

    override fun onPause() {
        super.onPause()
        if (listening) stopListening()
    }
}
