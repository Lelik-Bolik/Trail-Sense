package com.kylecorry.trail_sense.shared.livesensors

import android.content.Context
import androidx.lifecycle.LiveData

class LiveBarometer(private val context: Context): ILiveBarometer {
    override val pressure: LiveData<Float>
        get() = PressureLiveData(context)
}