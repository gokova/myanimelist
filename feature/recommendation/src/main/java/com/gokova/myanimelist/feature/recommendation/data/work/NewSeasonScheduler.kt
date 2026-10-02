package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface NewSeasonScheduler {
    fun schedulePeriodicWork()

    fun triggerImmediateCalculation()

    fun scheduleInitialCalculation()

    fun scheduleContinuation(
        isManual: Boolean,
        sessionId: String,
    )

    fun observeFetchWorkInfo(): Flow<List<WorkInfo>>

    suspend fun hasActiveOneTimeWork(): Boolean

    companion object {
        const val KEY_IS_MANUAL = "is_manual"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_START_NEW_SESSION = "start_new_session"
        const val PERIODIC_WORK_NAME = "periodic_new_season_fetch"
        const val FETCH_WORK_NAME = "one_time_new_season_fetch"
        const val PERIODIC_INTERVAL_DAYS = 30L
    }
}

@Singleton
class NewSeasonSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NewSeasonScheduler {
        private val workManager by lazy { WorkManager.getInstance(context) }

        override fun observeFetchWorkInfo(): Flow<List<WorkInfo>> =
            workManager.getWorkInfosForUniqueWorkFlow(NewSeasonScheduler.FETCH_WORK_NAME)

        override suspend fun hasActiveOneTimeWork(): Boolean =
            observeFetchWorkInfo().first().any { workInfo ->
                workInfo.state == WorkInfo.State.RUNNING ||
                    workInfo.state == WorkInfo.State.ENQUEUED
            }

        override fun schedulePeriodicWork() {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresDeviceIdle(true)
                    .build()

            val periodicRequest =
                PeriodicWorkRequestBuilder<FetchNewSeasonsWorker>(
                    NewSeasonScheduler.PERIODIC_INTERVAL_DAYS,
                    TimeUnit.DAYS,
                ).setConstraints(constraints)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                NewSeasonScheduler.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest,
            )
        }

        override fun triggerImmediateCalculation() {
            val oneTimeRequest = buildOneTimeRequest(isManual = true, newSession = true)

            workManager.enqueueUniqueWork(
                NewSeasonScheduler.FETCH_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest,
            )
        }

        override fun scheduleInitialCalculation() {
            val oneTimeRequest = buildOneTimeRequest(isManual = true, newSession = true)

            workManager.enqueueUniqueWork(
                NewSeasonScheduler.FETCH_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                oneTimeRequest,
            )
        }

        override fun scheduleContinuation(
            isManual: Boolean,
            sessionId: String,
        ) {
            val continuationRequest =
                buildOneTimeRequest(
                    isManual = isManual,
                    sessionId = sessionId,
                    newSession = false,
                )

            workManager.enqueueUniqueWork(
                NewSeasonScheduler.FETCH_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                continuationRequest,
            )
        }

        private fun buildOneTimeRequest(
            isManual: Boolean,
            sessionId: String = UUID.randomUUID().toString(),
            newSession: Boolean,
        ): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<FetchNewSeasonsWorker>()
                .setInputData(
                    Data
                        .Builder()
                        .putBoolean(NewSeasonScheduler.KEY_IS_MANUAL, isManual)
                        .putString(NewSeasonScheduler.KEY_SESSION_ID, sessionId)
                        .putBoolean(NewSeasonScheduler.KEY_START_NEW_SESSION, newSession)
                        .build(),
                ).setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                ).build()
    }
