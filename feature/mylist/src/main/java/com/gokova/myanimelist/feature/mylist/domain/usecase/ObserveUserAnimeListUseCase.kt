package com.gokova.myanimelist.feature.mylist.domain.usecase

import com.gokova.myanimelist.feature.mylist.domain.model.ListFilterCategory
import com.gokova.myanimelist.feature.mylist.domain.model.SortOption
import com.gokova.myanimelist.feature.mylist.domain.model.UserAnime
import com.gokova.myanimelist.feature.mylist.domain.repository.MyListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

private val ISO_PATTERNS =
    arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
    )

class ObserveUserAnimeListUseCase
    @Inject
    constructor(
        private val repository: MyListRepository,
    ) {
        operator fun invoke(
            category: ListFilterCategory,
            sort: SortOption,
        ): Flow<List<UserAnime>> =
            repository.observeUserAnimeList(category).map { list ->
                sortAnimeList(list, sort)
            }

        private fun sortAnimeList(
            list: List<UserAnime>,
            sort: SortOption,
        ): List<UserAnime> =
            when (sort) {
                SortOption.SCORE_DESC ->
                    list.sortedWith(
                        compareByDescending<UserAnime> { it.userScore }
                            .thenBy { it.displayTitle.lowercase() },
                    )
                SortOption.TITLE_ASC ->
                    list.sortedBy { it.displayTitle.lowercase() }
                SortOption.UPDATED_AT_DESC ->
                    list.sortedWith(
                        compareByDescending<UserAnime> { parseTimestamp(it.updatedAt) }
                            .thenBy { it.displayTitle.lowercase() },
                    )
            }

        private fun parseTimestamp(isoString: String?): Long {
            if (isoString.isNullOrBlank()) return 0L
            return ISO_PATTERNS.firstNotNullOfOrNull { pattern ->
                try {
                    val sdf =
                        SimpleDateFormat(pattern, Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                    sdf.parse(isoString)?.time
                } catch (_: ParseException) {
                    null
                }
            } ?: 0L
        }
    }
