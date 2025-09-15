package com.jetbrains.greeting.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromMapLongInt(map: Map<Long, Int>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
    }

    @TypeConverter
    fun toMapLongInt(data: String): Map<Long, Int> {
        return if (data.isEmpty()) emptyMap()
        else data.split(";").associate {
            val (key, value) = it.split(":")
            key.toLong() to value.toInt()
        }
    }

} 