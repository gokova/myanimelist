package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface NewSeasonScheduler {
    fun schedulePeriodicWork()

    fun triggerImmediateCalculation()

    fun scheduleInitialCalculation()

    fun observeFetchWorkInfo(): Flow<List<WorkInfo>>

    companion object {
        const val KEY_IS_MANUAL = "is_manual"
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
            val inputData =
                Data
                    .Builder()
                    .putBoolean(NewSeasonScheduler.KEY_IS_MANUAL, true)
                    .build()

            val oneTimeRequest =
                OneTimeWorkRequestBuilder<FetchNewSeasonsWorker>()
                    .setInputData(inputData)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS,
                    ).build()

            workManager.enqueueUniqueWork(
                NewSeasonScheduler.FETCH_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest,
            )
        }

        override fun scheduleInitialCalculation() {
            val inputData =
                Data
                    .Builder()
                    .putBoolean(NewSeasonScheduler.KEY_IS_MANUAL, true)
                    .build()

            val oneTimeRequest =
                OneTimeWorkRequestBuilder<FetchNewSeasonsWorker>()
                    .setInputData(inputData)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS,
                    ).build()

            workManager.enqueueUniqueWork(
                NewSeasonScheduler.FETCH_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                oneTimeRequest,
            )
        }
    }
