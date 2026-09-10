package com.gokova.myanimelist.feature.mylist.domain.model

enum class UserAnimeStatus(
    val apiValue: String,
) {
    WATCHING("watching"),
    COMPLETED("completed"),
    ON_HOLD("on_hold"),
    DROPPED("dropped"),
    PLAN_TO_WATCH("plan_to_watch"),
    ;

    companion object {
        fun fromApiValue(value: String?): UserAnimeStatus =
            entries.firstOrNull {
                it.apiValue.equals(value, ignoreCase = true)
            } ?: PLAN_TO_WATCH
    }
}
