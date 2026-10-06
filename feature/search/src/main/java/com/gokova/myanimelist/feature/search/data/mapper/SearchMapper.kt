package com.gokova.myanimelist.feature.search.data.mapper

import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem

object SearchMapper {
    fun toDomain(
        dto: AnimeListEntryDto,
        localUserStatus: String?,
        tasteMatchPercent: Int?,
    ): SearchAnimeItem {
        val node = dto.node
        val englishTitle = node.alternativeTitles?.en?.takeIf { it.isNotBlank() }
        val displayTitle = englishTitle ?: node.title
        val subtitleTitle =
            if (englishTitle != null && !englishTitle.equals(node.title, ignoreCase = true)) {
                node.title
            } else {
                null
            }
        val genres = node.genres?.map { it.name } ?: emptyList()
        val userStatus = localUserStatus ?: dto.listStatus?.status

        return SearchAnimeItem(
            id = node.id,
            title = node.title,
            englishTitle = englishTitle,
            displayTitle = displayTitle,
            subtitleTitle = subtitleTitle,
            thumbnailUrl = node.mainPicture?.large ?: node.mainPicture?.medium,
            mediaType = node.mediaType,
            numEpisodes = node.numEpisodes,
            meanScore = node.mean,
            genres = genres,
            userStatus = userStatus,
            tasteMatchPercent = tasteMatchPercent,
        )
    }
}
