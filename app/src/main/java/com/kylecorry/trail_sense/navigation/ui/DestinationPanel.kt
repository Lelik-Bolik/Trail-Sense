package com.kylecorry.trail_sense.navigation.ui

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.navigation.domain.*
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.formatHM
import com.kylecorry.trail_sense.shared.roundPlaces
import com.kylecorry.trail_sense.shared.system.UiUtils
import kotlin.math.roundToInt

class DestinationPanel(private val view: View) {

    private val beaconName = view.findViewById<TextView>(R.id.beacon_name)
    private val beaconComments = view.findViewById<ImageButton>(R.id.beacon_comment_btn)
    private val beaconDistance = view.findViewById<TextView>(R.id.beacon_distance)
    private val beaconDirection = view.findViewById<TextView>(R.id.beacon_direction)
    private val beaconDirectionCardinal = view.findViewById<TextView>(R.id.beacon_direction_cardinal)
    private val beaconElevationView = view.findViewById<LinearLayout>(R.id.beacon_elevation_view)
    private val beaconElevation = view.findViewById<TextView>(R.id.beacon_elevation)
    private val beaconElevationDiff = view.findViewById<TextView>(R.id.beacon_elevation_diff)
    private val beaconEta = view.findViewById<TextView>(R.id.beacon_eta)
    private val navigationService = NavigationService()
    private val context = view.context
    private var beacon: Beacon? = null

    init {
        beaconComments.setOnClickListener {
            if (beacon?.comment != null) {
                UiUtils.alert(context, beacon?.name ?: "", beacon?.comment ?: "")
            }
        }
    }

    fun show(position: Position, destination: Beacon, delta: NavigationDelta){
        view.visibility = View.VISIBLE

        beacon = destination

        if (destination.comment != null) {
            beaconComments.visibility = View.VISIBLE
        } else {
            beaconComments.visibility = View.GONE
        }

        beaconName.text = destination.name
        updateDestinationDirection(position.bearing, delta)
        updateDestinationElevation(destination, delta)
        updateDestinationEta(delta, position.speed)
    }

    fun hide(){
        view.visibility = View.GONE
        beacon = null
    }

    private fun updateDestinationDirection(azimuth: Bearing, delta: NavigationDelta){
        val destinationBearing = azimuth.plus(delta.bearing)
        beaconDirection.text = context.getString(R.string.degree_format, destinationBearing.value.roundToInt())
        beaconDirectionCardinal.text = destinationBearing.direction.symbol
    }

    private fun updateDestinationEta(delta: NavigationDelta, speed: Float){
        if (delta.distance != null) {
            // TODO: Select base unit if small enough
            beaconDistance.text =
                formatDistance(toUnits(delta.distance, DistanceUnits.Miles), DistanceUnits.Miles)
            val eta = navigationService.eta(delta, speed)?.formatHM(true) // TODO: Get average speed
            beaconEta.text =
                if (eta == null) context.getString(R.string.distance_away) else context.getString(R.string.eta, eta)
        }
    }

    private fun updateDestinationElevation(destination: Beacon, delta: NavigationDelta){
        if (delta.elevationChange != null && destination.elevation != null) {
            beaconElevationView.visibility = View.VISIBLE
            // TODO: Get actual distance units
            beaconElevation.text = formatDistance(toUnits(destination.elevation, DistanceUnits.Feet), DistanceUnits.Feet)

            val direction = when {
                delta.elevationChange == 0.0f -> ""
                delta.elevationChange > 0 -> "+"
                else -> "-"
            }

            beaconElevationDiff.text = direction + formatDistance(toUnits(delta.elevationChange, DistanceUnits.Feet), DistanceUnits.Feet)
            val changeColor = when {
                delta.elevationChange >= 0 -> {
                    R.color.positive
                }
                else -> {
                    R.color.negative
                }
            }
            beaconElevationDiff.setTextColor(context.resources.getColor(changeColor, null))
        } else {
            beaconElevationView.visibility = View.GONE
        }
    }

    private fun toUnits(meters: Float, units: DistanceUnits): Float {
        val prefUnits = when(units){
            DistanceUnits.Feet, DistanceUnits.Miles -> UserPreferences.DistanceUnits.Feet
            else -> UserPreferences.DistanceUnits.Meters
        }

        return if (units == DistanceUnits.Meters || units == DistanceUnits.Feet){
            LocationMath.convertToBaseUnit(meters, prefUnits)
        } else {
            LocationMath.convertToBigUnit(meters, prefUnits)
        }
    }

    private fun formatDistance(distance: Float, units: DistanceUnits): String {
        val roundedDistance: Number =
            if (units == DistanceUnits.Meters || units == DistanceUnits.Feet) distance.roundToInt() else distance.roundPlaces(
                2
            )
        return when (units) {
            DistanceUnits.Feet -> "$roundedDistance ft"
            DistanceUnits.Meters -> "$roundedDistance m"
            DistanceUnits.Kilometers -> "$roundedDistance km"
            DistanceUnits.Miles -> "$roundedDistance mi"
        }
    }

}