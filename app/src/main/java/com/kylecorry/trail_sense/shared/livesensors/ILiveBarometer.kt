package com.kylecorry.trail_sense.shared.livesensors

import androidx.lifecycle.LiveData

interface ILiveBarometer {

    val pressure: LiveData<Float>

}