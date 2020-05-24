package com.kylecorry.trail_sense.weather.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.kylecorry.trail_sense.shared.livesensors.ILiveBarometer

class WeatherViewModel(barometer: ILiveBarometer): ViewModel() {
    val pressure: LiveData<Float> = barometer.pressure
}