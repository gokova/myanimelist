package com.gokova.myanimelist.feature.recommendation.data.mapper

import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.GenreEntity
import com.gokova.myanimelist.core.database.entity.RecommendationEntity
import com.gokova.myanimelist.core.database.entity.StudioEntity
import com.gokova.myanimelist.core.database.model.RecommendationItem
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.feature.recommendation.domain.model.CandidateAnimeItem
import com.gokova.myanimelist.feature.recommendation.domain.model.RecommendedAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.ScoredCandidateResult

object RecommendationMapper {
    fun toAnimeEntity(node: AnimeNodeDto): AnimeEntity {
        val englishTitle = node.alternativeTitles?.en?.takeIf { it.isNotBlank() }
        val genres = node.genres?.map { GenreEntity(id = it.id, name = it.name) }
        val studios = node.studios?.map { StudioEntity(id = it.id, name = it.name) }
        return AnimeEntity(
            id = node.id,
            title = node.title,
            titleEnglish = englishTitle,
            mainPictureMedium = node.mainPicture?.medium,
            mainPictureLarge = node.mainPicture?.large,
            mediaType = node.mediaType,
            airingStatus = node.status,
            numEpisodes = node.numEpisodes,
            startSeasonYear = node.startSeason?.year,
            startSeasonSeason = node.startSeason?.season,
            meanScore = node.mean,
            genres = genres,
            studios = studios,
            source = node.source,
            synopsis = node.synopsis,
            rating = node.rating,
            rank = node.rank,
            popularity = node.popularity,
            numListUsers = node.numListUsers,
            averageEpisodeDuration = node.averageEpisodeDuration,
            nsfw = node.nsfw,
        )
    }

    fun toDomain(item: RecommendationItem): RecommendedAnime {
        val anime = item.anime
        val rec = item.recommendation
        return RecommendedAnime(
            animeId = anime.id,
            title = anime.title,
            titleEnglish = anime.titleEnglish,
            thumbnailUrl = anime.mainPictureMedium ?: anime.mainPictureLarge,
            largeImageUrl = anime.mainPictureLarge ?: anime.mainPictureMedium,
            mediaType = anime.mediaType,
            airingStatus = anime.airingStatus,
            numEpisodes = anime.numEpisodes,
            startSeasonYear = anime.startSeasonYear,
            startSeasonSeason = anime.startSeasonSeason,
            meanScore = anime.meanScore,
            genreRank = rec.genreRank,
            genreScore = rec.genreScore,
            genreMatchPercent = rec.genreMatchPercent,
            themeRank = rec.themeRank,
            themeScore = rec.themeScore,
            themeMatchPercent = rec.themeMatchPercent,
            genres = anime.genres?.map { it.name } ?: emptyList(),
            studios = anime.studios?.map { it.name } ?: emptyList(),
            source = anime.source,
        )
    }

    fun toCandidateAnimeItem(entity: AnimeEntity): CandidateAnimeItem =
        CandidateAnimeItem(
            animeId = entity.id,
            genres = entity.genres?.map { it.name } ?: emptyList(),
            meanScore = entity.meanScore,
            numListUsers = entity.numListUsers,
        )

    fun toRecommendationEntity(result: ScoredCandidateResult): RecommendationEntity =
        RecommendationEntity(
            animeId = result.animeId,
            genreScore = result.genreScore,
            genreRank = result.genreRank,
            themeScore = result.themeScore,
            themeRank = result.themeRank,
            genreMatchPercent = result.genreMatchPercent,
            themeMatchPercent = result.themeMatchPercent,
        )
}
