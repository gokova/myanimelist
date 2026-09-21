package com.gokova.myanimelist.feature.recommendation.data.repository

import androidx.work.WorkInfo
import com.gokova.myanimelist.core.database.dao.NewSeasonDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.feature.recommendation.data.mapper.NewSeasonMapper
import com.gokova.myanimelist.feature.recommendation.data.work.NewSeasonScheduler
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonAnime
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonSortOption
import com.gokova.myanimelist.feature.recommendation.domain.model.NewSeasonState
import com.gokova.myanimelist.feature.recommendation.domain.repository.NewSeasonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject

class NewSeasonRepositoryImpl
    @Inject
    constructor(
        private val newSeasonDao: NewSeasonDao,
        private val userAnimeListDao: UserAnimeListDao,
        private val scheduler: NewSeasonScheduler,
    ) : NewSeasonRepository {
        override fun observeNewSeasons(
            sortOption: NewSeasonSortOption,
        ): Flow<List<NewSeasonAnime>> =
            newSeasonDao.observeNewSeasons().map { list ->
                val domainItems = list.map { NewSeasonMapper.toDomain(it) }
                sortNewSeasons(domainItems, sortOption)
            }

        override fun observeNewSeasonState(): Flow<NewSeasonState> =
            combine(
                userAnimeListDao.observeAllUserAnime(),
                newSeasonDao.observeNewSeasonCount(),
                scheduler.observeFetchWorkInfo(),
            ) { userList, newSeasonCount, workInfos ->
                val activeOrFinished =
                    workInfos.filter { it.state != WorkInfo.State.CANCELLED }
                val isWorking =
                    activeOrFinished.any {
                        it.state == WorkInfo.State.RUNNING ||
                            it.state == WorkInfo.State.ENQUEUED
                    }
                val currentWork = activeOrFinished.lastOrNull()

                when {
                    userList.size < MIN_USER_LIST_THRESHOLD ->
                        NewSeasonState.EmptyInsufficientData
                    newSeasonCount > 0 ->
                        NewSeasonState.Ready(newSeasonCount)
                    isWorking ->
                        NewSeasonState.Calculating
                    currentWork?.state == WorkInfo.State.FAILED ->
                        NewSeasonState.Error("Failed to discover new seasons")
                    currentWork?.state == WorkInfo.State.SUCCEEDED ->
                        NewSeasonState.EmptyAllCaughtUp
                    else ->
                        NewSeasonState.Calculating
                }
            }

        override suspend fun scheduleInitialOrPeriodicWork() {
            scheduler.schedulePeriodicWork()
            val currentCount = newSeasonDao.getNewSeasonCount()
            val userAnimeCount = userAnimeListDao.getAllUserAnime().size
            if (currentCount == 0 && userAnimeCount >= MIN_USER_LIST_THRESHOLD) {
                scheduler.scheduleInitialCalculation()
            }
        }

        override suspend fun triggerImmediateEvaluation() {
            scheduler.triggerImmediateCalculation()
        }

        private fun sortNewSeasons(
            list: List<NewSeasonAnime>,
            sortOption: NewSeasonSortOption,
        ): List<NewSeasonAnime> =
            when (sortOption) {
                NewSeasonSortOption.RELEASE_DATE_DESC ->
                    list.sortedWith(
                        compareByDescending<NewSeasonAnime> { it.startSeasonYear ?: 0 }
                            .thenByDescending { seasonToOrder(it.startSeasonSeason) }
                            .thenByDescending { it.animeId },
                    )
                NewSeasonSortOption.SCORE_DESC ->
                    list.sortedWith(
                        compareByDescending<NewSeasonAnime> { it.score ?: 0.0 }
                            .thenByDescending { it.animeId },
                    )
                NewSeasonSortOption.TITLE_ASC ->
                    list.sortedWith(
                        compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle },
                    )
            }

        private fun seasonToOrder(season: String?): Int =
            when (season?.lowercase(Locale.US)) {
                "winter" -> SEASON_ORDER_WINTER
                "spring" -> SEASON_ORDER_SPRING
                "summer" -> SEASON_ORDER_SUMMER
                "fall" -> SEASON_ORDER_FALL
                else -> SEASON_ORDER_UNKNOWN
            }

        companion object {
            private const val MIN_USER_LIST_THRESHOLD = 5
            private const val SEASON_ORDER_UNKNOWN = 0
            private const val SEASON_ORDER_WINTER = 1
            private const val SEASON_ORDER_SPRING = 2
            private const val SEASON_ORDER_SUMMER = 3
            private const val SEASON_ORDER_FALL = 4
        }
    }
