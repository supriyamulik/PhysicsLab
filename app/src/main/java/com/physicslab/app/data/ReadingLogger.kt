package com.physicslab.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Small helper used by every sensor screen to persist readings into Room.
 * logThrottled() is used for continuous sensors (gyroscope, magnetometer, ...)
 * so we don't write a row on every single onSensorChanged callback (which can
 * fire dozens of times per second) - only once per [minIntervalMs].
 * log() is used for explicit, one-off events (reaction time trial, ruler
 * measurement, a manual "Save reading" tap).
 */
object ReadingLogger {

    private val lastLoggedAt = HashMap<String, Long>()

    fun logThrottled(
        context: Context,
        scope: CoroutineScope,
        sensorType: String,
        label: String,
        value: Double,
        unit: String,
        minIntervalMs: Long = 3000L
    ) {
        val now = System.currentTimeMillis()
        val last = lastLoggedAt[sensorType] ?: 0L
        if (now - last < minIntervalMs) return
        lastLoggedAt[sensorType] = now
        log(context, scope, sensorType, label, value, unit)
    }

    fun log(
        context: Context,
        scope: CoroutineScope,
        sensorType: String,
        label: String,
        value: Double,
        unit: String
    ) {
        scope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(context).readingDao().insert(
                ReadingEntity(
                    sensorType = sensorType,
                    label = label,
                    value = value,
                    unit = unit,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
