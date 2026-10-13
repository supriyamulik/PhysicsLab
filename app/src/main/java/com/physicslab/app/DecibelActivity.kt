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
import com.physicslab.app.databinding.ActivityDecibelBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

class DecibelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecibelBinding
    private var audioRecord: AudioRecord? = null
    private var listenJob: Job? = null
    private var listening = false

    companion object {
        private const val REQ_RECORD_AUDIO = 602
        private const val SAMPLE_RATE = 44100
        private const val REFERENCE = 1.0 // arbitrary reference for a relative, uncalibrated dB scale
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecibelBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }
        binding.txtSensorInfo.text = "Microphone: system default, ${SAMPLE_RATE} Hz PCM"

        binding.btnListen.setOnClickListener {
            if (!hasPermission()) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_RECORD_AUDIO)
                return@setOnClickListener
            }
            if (listening) stopListening() else startListening()
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.txtUnavailable.visibility = View.GONE
                startListening()
            } else {
                binding.txtUnavailable.visibility = View.VISIBLE
            }
        }
    }

    @Suppress("MissingPermission")
    private fun startListening() {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) {
            binding.txtUnavailable.text = "⚠ This device does not support the required audio configuration."
            binding.txtUnavailable.visibility = View.VISIBLE
            return
        }
        val record = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
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
            val buffer = ShortArray(2048)
            while (listening) {
                val read = record.read(buffer, 0, buffer.size)
                if (read <= 0) continue

                var sumSquares = 0.0
                for (i in 0 until read) sumSquares += buffer[i].toDouble() * buffer[i].toDouble()
                val rms = sqrt(sumSquares / read)
                val db = if (rms > 1.0) 20.0 * log10(rms / REFERENCE) else 0.0

                withContext(Dispatchers.Main) {
                    binding.txtResultant.text = String.format("%.0f", db)
                    ReadingLogger.logThrottled(this@DecibelActivity, lifecycleScope, "DECIBEL", "Sound level (relative)", db, "dB")
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

    override fun onPause() {
        super.onPause()
        if (listening) stopListening()
    }
}
