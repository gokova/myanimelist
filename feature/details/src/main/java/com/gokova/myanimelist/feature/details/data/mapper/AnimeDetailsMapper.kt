package com.gokova.myanimelist.feature.details.data.mapper

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.GenreEntity
import com.gokova.myanimelist.core.database.entity.StudioEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.domain.taxonomy.partitionGenresAndThemes
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.feature.details.domain.model.AnimeDetails
import com.gokova.myanimelist.feature.details.domain.model.DetailRecommendation
import com.gokova.myanimelist.feature.details.domain.model.RelatedAnime
import java.util.Locale

object AnimeDetailsMapper {
    private const val MAX_RECOMMENDATIONS = 10

    fun toAnimeEntity(dto: AnimeDetailsDto): AnimeEntity {
        val englishTitle = dto.alternativeTitles?.en?.takeIf { it.isNotBlank() }
        val genres = dto.genres?.map { GenreEntity(id = it.id, name = it.name) }
        val studios = dto.studios?.map { StudioEntity(id = it.id, name = it.name) }
        return AnimeEntity(
            id = dto.id,
            title = dto.title,
            titleEnglish = englishTitle,
            mainPictureMedium = dto.mainPicture?.medium,
            mainPictureLarge = dto.mainPicture?.large,
            mediaType = dto.mediaType,
            airingStatus = dto.status,
            numEpisodes = dto.numEpisodes,
            startSeasonYear = dto.startSeason?.year,
            startSeasonSeason = dto.startSeason?.season,
            meanScore = dto.mean,
            genres = genres,
            studios = studios,
            source = dto.source,
            synopsis = dto.synopsis,
            rating = dto.rating,
            rank = dto.rank,
            popularity = dto.popularity,
            numListUsers = dto.numListUsers,
            averageEpisodeDuration = dto.averageEpisodeDuration,
            nsfw = dto.nsfw,
        )
    }

    fun toDomain(
        entity: AnimeEntity,
        userAnime: UserAnimeListEntity?,
    ): AnimeDetails {
        val englishTitle = entity.titleEnglish?.takeIf { it.isNotBlank() }
        val displayTitle = englishTitle ?: entity.title
        val subtitleTitle =
            if (englishTitle != null && !englishTitle.equals(entity.title, ignoreCase = true)) {
                entity.title
            } else {
                null
            }

        val rawTags = entity.genres?.map { it.name } ?: emptyList()
        val (genres, themes) = rawTags.partitionGenresAndThemes { it }

        return AnimeDetails(
            id = entity.id,
            displayTitle = displayTitle,
            subtitleTitle = subtitleTitle,
            originalTitle = entity.title,
            englishTitle = englishTitle,
            mainPictureMedium = entity.mainPictureMedium,
            mainPictureLarge = entity.mainPictureLarge,
            synopsis = entity.synopsis,
            genres = genres,
            themes = themes,
            meanScore = entity.meanScore,
            userScore = userAnime?.score?.takeIf { it > 0 },
            userStatus = userAnime?.status,
            isInUserList = userAnime != null,
            rank = entity.rank,
            popularity = entity.popularity,
            numListUsers = entity.numListUsers,
            numScoringUsers = null,
            mediaType = entity.mediaType,
            status = entity.airingStatus,
            season = formatSeason(entity.startSeasonYear, entity.startSeasonSeason),
            numEpisodes = entity.numEpisodes,
            durationSeconds = entity.averageEpisodeDuration,
            relatedAnime = emptyList(),
            recommendations = emptyList(),
        )
    }

    fun toDomain(
        dto: AnimeDetailsDto,
        userAnime: UserAnimeListEntity?,
    ): AnimeDetails {
        val englishTitle = dto.alternativeTitles?.en?.takeIf { it.isNotBlank() }
        val japaneseTitle = dto.alternativeTitles?.ja?.takeIf { it.isNotBlank() }
        val displayTitle = englishTitle ?: dto.title
        val subtitleTitle =
            when {
                !displayTitle.equals(dto.title, ignoreCase = true) -> dto.title
                !japaneseTitle.isNullOrBlank() &&
                    !japaneseTitle.equals(displayTitle, ignoreCase = true) -> japaneseTitle
                else -> null
            }

        val rawTags = dto.genres?.map { it.name } ?: emptyList()
        val (genres, themes) = rawTags.partitionGenresAndThemes { it }
        val userStatus = userAnime?.status ?: dto.myListStatus?.status
        val userScore =
            userAnime?.score?.takeIf { it > 0 }
                ?: dto.myListStatus?.score?.takeIf { it > 0 }

        return AnimeDetails(
            id = dto.id,
            displayTitle = displayTitle,
            subtitleTitle = subtitleTitle,
            originalTitle = japaneseTitle ?: dto.title,
            englishTitle = englishTitle,
            mainPictureMedium = dto.mainPicture?.medium,
            mainPictureLarge = dto.mainPicture?.large,
            synopsis = dto.synopsis,
            genres = genres,
            themes = themes,
            meanScore = dto.mean,
            userScore = userScore,
            userStatus = userStatus,
            isInUserList = userStatus != null,
            rank = dto.rank,
            popularity = dto.popularity,
            numListUsers = dto.numListUsers,
            numScoringUsers = dto.numScoringUsers,
            mediaType = dto.mediaType,
            status = dto.status,
            season = formatSeason(dto.startSeason?.year, dto.startSeason?.season),
            numEpisodes = dto.numEpisodes,
            durationSeconds = dto.averageEpisodeDuration,
            relatedAnime = dto.toRelatedAnimeList(),
            recommendations = dto.toDetailRecommendations(),
        )
    }

    private fun AnimeDetailsDto.toRelatedAnimeList(): List<RelatedAnime> =
        relatedAnime?.map { edge ->
            val edgeEnTitle =
                edge.node.alternativeTitles
                    ?.en
                    ?.takeIf { it.isNotBlank() }
            RelatedAnime(
                id = edge.node.id,
                title = edgeEnTitle ?: edge.node.title,
                thumbnailUrl = edge.node.mainPicture?.large ?: edge.node.mainPicture?.medium,
                relationType = edge.relationType,
                relationTypeFormatted = edge.relationTypeFormatted,
            )
        } ?: emptyList()

    private fun AnimeDetailsDto.toDetailRecommendations(): List<DetailRecommendation> =
        recommendations?.take(MAX_RECOMMENDATIONS)?.map { edge ->
            val edgeEnTitle =
                edge.node.alternativeTitles
                    ?.en
                    ?.takeIf { it.isNotBlank() }
            DetailRecommendation(
                id = edge.node.id,
                title = edgeEnTitle ?: edge.node.title,
                thumbnailUrl = edge.node.mainPicture?.large ?: edge.node.mainPicture?.medium,
                meanScore = edge.node.mean,
                numRecommendations = edge.numRecommendations,
            )
        } ?: emptyList()

    private fun formatSeason(
        year: Int?,
        season: String?,
    ): String? =
        if (year != null && season != null) {
            val capitalizedSeason =
                season.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
                }
            "$year $capitalizedSeason"
        } else if (year != null) {
            "$year"
        } else {
            null
        }
}
