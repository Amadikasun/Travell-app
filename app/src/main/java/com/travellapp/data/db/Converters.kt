package com.travellapp.data.db

import androidx.room.TypeConverter
import com.travellapp.data.model.AttractionType

class Converters {
    @TypeConverter
    fun fromAttractionType(type: AttractionType): String = type.name

    @TypeConverter
    fun toAttractionType(value: String): AttractionType = AttractionType.valueOf(value)
}
