package com.kylecorry.trail_sense.navigation.domain

import android.location.Location
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.domain.Coordinate
import java.time.Duration
import kotlin.math.roundToLong

class NavigationService {

    fun navigate(from: Coordinate, to: Coordinate, declination: Float = 0.0f): NavigationVector {
        val results = FloatArray(3)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return NavigationVector(Bearing(results[1]).withDeclination(-declination), results[0])
    }

    fun delta(from: Position, to: Beacon, declination: Float = 0.0f): NavigationDelta {
        val vector = navigate(from.location, to.coordinate, declination)
        val angleBetween = from.bearing.angleTo(vector.direction)
        val deltaElevation = if (to.elevation == null) null else to.elevation - from.elevation
        return NavigationDelta(angleBetween, vector.distance, deltaElevation)
    }

    fun eta(delta: NavigationDelta, speed: Float): Duration? {
        val distance = delta.distance
        if (speed == 0.0f || distance == null){
            return null
        }

        return Duration.ofSeconds((distance / speed).roundToLong())
    }

}