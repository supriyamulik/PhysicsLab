package com.physicslab.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Persists the user's chosen theme (Light / Dark / follow System) in
 * SharedPreferences and applies it via AppCompatDelegate, which is the
 * officially supported way to force day/night mode across the whole app.
 */
object ThemeManager {

    private const val PREFS = "physicslab_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    const val MODE_LIGHT = "light"

    fun getMode(context: Context): String = MODE_LIGHT

    fun setMode(context: Context, mode: String) {
        // App is strictly light theme only as specified.
        apply(MODE_LIGHT)
    }

    /** Call once at process start (Application.onCreate) and whenever the mode changes. */
    fun apply(mode: String = MODE_LIGHT) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    // --- Calibration preferences (digital ruler pixels-per-cm ratio) ---

    private const val KEY_RULER_CALIBRATION = "ruler_px_per_cm"

    fun getRulerCalibration(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_RULER_CALIBRATION, -1f)
    }

    fun setRulerCalibration(context: Context, pxPerCm: Float) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_RULER_CALIBRATION, pxPerCm).apply()
    }

    // --- Accelerometer zero-offset calibration ---

    fun getAccelOffset(context: Context): FloatArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return floatArrayOf(
            prefs.getFloat("accel_offset_x", 0f),
            prefs.getFloat("accel_offset_y", 0f),
            prefs.getFloat("accel_offset_z", 0f)
        )
    }

    fun setAccelOffset(context: Context, x: Float, y: Float, z: Float) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("accel_offset_x", x)
            .putFloat("accel_offset_y", y)
            .putFloat("accel_offset_z", z)
            .apply()
    }
}
