package com.gokova.myanimelist.feature.taste.domain.usecase

import com.gokova.myanimelist.core.domain.taxonomy.TagType
import com.gokova.myanimelist.core.domain.taxonomy.classifyTag
import com.gokova.myanimelist.feature.taste.domain.algorithm.CirclePacking
import com.gokova.myanimelist.feature.taste.domain.algorithm.PackableCircle
import com.gokova.myanimelist.feature.taste.domain.model.TasteAnalytics
import com.gokova.myanimelist.feature.taste.domain.model.TasteAnimeItem
import com.gokova.myanimelist.feature.taste.domain.model.TasteBubble
import com.gokova.myanimelist.feature.taste.domain.model.UserAnimeRecord
import com.gokova.myanimelist.feature.taste.domain.repository.TasteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.sqrt

class ObserveTasteAnalyticsUseCase
    @Inject
    constructor(
        private val repository: TasteRepository,
    ) {
        operator fun invoke(): Flow<TasteAnalytics> =
            repository.observeUserAnimeRecords().map { records ->
                processTasteAnalytics(records)
            }

        private fun processTasteAnalytics(records: List<UserAnimeRecord>): TasteAnalytics {
            if (records.isEmpty()) {
                return TasteAnalytics(
                    genres = emptyList(),
                    themes = emptyList(),
                    totalAnimeCount = 0,
                )
            }

            val genreMap = mutableMapOf<String, MutableList<UserAnimeRecord>>()
            val themeMap = mutableMapOf<String, MutableList<UserAnimeRecord>>()

            for (record in records) {
                classifyRecordTags(record, genreMap, themeMap)
            }

            return TasteAnalytics(
                genres = buildBubbles(genreMap),
                themes = buildBubbles(themeMap),
                totalAnimeCount = records.size,
            )
        }

        private fun classifyRecordTags(
            record: UserAnimeRecord,
            genreMap: MutableMap<String, MutableList<UserAnimeRecord>>,
            themeMap: MutableMap<String, MutableList<UserAnimeRecord>>,
        ) {
            val seenGenres = mutableSetOf<String>()
            val seenThemes = mutableSetOf<String>()

            for (tag in record.genres) {
                when (classifyTag(tag)) {
                    TagType.GENRE -> {
                        if (seenGenres.add(tag)) {
                            genreMap.getOrPut(tag) { mutableListOf() }.add(record)
                        }
                    }
                    TagType.THEME -> {
                        if (seenThemes.add(tag)) {
                            themeMap.getOrPut(tag) { mutableListOf() }.add(record)
                        }
                    }
                }
            }
        }

        private fun buildBubbles(grouped: Map<String, List<UserAnimeRecord>>): List<TasteBubble> {
            val eligible =
                grouped
                    .filter { it.value.size >= MIN_OCCURRENCE_THRESHOLD }
                    .toList()
                    .sortedByDescending { it.second.size }

            if (eligible.isEmpty()) return emptyList()

            val minCount = eligible.minOf { it.second.size }
            val maxCount = eligible.maxOf { it.second.size }
            val minSqrt = sqrt(minCount.toDouble()).toFloat()
            val maxSqrt = sqrt(maxCount.toDouble()).toFloat()
            val sqrtRange = maxSqrt - minSqrt

            val packables =
                eligible.mapIndexed { index, (name, animeList) ->
                    createPackableCircle(name, animeList, index, minSqrt, sqrtRange)
                }

            val packed = CirclePacking.pack(packables, spacing = BUBBLE_SPACING_DP)

            return packed.map { positioned ->
                val item = positioned.item
                TasteBubble(
                    name = item.name,
                    count = item.count,
                    radius = positioned.radius,
                    x = positioned.x,
                    y = positioned.y,
                    colorIndex = item.colorIndex,
                    averageScore = item.averageScore,
                    matchingAnime = item.matchingAnime,
                )
            }
        }

        private fun createPackableCircle(
            name: String,
            animeList: List<UserAnimeRecord>,
            index: Int,
            minSqrt: Float,
            sqrtRange: Float,
        ): PackableCircle<IntermediateBubble> {
            val count = animeList.size
            val radius =
                if (sqrtRange > 0.001f) {
                    val countSqrt = sqrt(count.toDouble()).toFloat()
                    val ratio = (countSqrt - minSqrt) / sqrtRange
                    MIN_RADIUS_DP + ratio * (MAX_RADIUS_DP - MIN_RADIUS_DP)
                } else {
                    (MIN_RADIUS_DP + MAX_RADIUS_DP) / 2f
                }

            val ratedAnime = animeList.filter { it.userScore > 0 }
            val avgScore =
                if (ratedAnime.isNotEmpty()) {
                    ratedAnime.map { it.userScore }.average()
                } else {
                    null
                }

            return PackableCircle(
                item =
                    IntermediateBubble(
                        name = name,
                        count = count,
                        colorIndex = index % PALETTE_SIZE,
                        averageScore = avgScore,
                        matchingAnime = animeList.map { it.toTasteAnimeItem() },
                    ),
                radius = radius,
            )
        }

        private fun UserAnimeRecord.toTasteAnimeItem(): TasteAnimeItem =
            TasteAnimeItem(
                id = animeId,
                title = title,
                imageUrl = imageUrl,
                userScore = userScore,
                userStatus = status,
                totalEpisodes = totalEpisodes,
                watchedEpisodes = numEpisodesWatched,
            )

        private data class IntermediateBubble(
            val name: String,
            val count: Int,
            val colorIndex: Int,
            val averageScore: Double?,
            val matchingAnime: List<TasteAnimeItem>,
        )

        companion object {
            const val MIN_OCCURRENCE_THRESHOLD = 3
            const val MIN_RADIUS_DP = 36f
            const val MAX_RADIUS_DP = 88f
            const val BUBBLE_SPACING_DP = 4f
            const val PALETTE_SIZE = 15
        }
    }
