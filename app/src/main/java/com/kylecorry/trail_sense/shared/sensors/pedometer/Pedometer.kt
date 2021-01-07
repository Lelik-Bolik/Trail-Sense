package com.kylecorry.trail_sense.shared.sensors.pedometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.trailsensecore.infrastructure.sensors.BaseSensor
import kotlin.math.roundToInt

class Pedometer(context: Context) :
    BaseSensor(context, Sensor.TYPE_STEP_COUNTER, SensorManager.SENSOR_DELAY_NORMAL), IPedometer {
    override val steps: Int
        get() = _steps
    override val hasValidReading: Boolean
        get() = _hasReading

    private var _steps = 0
    private var _hasReading = false

    override fun handleSensorEvent(event: SensorEvent) {
        if (event.values.isEmpty()) {
            return
        }
        _steps = event.values[0].roundToInt()
        _hasReading = true
    }
}