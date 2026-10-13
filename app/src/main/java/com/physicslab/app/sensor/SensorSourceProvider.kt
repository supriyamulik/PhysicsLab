package com.physicslab.app.sensor

import android.content.Context
import com.physicslab.app.data.Quantity

/**
 * Provider interface supplying [SensorSource] instances for physical quantities.
 */
interface SensorSourceProvider {

    /** Returns the [SensorSource] corresponding to the given [Quantity]. */
    fun getSource(quantity: Quantity): SensorSource
}

/**
 * Default production implementation supplying [AndroidSensorSource] instances.
 */
class DefaultSensorSourceProvider(private val context: Context) : SensorSourceProvider {

    private val cachedSources = mutableMapOf<Quantity, SensorSource>()

    override fun getSource(quantity: Quantity): SensorSource {
        return synchronized(cachedSources) {
            cachedSources.getOrPut(quantity) {
                AndroidSensorSource(
                    context = context,
                    sensorType = quantity.sensorType
                )
            }
        }
    }
}
