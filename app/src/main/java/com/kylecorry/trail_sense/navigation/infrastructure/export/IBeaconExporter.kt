package com.kylecorry.trail_sense.navigation.infrastructure.export

import com.kylecorry.trailsensecore.domain.navigation.Beacon

interface IBeaconExporter {

    fun export(beacons: Collection<Beacon>)

}