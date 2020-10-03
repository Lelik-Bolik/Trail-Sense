package com.kylecorry.trail_sense.navigation.infrastructure.database

import android.content.Context
import com.kylecorry.trailsensecore.domain.navigation.Beacon
import com.kylecorry.trailsensecore.domain.navigation.BeaconGroup
import com.kylecorry.trailsensecore.infrastructure.persistence.DatabaseConnection

class BeaconRepo2(context: Context) {

    private val conn: DatabaseConnection2

    init {
        conn = DatabaseConnection2(context, "survive", 4, { conn ->
            conn.transaction {
                createTables(conn)
            }
        }, { conn, oldVersion, newVersion ->
            conn.transaction {
                for (i in oldVersion..newVersion) {
                    when (i + 1) {
                        2 -> {
                            conn.execute("ALTER TABLE beacons ADD COLUMN visible INTEGER NOT NULL DEFAULT 1")
                        }
                        3 -> {
                            conn.execute("ALTER TABLE beacons ADD COLUMN comment TEXT NULL DEFAULT NULL")
                            conn.execute("ALTER TABLE beacons ADD COLUMN beacon_group_id INTEGER NULL DEFAULT NULL")
                        }
                        4 -> {
                            conn.execute("ALTER TABLE beacons ADD COLUMN elevation REAL NULL DEFAULT NULL")
                        }
                    }
                }
                createTables(conn)
            }
        })
    }

    fun get(id: Long): Beacon2? {
        conn.open()
        val beacon = conn.query<Beacon2>(
            "select * from beacons where _id = ?",
            arrayOf<String?>(id.toString())
        )
        conn.close()
        return beacon
    }

    fun getByGroup(groupId: Long?): Collection<Beacon2> {
        conn.open()
        val beacons = if (groupId == null) {
            conn.queryAll<Beacon2>("select * from beacons where beacon_group_id IS NULL"
            )
        } else {
            conn.queryAll<Beacon2>(
                "select * from beacons where beacon_group_id = ?",
                arrayOf(groupId.toString())
            )
        }
        conn.close()
        return beacons
    }

    fun get(): Collection<Beacon2> {
        conn.open()
        val beacons = conn.queryAll<Beacon2>("select * from beacons")
        conn.close()
        return beacons
    }

    fun getNumberOfBeaconsInGroup(groupId: Long?): Int {
        conn.open()
        val count = if (groupId == null) {
            conn.query<Int>(
                "select COUNT(_id) as cnt from beacons where beacon_group_id IS NULL"
            )
        } else {
            conn.query<Int>(
                "select COUNT(_id) as cnt from beacons where beacon_group_id = ?",
                arrayOf(groupId.toString())
            )
        } ?: 0
        conn.close()
        return count
    }

    fun getGroups(): Collection<BeaconGroup> {
        conn.open()
        val groups = conn.queryAll<BeaconGroup>("select * from beacon_groups")
        conn.close()
        return groups
    }

    fun getGroup(id: Long): BeaconGroup? {
        conn.open()
        val beacon = conn.query<BeaconGroup>("select * from beacon_groups where beacon_group_id = ?",
            arrayOf(id.toString())
        )
        conn.close()
        return beacon
    }

    fun delete(group: BeaconGroup) {
        conn.open()
        conn.transaction {
            conn.execute(
                "delete from beacons where beacon_group_id = ?",
                arrayOf(group.id.toString())
            )
            conn.execute(
                "delete from beacon_groups where beacon_group_id = ?",
                arrayOf(group.id.toString())
            )
        }
        conn.close()
    }

    fun delete(beacon: Beacon) {
        conn.open()
        conn.transaction {
            conn.execute("delete from beacons where _id = ?", arrayOf(beacon.id.toString()))
        }
        conn.close()
    }

    fun add(beacon: Beacon) {
        conn.open()
        conn.transaction {
            if (beacon.id == 0L) {
                // Create a new beacon
                conn.execute(
                    "insert into beacons (name, lat, lng, visible, comment, beacon_group_id, elevation) values (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(
                        beacon.name,
                        beacon.coordinate.latitude.toString(),
                        beacon.coordinate.longitude.toString(),
                        if (beacon.visible) "1" else "0",
                        beacon.comment,
                        beacon.beaconGroupId?.toString(),
                        beacon.elevation?.toString()
                    )
                )
            } else {
                // Update an existing beacon
                conn.execute(
                    "update beacons set name = ?, lat = ?, lng = ?, visible = ?, comment = ?, beacon_group_id = ?, elevation = ? where _id = ?",
                    arrayOf(
                        beacon.name,
                        beacon.coordinate.latitude.toString(),
                        beacon.coordinate.longitude.toString(),
                        if (beacon.visible) "1" else "0",
                        beacon.comment,
                        beacon.beaconGroupId?.toString(),
                        beacon.elevation?.toString(),
                        beacon.id.toString()
                    )
                )
            }
        }
        conn.close()
    }

    fun add(group: BeaconGroup) {
        conn.open()
        conn.transaction {
            if (group.id == 0L) {
                // Create a new beacon
                conn.execute(
                    "insert into beacon_groups (group_name) values (?)",
                    arrayOf(group.name)
                )
            } else {
                // Update an existing beacon
                conn.execute(
                    "update beacon_groups set group_name = ? where beacon_group_id = ?",
                    arrayOf(
                        group.name,
                        group.id.toString()
                    )
                )
            }
        }
        conn.close()
    }

    private fun createTables(conn: DatabaseConnection2) {
        conn.execute("CREATE TABLE IF NOT EXISTS beacons (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, visible INTEGER NOT NULL, comment TEXT NULL, beacon_group_id INTEGER NULL, elevation REAL NULL)")
        conn.execute("CREATE TABLE IF NOT EXISTS beacon_groups (beacon_group_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, group_name TEXT NOT NULL)")
    }

}