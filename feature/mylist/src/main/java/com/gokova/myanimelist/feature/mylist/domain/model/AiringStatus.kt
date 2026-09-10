package com.gokova.myanimelist.feature.mylist.domain.model

enum class AiringStatus(
    val apiValue: String,
) {
    CURRENTLY_AIRING("currently_airing"),
    FINISHED_AIRING("finished_airing"),
    NOT_YET_AIRED("not_yet_aired"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromApiValue(value: String?): AiringStatus =
            entries.firstOrNull {
                it.apiValue.equals(value, ignoreCase = true)
            } ?: UNKNOWN
    }
}
