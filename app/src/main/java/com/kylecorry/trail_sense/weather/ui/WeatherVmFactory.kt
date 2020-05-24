package com.kylecorry.trail_sense.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kylecorry.trail_sense.shared.livesensors.ILiveBarometer

class WeatherVmFactory(private val barometer: ILiveBarometer): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return WeatherViewModel(barometer) as T
    }
}