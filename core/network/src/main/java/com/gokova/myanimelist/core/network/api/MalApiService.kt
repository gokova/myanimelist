package com.gokova.myanimelist.core.network.api

import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.core.network.model.UserDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PUT
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
    suspend fun getAnimeListNextPage(
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

    @GET("v2/anime/{anime_id}")
    suspend fun getAnimeDetails(
        @Path("anime_id") animeId: Long,
        @Query("fields") fields: String = DEFAULT_ANIME_DETAILS_FIELDS,
    ): AnimeDetailsDto

    @FormUrlEncoded
    @PUT("v2/anime/{anime_id}/my_list_status")
    suspend fun updateMyListStatus(
        @Path("anime_id") animeId: Long,
        @Field("status") status: String = "plan_to_watch",
        @Field("num_watched_episodes") numWatchedEpisodes: Int = 0,
        @Field("score") score: Int = 0,
    ): MyListStatusDto

    @GET("v2/users/{user_id}")
    suspend fun getUserProfile(
        @Path("user_id") userId: String = USER_ID_ME,
        @Query("fields") fields: String = DEFAULT_USER_FIELDS,
    ): UserDto

    companion object {
        const val USER_ID_ME = "@me"
        const val DEFAULT_USER_FIELDS =
            "id,name,picture,gender,birthday,location,joined_at,anime_statistics," +
                "time_zone,is_supporter"
        const val DEFAULT_PAGE_LIMIT = 500
        const val DEFAULT_ANIME_LIST_FIELDS =
            "id,title,main_picture,alternative_titles,media_type,status,num_episodes," +
                "start_season,mean,genres,studios,source,synopsis,rating,rank,popularity," +
                "num_list_users,average_episode_duration,nsfw,list_status"
        const val DEFAULT_ANIME_DETAILS_FIELDS =
            "id,title,main_picture,alternative_titles,media_type,status,num_episodes," +
                "start_season,mean,genres,studios,source,synopsis,rating,rank,popularity," +
                "num_list_users,num_scoring_users,average_episode_duration,nsfw," +
                "related_anime,recommendations,my_list_status"
        const val FIELDS_RELATED_ANIME = "related_anime"
    }
}
