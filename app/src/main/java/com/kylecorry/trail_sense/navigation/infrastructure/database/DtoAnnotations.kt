package com.kylecorry.trail_sense.navigation.infrastructure.database

import com.kylecorry.trailsensecore.infrastructure.persistence.SqlType

annotation class Dto(val tableName: String = "")
annotation class PrimaryKey
annotation class Column(val columnName: String, val type: SqlType)