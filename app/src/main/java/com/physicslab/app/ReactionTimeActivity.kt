package com.physicslab.app

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.physicslab.app.data.ReadingLogger
import com.physicslab.app.databinding.ActivityReactionBinding
import kotlin.random.Random

/**
 * Classic reaction-time test. States:
 *  IDLE -> user taps Start -> WAITING (random 1.5-4s delay, card gray) ->
 *  READY (card turns green, timer starts via System.nanoTime()) -> user taps
 *  the card -> elapsed time measured and shown.
 * Tapping too early during WAITING is flagged as a false start.
 */
class ReactionTimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReactionBinding
    private val handler = Handler(Looper.getMainLooper())
    private var state = State.IDLE
    private var readyAtNanos = 0L
    private var bestMs: Long? = null
    private var waitingRunnable: Runnable? = null

    private enum class State { IDLE, WAITING, READY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }

        binding.btnStart.setOnClickListener { startTrial() }
        binding.reactionArea.setOnClickListener { onAreaTapped() }
    }

    private fun startTrial() {
        state = State.WAITING
        binding.txtPrompt.text = "Wait for green..."
        binding.reactionArea.setBackgroundColor(Color.parseColor("#334155"))
        binding.btnStart.isEnabled = false

        val delayMs = Random.nextLong(1500, 4000)
        val r = Runnable {
            state = State.READY
            readyAtNanos = System.nanoTime()
            binding.txtPrompt.text = "TAP NOW!"
            binding.reactionArea.setBackgroundColor(Color.parseColor("#059669"))
        }
        waitingRunnable = r
        handler.postDelayed(r, delayMs)
    }

    private fun onAreaTapped() {
        when (state) {
            State.WAITING -> {
                waitingRunnable?.let { handler.removeCallbacks(it) }
                state = State.IDLE
                binding.txtPrompt.text = "Too early! Tap Start to retry"
                binding.reactionArea.setBackgroundColor(Color.parseColor("#F87171"))
                binding.btnStart.isEnabled = true
            }
            State.READY -> {
                val elapsedNs = System.nanoTime() - readyAtNanos
                val elapsedMs = elapsedNs / 1_000_000
                binding.txtLast.text = "$elapsedMs ms"
                if (bestMs == null || elapsedMs < bestMs!!) {
                    bestMs = elapsedMs
                    binding.txtBest.text = "$elapsedMs ms"
                }
                binding.txtPrompt.text = "Tap Start"
                binding.reactionArea.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.lab_surface))
                state = State.IDLE
                binding.btnStart.isEnabled = true

                ReadingLogger.log(this, lifecycleScope, "REACTION_TIME", "Reaction time", elapsedMs.toDouble(), "ms")
            }
            State.IDLE -> { /* ignore taps outside a trial */ }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        waitingRunnable?.let { handler.removeCallbacks(it) }
    }
}
