package com.android.hms.db

import java.util.*

/**
 * Created by SivaMalini on 15-03-2018.
 */

class Converters {
//    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

//    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}