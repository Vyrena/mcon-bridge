package com.vyrena.mconbridge.data

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun sourceToString(value: SourceType): String = value.name

    @TypeConverter
    fun stringToSource(value: String): SourceType = SourceType.valueOf(value)
}
