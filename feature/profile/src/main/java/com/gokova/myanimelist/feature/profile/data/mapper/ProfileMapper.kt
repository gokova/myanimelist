package com.gokova.myanimelist.feature.profile.data.mapper

import com.gokova.myanimelist.core.domain.model.UserAnimeStatistics
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.network.model.UserAnimeStatisticsDto
import com.gokova.myanimelist.core.network.model.UserDto

fun UserDto.toDomain(): UserProfile =
    UserProfile(
        id = id,
        name = name,
        pictureUrl = picture,
        gender = gender,
        birthday = birthday,
        location = location,
        joinedAt = joinedAt,
        statistics = animeStatistics?.toDomain(),
    )

fun UserAnimeStatisticsDto.toDomain(): UserAnimeStatistics =
    UserAnimeStatistics(
        numItemsWatching = numItemsWatching,
        numItemsCompleted = numItemsCompleted,
        numItemsOnHold = numItemsOnHold,
        numItemsDropped = numItemsDropped,
        numItemsPlanToWatch = numItemsPlanToWatch,
        numItems = numItems,
        numDaysWatched = numDaysWatched,
        numDaysWatching = numDaysWatching,
        numDaysCompleted = numDaysCompleted,
        numDaysOnHold = numDaysOnHold,
        numDaysDropped = numDaysDropped,
        numDays = numDays,
        numEpisodes = numEpisodes,
        numTimesRewatched = numTimesRewatched,
        meanScore = meanScore,
    )
