package com.gokova.myanimelist.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class AnimeListResponseDto(
    @SerialName("data")
    val data: List<AnimeListEntryDto> = emptyList(),
    @SerialName("paging")
    val paging: PagingDto? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class AnimeListEntryDto(
    @SerialName("node")
    val node: AnimeNodeDto,
    @SerialName("list_status")
    val listStatus: MyListStatusDto? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class AnimeNodeDto(
    @SerialName("id")
    val id: Long,
    @SerialName("title")
    val title: String,
    @SerialName("main_picture")
    val mainPicture: PictureDto? = null,
    @SerialName("alternative_titles")
    val alternativeTitles: AlternativeTitlesDto? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("num_episodes")
    val numEpisodes: Int? = null,
    @SerialName("start_season")
    val startSeason: StartSeasonDto? = null,
    @SerialName("mean")
    val mean: Double? = null,
    @SerialName("genres")
    val genres: List<GenreDto>? = null,
    @SerialName("studios")
    val studios: List<StudioDto>? = null,
    @SerialName("source")
    val source: String? = null,
    @SerialName("synopsis")
    val synopsis: String? = null,
    @SerialName("rating")
    val rating: String? = null,
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("popularity")
    val popularity: Int? = null,
    @SerialName("num_list_users")
    val numListUsers: Int? = null,
    @SerialName("average_episode_duration")
    val averageEpisodeDuration: Int? = null,
    @SerialName("nsfw")
    val nsfw: String? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class GenreDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class StudioDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class AlternativeTitlesDto(
    @SerialName("synonyms")
    val synonyms: List<String>? = null,
    @SerialName("en")
    val en: String? = null,
    @SerialName("ja")
    val ja: String? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class PictureDto(
    @SerialName("medium")
    val medium: String? = null,
    @SerialName("large")
    val large: String? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class StartSeasonDto(
    @SerialName("year")
    val year: Int? = null,
    @SerialName("season")
    val season: String? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class MyListStatusDto(
    @SerialName("status")
    val status: String? = null,
    @SerialName("score")
    val score: Int? = null,
    @SerialName("num_episodes_watched")
    val numEpisodesWatched: Int? = null,
    @SerialName("is_rewatching")
    val isRewatching: Boolean? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class PagingDto(
    @SerialName("next")
    val next: String? = null,
    @SerialName("previous")
    val previous: String? = null,
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class AnimeDetailsDto(
    @SerialName("id")
    val id: Long,
    @SerialName("title")
    val title: String,
    @SerialName("main_picture")
    val mainPicture: PictureDto? = null,
    @SerialName("alternative_titles")
    val alternativeTitles: AlternativeTitlesDto? = null,
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("num_episodes")
    val numEpisodes: Int? = null,
    @SerialName("start_season")
    val startSeason: StartSeasonDto? = null,
    @SerialName("mean")
    val mean: Double? = null,
    @SerialName("genres")
    val genres: List<GenreDto>? = null,
    @SerialName("studios")
    val studios: List<StudioDto>? = null,
    @SerialName("source")
    val source: String? = null,
    @SerialName("synopsis")
    val synopsis: String? = null,
    @SerialName("rating")
    val rating: String? = null,
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("popularity")
    val popularity: Int? = null,
    @SerialName("num_list_users")
    val numListUsers: Int? = null,
    @SerialName("average_episode_duration")
    val averageEpisodeDuration: Int? = null,
    @SerialName("nsfw")
    val nsfw: String? = null,
    @SerialName("related_anime")
    val relatedAnime: List<RelatedAnimeEdgeDto>? = null,
) {
    fun toAnimeNodeDto(): AnimeNodeDto =
        AnimeNodeDto(
            id = id,
            title = title,
            mainPicture = mainPicture,
            alternativeTitles = alternativeTitles,
            mediaType = mediaType,
            status = status,
            numEpisodes = numEpisodes,
            startSeason = startSeason,
            mean = mean,
            genres = genres,
            studios = studios,
            source = source,
            synopsis = synopsis,
            rating = rating,
            rank = rank,
            popularity = popularity,
            numListUsers = numListUsers,
            averageEpisodeDuration = averageEpisodeDuration,
            nsfw = nsfw,
        )
}

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Serializable
data class RelatedAnimeEdgeDto(
    @SerialName("node")
    val node: AnimeNodeDto,
    @SerialName("relation_type")
    val relationType: String,
    @SerialName("relation_type_formatted")
    val relationTypeFormatted: String,
)
