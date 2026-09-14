package com.gokova.myanimelist.core.database.entity

import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class GenreEntity(
    val id: Int,
    val name: String,
)
