package com.kylecorry.trail_sense.weather.domain.sealevel

import android.hardware.SensorManager
import com.kylecorry.trail_sense.weather.domain.AltitudeReading
import com.kylecorry.trail_sense.weather.domain.PressureAltitudeReading
import java.time.Duration
import kotlin.math.abs

internal class BarometerGPSAltitudeCalculator(private val maxNaturalPressureChange: Float = 3f) :
    IAltitudeCalculator {
    override fun convert(readings: List<PressureAltitudeReading>): List<AltitudeReading> {

        if (readings.isEmpty()) return listOf()

        var lastReading = readings.first()

        var altitude = lastReading.altitude

        return readings.map {
            if (it != lastReading) {
                if (hasAltitudeChanged(lastReading, it)) {
                    val barometerAltitude = altitude + getAltitudeChange(lastReading.pressure, it.pressure)
                    val alpha = 0.5f
                    altitude = it.altitude * alpha + barometerAltitude * (1 - alpha)
                }
            }
            lastReading = it
            AltitudeReading(
                it.time,
                altitude
            )
        }
    }

    private fun hasAltitudeChanged(lastReading: PressureAltitudeReading, currentReading: PressureAltitudeReading): Boolean {
        val dt = Duration.between(lastReading.time, currentReading.time).toMillis() * MILLIS_TO_HOURS
        val dpdt = (currentReading.pressure - lastReading.pressure) / dt
        val da = (currentReading.altitude - lastReading.altitude)
        return abs(dpdt) > maxNaturalPressureChange && abs(da) > MAX_ALTITUDE_DIFF
    }

    private fun getAltitudeChange(lastPressure: Float, currentPressure: Float): Float {
        val lastBarometricAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, lastPressure)
        val currentBarometricAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure)
        return currentBarometricAltitude - lastBarometricAltitude
    }

    companion object {
        private const val MILLIS_TO_HOURS = 1f / (1000f * 60f * 60f)
        private const val MAX_ALTITUDE_DIFF = 10f
    }
}