package com.example.leafreader.core.database

import androidx.room.TypeConverter
import com.example.leafreader.core.model.BookFormat
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.BookLocatorSerializer

class Converters {
    @TypeConverter
    fun fromBookFormat(format: BookFormat): String = format.name

    @TypeConverter
    fun toBookFormat(value: String): BookFormat = runCatching {
        BookFormat.valueOf(value)
    }.getOrDefault(BookFormat.TXT)

    @TypeConverter
    fun fromBookLocator(locator: BookLocator?): String? = BookLocatorSerializer.serialize(locator)

    @TypeConverter
    fun toBookLocator(value: String?): BookLocator? = BookLocatorSerializer.deserialize(value)
}
