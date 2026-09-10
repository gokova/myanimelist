package com.gokova.myanimelist.feature.mylist.domain.model

enum class ListFilterCategory(
    val status: UserAnimeStatus?,
) {
    ALL(null),
    WATCHING(UserAnimeStatus.WATCHING),
    COMPLETED(UserAnimeStatus.COMPLETED),
    ON_HOLD(UserAnimeStatus.ON_HOLD),
    DROPPED(UserAnimeStatus.DROPPED),
    PLAN_TO_WATCH(UserAnimeStatus.PLAN_TO_WATCH),
}
