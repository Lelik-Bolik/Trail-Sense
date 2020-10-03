package com.kylecorry.trail_sense.weather.domain.forcasting

import android.database.CursorWrapper
import android.os.Parcel
import com.kylecorry.trail_sense.navigation.infrastructure.database.Beacon2
import com.kylecorry.trail_sense.navigation.infrastructure.database.Column
import com.kylecorry.trail_sense.navigation.infrastructure.database.Dto
import com.kylecorry.trailsensecore.domain.geo.Coordinate
import org.junit.Test
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

class DtoTest {

    @Test
    fun test(){
        val c = Beacon2::class
        val dto = (c.annotations.find { it is Dto } as? Dto) ?: return

        val tableName = dto.tableName

        val con = c.primaryConstructor ?: return
        val parameters = con.valueParameters ?: return


        val columns = parameters.mapNotNull {
            it.findAnnotation<Column>()
        }

        if (parameters.size != columns.size){
            // Could not map all parameters
            return
        }


        val d = con.call(1L, "test")

        println(d)
    }

    inline fun <reified T> mapTo(cursorWrapper: CursorWrapper): T? {

        val dto = (T::class.annotations.find { it is Dto } as? Dto) ?: return null

        val tableName = dto.tableName

        val con = T::class.constructors.firstOrNull() ?: return null
        val parameters = con.valueParameters


        val columns = parameters.mapNotNull {
            it.findAnnotation<Column>()
        }

        if (parameters.size != columns.size){
            // Could not map all parameters
            return null
        }

        val params = listOf<Any?>()

        for (column in columns){
            val index = cursorWrapper.getColumnIndex(column.columnName)
        }
    }

}