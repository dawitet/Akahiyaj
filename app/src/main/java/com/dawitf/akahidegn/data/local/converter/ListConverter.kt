package com.dawitf.akahidegn.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun <T> fromList(value: List<T>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun <T> toList(value: String?): List<T>? {
        if (value == null) return null
        val listType = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(value, listType)
    }
} 