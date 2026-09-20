package com.gokova.myanimelist.core.network.api

import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface MalApiService {
    @GET("v2/users/@me/animelist")
    suspend fun getUserAnimeList(
        @Query("fields") fields: String = DEFAULT_ANIME_LIST_FIELDS,
        @Query("limit") limit: Int = DEFAULT_PAGE_LIMIT,
        @Query("offset") offset: Int = 0,
        @Query("nsfw") nsfw: Boolean = true,
    ): AnimeListResponseDto

    @GET
    suspend fun getUserAnimeListNextPage(
        @Url url: String,
    ): AnimeListResponseDto

    @GET("v2/anime/ranking")
    suspend fun getAnimeRanking(
        @Query("ranking_type") rankingType: String = "all",
        @Query("limit") limit: Int = DEFAULT_PAGE_LIMIT,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = DEFAULT_ANIME_LIST_FIELDS,
        @Query("nsfw") nsfw: Boolean = true,
    ): AnimeListResponseDto

    @GET("v2/anime/season/{year}/{season}?sort=anime_num_list_users&nsfw=true")
    suspend fun getSeasonalAnime(
        @Path("year") year: Int,
        @Path("season") season: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = DEFAULT_ANIME_LIST_FIELDS,
    ): AnimeListResponseDto

    companion object {
        const val DEFAULT_PAGE_LIMIT = 500
        const val DEFAULT_ANIME_LIST_FIELDS =
            "id,title,main_picture,alternative_titles,media_type,status,num_episodes," +
                "start_season,mean,genres,studios,source,synopsis,rating,rank,popularity," +
                "num_list_users,average_episode_duration,nsfw,list_status"
    }
}
