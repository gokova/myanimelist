package com.gokova.myanimelist.feature.details.domain.model

data class RelatedAnime(
    val id: Long,
    val title: String,
    val thumbnailUrl: String?,
    val relationType: String,
    val relationTypeFormatted: String,
)
