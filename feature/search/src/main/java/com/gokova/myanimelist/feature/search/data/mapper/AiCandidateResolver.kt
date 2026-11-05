package com.gokova.myanimelist.feature.search.data.mapper

import com.gokova.myanimelist.core.ai.model.AiCandidateHint
import com.gokova.myanimelist.core.database.model.UserAnimeListItem
import com.gokova.myanimelist.core.network.model.AnimeListEntryDto
import com.gokova.myanimelist.feature.search.domain.algorithm.SearchTasteMatcher
import com.gokova.myanimelist.feature.search.domain.model.SearchAnimeItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiCandidateResolver
    @Inject
    constructor(
        private val tasteMatcher: SearchTasteMatcher,
    ) {
        fun resolveCandidate(
            candidate: AiCandidateHint,
            entries: List<AnimeListEntryDto>,
            userList: List<UserAnimeListItem>,
            ignoredIds: Set<Long>,
            alreadyResolvedIds: Set<Long>,
        ): SearchAnimeItem? {
            val validEntries =
                entries.filter {
                    it.node.id !in ignoredIds && it.node.id !in alreadyResolvedIds
                }
            val bestEntry = findBestMatch(candidate, validEntries) ?: return null
            val userStatusMap = userList.associate { it.userAnime.animeId to it.userAnime.status }
            val localStatus = userStatusMap[bestEntry.node.id]
            val candidateGenres = bestEntry.node.genres?.map { it.name } ?: emptyList()
            val matchPercent =
                tasteMatcher.calculateMatch(
                    candidateGenres = candidateGenres,
                    candidateMeanScore = bestEntry.node.mean,
                    candidateNumListUsers = bestEntry.node.numListUsers,
                    userList = userList,
                )

            return SearchMapper
                .toDomain(bestEntry, localStatus, matchPercent)
                .copy(aiReason = candidate.reason)
        }

        fun calculateTasteMatch(
            genres: List<String>,
            meanScore: Double?,
            numListUsers: Int?,
            userList: List<UserAnimeListItem>,
        ): Int? =
            tasteMatcher.calculateMatch(
                candidateGenres = genres,
                candidateMeanScore = meanScore,
                candidateNumListUsers = numListUsers,
                userList = userList,
            )

        private fun findBestMatch(
            candidate: AiCandidateHint,
            entries: List<AnimeListEntryDto>,
        ): AnimeListEntryDto? {
            val candidateTitle = candidate.title.trim()
            val exactTitle =
                entries.firstOrNull {
                    it.node.title.equals(candidateTitle, ignoreCase = true) ||
                        it.node.alternativeTitles
                            ?.en
                            .equals(candidateTitle, ignoreCase = true)
                }
            val yearMatch =
                candidate.expectedYear?.let { year ->
                    entries.firstOrNull { it.node.startSeason?.year == year }
                }
            return exactTitle ?: yearMatch
        }
    }
