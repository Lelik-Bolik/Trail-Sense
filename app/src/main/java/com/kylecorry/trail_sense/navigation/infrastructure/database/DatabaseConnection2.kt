package com.kylecorry.trail_sense.navigation.infrastructure.database

import android.content.Context
import android.database.Cursor
import android.database.CursorWrapper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.*
import androidx.core.database.sqlite.transaction
import com.kylecorry.trailsensecore.infrastructure.persistence.SqlType
import java.lang.Exception

/**
 * A database connection
 * @param context The context
 * @param dbName The name of the database to connect to
 * @param version The version of the database
 * @param onCreate The code to run when the database is first created - DO NOT OPEN OR CLOSE THE CONNECTION HERE
 * @param onUpgrade The code to run when the database version changes - DO NOT OPEN OR CLOSE THE CONNECTION HERE
 */
class DatabaseConnection2(
    context: Context,
    dbName: String,
    version: Int,
    onCreate: (connection: DatabaseConnection2) -> Unit,
    onUpgrade: (connection: DatabaseConnection2, oldVersion: Int, newVersion: Int) -> Unit
) {
    private val helper: SQLiteOpenHelper

    private var database: SQLiteDatabase? = null

    init {
        val inst = this
        helper = object : SQLiteOpenHelper(context, dbName, null, version) {
            override fun onCreate(db: SQLiteDatabase?) {
                database = db
                onCreate(inst)
            }

            override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                database = db
                onUpgrade(inst, oldVersion, newVersion)
            }
        }
    }

    fun transaction(fn: () -> Unit) {
        guardClosedDb()
        database?.transaction {
            fn()
        }
    }

    fun execute(sql: String, args: Array<String?>? = null) {
        guardClosedDb()
        if (args == null) {
            database?.execSQL(sql)
        } else {
            database?.execSQL(sql, args)
        }
    }

    inline fun <reified T> query(sql: String, args: Array<String?>? = null): T? =
        query(T::class.java, sql, args)

    fun <T> query(dtoClass: Class<T>, sql: String, args: Array<String?>? = null): T? {
        guardClosedDb()
        val cursor = database?.rawQuery(sql, args)
        var dto: T? = null
        cursor?.apply {
            if (count != 0) {
                moveToFirst()
                dto = MyCursor2(cursor).get(dtoClass)
            }
        }
        cursor?.close()
        return dto
    }

    inline fun <reified T> queryAll(sql: String, args: Array<String?>? = null): Collection<T> =
        queryAll(T::class.java, sql, args)

    fun <T> queryAll(
        dtoClass: Class<T>,
        sql: String,
        args: Array<String?>? = null
    ): Collection<T> {
        guardClosedDb()
        val cursor = database?.rawQuery(sql, args)
        val list = mutableListOf<T>()
        cursor?.apply {
            if (count != 0) {
                moveToFirst()
                val myCursor = MyCursor2(this)
                while (!isAfterLast) {
                    val obj = myCursor.get(dtoClass) ?: continue
                    list.add(obj)
                    moveToNext()
                }
            }
        }
        cursor?.close()
        return list
    }

    fun open() {
        if (database != null) {
            return
        }
        database?.close()
        database = helper.writableDatabase
    }

    fun close() {
        database?.close()
        database = null
    }

    private fun guardClosedDb() {
        database
            ?: throw Exception("Database is closed, please call DatabaseConnection.open() before using.")
    }

    private class MyCursor2(cursor: Cursor) : CursorWrapper(cursor) {
        fun <T> get(mClass: Class<T>): T? {
            val con = mClass.constructors.firstOrNull() ?: return null
            val annotations = con.parameterAnnotations


            val columns = annotations.mapNotNull {
                it.find { a -> a is Column } as? Column
            }

            if (annotations.size != columns.size) {
                // Could not map all parameters
                return null
            }

            val params = mutableListOf<Any?>()

            for (column in columns) {
                val columnIdx = getColumnIndex(column.columnName)

                val value: Any? = when (column.type) {
                    SqlType.Short -> getShort(columnIdx)
                    SqlType.Int -> getInt(columnIdx)
                    SqlType.Long -> getLong(columnIdx)
                    SqlType.Float -> getFloat(columnIdx)
                    SqlType.Double -> getDouble(columnIdx)
                    SqlType.String -> getString(columnIdx)
                    SqlType.Boolean -> getInt(columnIdx) == 1
                    SqlType.NullableShort -> getShortOrNull(columnIdx)
                    SqlType.NullableInt -> getIntOrNull(columnIdx)
                    SqlType.NullableLong -> getLongOrNull(columnIdx)
                    SqlType.NullableFloat -> getFloatOrNull(columnIdx)
                    SqlType.NullableDouble -> getDoubleOrNull(columnIdx)
                    SqlType.NullableString -> getStringOrNull(columnIdx)
                    SqlType.NullableBoolean -> {
                        val value = getIntOrNull(columnIdx)
                        if (value == null) null else value == 1
                    }
                    else -> null
                }

                params.add(value)
            }

            if (params.size != columns.size) {
                return null
            }

            return con.newInstance(*params.toTypedArray()) as T
        }
    }
}