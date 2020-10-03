package com.kylecorry.trail_sense.navigation.infrastructure.database

import com.kylecorry.trailsensecore.infrastructure.persistence.SqlType

@Dto("beacons")
data class Beacon2(
    @Column("_id", SqlType.Long) @PrimaryKey val id: Long,
    @Column("name", SqlType.String) val name: String
)