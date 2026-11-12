package com.gokova.myanimelist.feature.franchise.data.repository

import com.gokova.myanimelist.core.database.dao.AnimeRelationshipDao
import com.gokova.myanimelist.core.database.dao.IgnoredAnimeDao
import com.gokova.myanimelist.core.database.dao.UserAnimeListDao
import com.gokova.myanimelist.core.database.entity.AnimeRelationshipEntity
import com.gokova.myanimelist.core.database.entity.UserAnimeListEntity
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.di.IoDispatcher
import com.gokova.myanimelist.feature.franchise.domain.model.FranchiseGraph
import com.gokova.myanimelist.feature.franchise.domain.repository.FranchiseRepository
import com.gokova.myanimelist.feature.franchise.domain.usecase.GetFranchiseGraphUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_NON_SEQUEL_HOPS = 2
private val ALLOWED_RELATION_TYPES =
    setOf(
        "sequel",
        "prequel",
        "side_story",
        "spin_off",
        "alternative_version",
        "alternative_setting",
        "summary",
        "full_story",
        "parent_story",
        "other",
    )

@Singleton
@Suppress("LongParameterList", "TooManyFunctions")
class FranchiseRepositoryImpl
    @Inject
    constructor(
        private val animeRelationshipDao: AnimeRelationshipDao,
        private val userAnimeListDao: UserAnimeListDao,
        private val ignoredAnimeDao: IgnoredAnimeDao,
        private val malApiService: MalApiService,
        private val getFranchiseGraphUseCase: GetFranchiseGraphUseCase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : FranchiseRepository {
        override fun observeFranchiseGraph(rootAnimeId: Long): Flow<FranchiseGraph> =
            flow {
                val connectedIds = resolveConnectedComponentIds(rootAnimeId)
                val allRelationships =
                    animeRelationshipDao.getRelationshipsForAnimeIds(connectedIds)

                val graphFlow =
                    combine(
                        animeRelationshipDao.observeAnimeEntitiesByIds(connectedIds),
                        animeRelationshipDao.observeUserAnimeByIds(connectedIds),
                        ignoredAnimeDao.observeIgnoredAnimeIds(),
                    ) { animes, userAnimes, ignoredIds ->
                        getFranchiseGraphUseCase.buildGraph(
                            rootAnimeId = rootAnimeId,
                            relationships = allRelationships,
                            animeEntities = animes,
                            userAnimeItems = userAnimes,
                            ignoredAnimeIds = ignoredIds.toSet(),
                        )
                    }

                graphFlow.collect { graph -> emit(graph) }
            }.flowOn(ioDispatcher)

        private suspend fun resolveConnectedComponentIds(rootAnimeId: Long): Set<Long> {
            val visited = mutableSetOf<Long>()
            val queue = ArrayDeque<Pair<Long, Int>>()
            queue.add(rootAnimeId to 0)
            visited.add(rootAnimeId)

            while (queue.isNotEmpty()) {
                val (currId, nonSequelHops) = queue.removeFirst()
                val edges = animeRelationshipDao.getRelationshipsForAnimeIds(setOf(currId))
                exploreEdges(currId, nonSequelHops, edges, visited, queue)
            }
            return visited
        }

        private fun exploreEdges(
            currId: Long,
            nonSequelHops: Int,
            edges: List<AnimeRelationshipEntity>,
            visited: MutableSet<Long>,
            queue: ArrayDeque<Pair<Long, Int>>,
        ) {
            for (edge in edges) {
                val relType = edge.relationType.lowercase()
                val isValidRelation = relType != "character" && relType in ALLOWED_RELATION_TYPES
                val isSequelPrequel = relType == "sequel" || relType == "prequel"
                val nextHops = if (isSequelPrequel) nonSequelHops else nonSequelHops + 1

                if (isValidRelation && nextHops <= MAX_NON_SEQUEL_HOPS) {
                    val neighborId =
                        if (edge.sourceAnimeId == currId) edge.targetAnimeId else edge.sourceAnimeId
                    if (visited.add(neighborId)) {
                        queue.add(neighborId to nextHops)
                    }
                }
            }
        }

        override suspend fun incrementWatchedEpisode(animeId: Long): Result<Unit> =
            withContext(ioDispatcher) {
                try {
                    val userAnimeItem = userAnimeListDao.getUserAnimeById(animeId)
                    val currentEpisodes = userAnimeItem?.userAnime?.numEpisodesWatched ?: 0
                    val nextEpisodes = currentEpisodes + 1

                    val statusDto =
                        malApiService.updateMyListStatus(
                            animeId = animeId,
                            numWatchedEpisodes = nextEpisodes,
                        )

                    val updatedEntity =
                        UserAnimeListEntity(
                            animeId = animeId,
                            status =
                                statusDto.status ?: userAnimeItem?.userAnime?.status ?: "watching",
                            score = statusDto.score ?: userAnimeItem?.userAnime?.score ?: 0,
                            numEpisodesWatched = statusDto.numEpisodesWatched ?: nextEpisodes,
                            updatedAt = statusDto.updatedAt ?: "",
                            isRewatching = statusDto.isRewatching ?: false,
                        )
                    userAnimeListDao.upsertUserAnimeList(listOf(updatedEntity))
                    Result.success(Unit)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.domain.e(e) { "Failed to increment watched episode for anime $animeId" }
                    Result.failure(e)
                }
            }

        override suspend fun unmarkNotInterested(animeId: Long): Result<Unit> =
            withContext(ioDispatcher) {
                try {
                    ignoredAnimeDao.deleteIgnoredAnime(animeId)
                    Result.success(Unit)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    AppLog.domain.e(e) { "Failed to unmark not interested for anime $animeId" }
                    Result.failure(e)
                }
            }
    }
