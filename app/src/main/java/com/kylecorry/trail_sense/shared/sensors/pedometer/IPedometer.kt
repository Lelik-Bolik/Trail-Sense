package com.kylecorry.trail_sense.shared.sensors.pedometer

import com.kylecorry.trailsensecore.infrastructure.sensors.ISensor

interface IPedometer: ISensor {

    val steps: Int

}