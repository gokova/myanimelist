package com.gokova.myanimelist.core.database.converter

import androidx.room.TypeConverter
import com.gokova.myanimelist.core.database.entity.GenreEntity
import com.gokova.myanimelist.core.database.entity.StudioEntity
import kotlinx.serialization.json.Json

class AnimeTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromGenreList(genres: List<GenreEntity>?): String? = genres?.let { json.encodeToString(it) }

    @TypeConverter
    fun toGenreList(value: String?): List<GenreEntity>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromStudioList(studios: List<StudioEntity>?): String? = studios?.let { json.encodeToString(it) }

    @TypeConverter
    fun toStudioList(value: String?): List<StudioEntity>? = value?.let { json.decodeFromString(it) }
}
