package com.physicslab.app.ui

import com.physicslab.app.data.Quantity
import com.physicslab.app.sensor.FakeSensorSource
import com.physicslab.app.sensor.SensorSource
import com.physicslab.app.sensor.SensorSourceProvider

class FakeSensorSourceProvider : SensorSourceProvider {

    val sources = mapOf(
        Quantity.MAGNETIC_FIELD to FakeSensorSource(Quantity.MAGNETIC_FIELD.sensorType),
        Quantity.ACCELERATION to FakeSensorSource(Quantity.ACCELERATION.sensorType),
        Quantity.LIGHT to FakeSensorSource(Quantity.LIGHT.sensorType)
    )

    override fun getSource(quantity: Quantity): SensorSource {
        return sources[quantity] ?: FakeSensorSource(quantity.sensorType)
    }

    fun getFake(quantity: Quantity): FakeSensorSource {
        return sources[quantity] ?: error("Unknown quantity")
    }
}
