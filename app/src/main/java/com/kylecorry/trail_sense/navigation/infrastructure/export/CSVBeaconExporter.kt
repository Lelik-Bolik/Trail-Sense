package com.kylecorry.trail_sense.navigation.infrastructure.export

import android.app.Activity
import android.net.Uri
import androidx.core.app.ShareCompat
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import java.io.File
import java.io.FileWriter

class CSVBeaconExporter(private val activity: Activity) : IBeaconExporter {
    override fun export(beacons: Collection<Beacon>) {
        val csv = toCSV(beacons)

        // TODO: Download as file instead
        val chooser = ShareCompat.IntentBuilder
            .from(activity)
            .setType("text/plain")
            .setText(csv)
            .createChooserIntent()

        activity.startActivity(chooser)
    }

    private fun toCSV(beacons: Collection<Beacon>): String {
        return beacons.joinToString(separator = "\n") { toCSV(it) }
    }

    private fun toCSV(beacon: Beacon): String {
        return "${beacon.name},${beacon.coordinate.latitude},${beacon.coordinate.longitude},${beacon.elevation},${beacon.comment}"
    }



}