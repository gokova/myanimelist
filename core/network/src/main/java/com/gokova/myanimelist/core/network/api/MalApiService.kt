package com.gokova.myanimelist.core.network.api

import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import retrofit2.http.GET
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

    companion object {
        const val DEFAULT_PAGE_LIMIT = 500
        const val DEFAULT_ANIME_LIST_FIELDS =
            "id,title,main_picture,alternative_titles,media_type,status,num_episodes," +
                "start_season,mean,list_status"
    }
}
