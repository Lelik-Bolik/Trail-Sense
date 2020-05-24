package com.kylecorry.trail_sense.shared.livesensors

import android.content.Context
import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.shared.sensors.Barometer

class PressureLiveData(context: Context): LiveData<Float>() {

    private val barometer: Barometer = Barometer(context)

    override fun onActive() {
        super.onActive()
        barometer.start(this::onBarometerUpdate)
    }

    override fun onInactive() {
        super.onInactive()
        barometer.stop(this::onBarometerUpdate)
    }

    private fun onBarometerUpdate(): Boolean {
        value = barometer.pressure
        return true
    }

}