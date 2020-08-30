package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.navigation.domain.compass.DeclinationCalculator
import com.kylecorry.trail_sense.shared.domain.Coordinate

class NavigationService {
    
    fun navigate(from: Coordinate, to: Coordinate, declination: Float = 0.0f): NavigationVector {
        val results = FloatArray(3)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return NavigationVector(Bearing(results[1]).withDeclination(-declination), results[0])
    }

}