package com.kylecorry.trail_sense.navigation.domain

import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.domain.Coordinate

data class NavigationVector(val direction: Bearing, val distance: Float)


data class NavigationDelta(val bearing: Float, val distance: Float?, val elevationChange: Float?)


data class Position(val location: Coordinate, val elevation: Float, val bearing: Bearing)