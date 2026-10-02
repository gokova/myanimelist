package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import android.content.ContextWrapper
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.gokova.myanimelist.core.database.dao.RecommendationDao
import com.gokova.myanimelist.core.database.entity.AnimeEntity
import com.gokova.myanimelist.core.database.entity.RecommendationCandidateEntity
import com.gokova.myanimelist.core.database.entity.RecommendationEntity
import com.gokova.myanimelist.core.database.model.RecommendationItem
import com.gokova.myanimelist.core.network.model.AnimeNodeDto
import com.gokova.myanimelist.feature.recommendation.data.remote.RecommendationRemoteDataSource
import com.gokova.myanimelist.feature.recommendation.data.remote.SeasonalPeriodCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class FetchCandidatesWorkerTest {
    @Test
    fun `doWork waits while New Seasons one-time work is active`() =
        runTest {
            val remoteDataSource = FakeRemoteDataSource(createHttpException(429))

            val result = createWorker(remoteDataSource, newSeasonWorkActive = true).doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        }

    @Test
    fun `doWork retries after transient HTTP failure exhausts remote retry budget`() =
        runTest {
            val remoteDataSource = FakeRemoteDataSource(createHttpException(429))

            val result = createWorker(remoteDataSource).doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        }

    @Test
    fun `doWork fails for non-retryable HTTP failure`() =
        runTest {
            val remoteDataSource = FakeRemoteDataSource(createHttpException(404))

            val result = createWorker(remoteDataSource).doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }

    private fun createWorker(
        remoteDataSource: RecommendationRemoteDataSource,
        newSeasonWorkActive: Boolean = false,
    ): FetchCandidatesWorker {
        val context =
            object : ContextWrapper(null) {
                override fun getApplicationContext(): Context = this
            }
        val inputData =
            Data
                .Builder()
                .putBoolean(RecommendationScheduler.KEY_IS_MANUAL, true)
                .build()
        val recommendationDao = FakeRecommendationDao()
        return TestListenableWorkerBuilder<FetchCandidatesWorker>(context)
            .setInputData(inputData)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker =
                        FetchCandidatesWorker(
                            appContext,
                            workerParameters,
                            remoteDataSource,
                            recommendationDao,
                            SeasonalPeriodCalculator(),
                            FakeNewSeasonScheduler(newSeasonWorkActive),
                        )
                },
            ).build()
    }

    private class FakeRemoteDataSource(
        private val failure: HttpException,
    ) : RecommendationRemoteDataSource {
        override suspend fun fetchTopRankingAnime(limit: Int): List<AnimeNodeDto> = throw failure

        override suspend fun fetchSeasonalAnime(
            year: Int,
            season: String,
            limit: Int,
        ): List<AnimeNodeDto> = emptyList()
    }

    private class FakeNewSeasonScheduler(
        private val workActive: Boolean = false,
    ) : NewSeasonScheduler {
        override fun schedulePeriodicWork() = Unit

        override fun triggerImmediateCalculation() = Unit

        override fun scheduleInitialCalculation() = Unit

        override fun observeFetchWorkInfo(): Flow<List<androidx.work.WorkInfo>> = emptyFlow()

        override suspend fun hasActiveOneTimeWork(): Boolean = workActive

        override fun scheduleContinuation(
            isManual: Boolean,
            sessionId: String,
        ) = Unit
    }

    private class FakeRecommendationDao : RecommendationDao {
        override fun observeRecommendationsByGenre(limit: Int): Flow<List<RecommendationItem>> =
            emptyFlow()

        override fun observeRecommendationsByTheme(limit: Int): Flow<List<RecommendationItem>> =
            emptyFlow()

        override fun observeRecommendationCount(): Flow<Int> = emptyFlow()

        override suspend fun getRecommendationCount(): Int = 0

        override suspend fun upsertAnimes(animes: List<AnimeEntity>) = Unit

        override suspend fun upsertCandidates(candidates: List<RecommendationCandidateEntity>) {
            // no-op
        }

        override suspend fun getUserAnimeIds(): List<Long> = (1L..5L).toList()

        override suspend fun getExcludedCandidateAnimeIds(): List<Long> = emptyList()

        override suspend fun getCandidateAnimes(): List<AnimeEntity> = emptyList()

        override suspend fun upsertRecommendations(recommendations: List<RecommendationEntity>) {
            // no-op
        }

        override suspend fun clearRecommendations(): Int = 0

        override suspend fun clearCandidates(): Int = 0

        override suspend fun deleteRecommendation(animeId: Long): Int = 0

        override suspend fun deleteNonUserData(animeIds: List<Long>): Int = 0
    }

    private fun createHttpException(code: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }
}
