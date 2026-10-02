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
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface RecommendationScheduler {
    fun schedulePeriodicWork()

    fun triggerImmediateCalculation()

    fun scheduleInitialCalculation()

    fun enqueueFetchAndEvaluation(
        isManual: Boolean,
        replaceExisting: Boolean,
    )

    companion object {
        const val KEY_IS_MANUAL = "is_manual"
        const val PERIODIC_WORK_NAME = "periodic_recommendation_fetch"
        const val FETCH_WORK_NAME = "one_time_recommendation_fetch"
        const val PERIODIC_INTERVAL_DAYS = 90L
    }
}

internal fun buildRecommendationFetchRequest(isManual: Boolean): OneTimeWorkRequest =
    OneTimeWorkRequestBuilder<FetchCandidatesWorker>()
        .setConstraints(
            Constraints
                .Builder()
                .apply {
                    if (!isManual) {
                        setRequiredNetworkType(NetworkType.CONNECTED)
                    }
                }.build(),
        ).setInputData(
            Data
                .Builder()
                .putBoolean(RecommendationScheduler.KEY_IS_MANUAL, isManual)
                .build(),
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS,
        ).build()

internal fun buildRecommendationPeriodicRequest(): PeriodicWorkRequest {
    val constraints =
        Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    return PeriodicWorkRequestBuilder<PeriodicRecommendationTriggerWorker>(
        RecommendationScheduler.PERIODIC_INTERVAL_DAYS,
        TimeUnit.DAYS,
    ).setConstraints(constraints)
        .build()
}

@Singleton
class RecommendationSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : RecommendationScheduler {
        private val workManager by lazy { WorkManager.getInstance(context) }

        override fun schedulePeriodicWork() {
            workManager.enqueueUniquePeriodicWork(
                RecommendationScheduler.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                buildRecommendationPeriodicRequest(),
            )
        }

        override fun triggerImmediateCalculation() {
            enqueueFetchAndEvaluation(isManual = true, replaceExisting = true)
        }

        override fun scheduleInitialCalculation() {
            enqueueFetchAndEvaluation(isManual = true, replaceExisting = false)
        }

        override fun enqueueFetchAndEvaluation(
            isManual: Boolean,
            replaceExisting: Boolean,
        ) {
            val policy =
                if (replaceExisting) {
                    ExistingWorkPolicy.REPLACE
                } else {
                    ExistingWorkPolicy.KEEP
                }
            workManager
                .beginUniqueWork(
                    RecommendationScheduler.FETCH_WORK_NAME,
                    policy,
                    buildRecommendationFetchRequest(isManual),
                ).then(buildEvaluationRequest(isManual))
                .enqueue()
        }

        private fun buildEvaluationRequest(isManual: Boolean): OneTimeWorkRequest {
            val constraintsBuilder = Constraints.Builder()
            if (!isManual) {
                constraintsBuilder.setRequiresDeviceIdle(true)
            }

            return OneTimeWorkRequestBuilder<EvaluateRecommendationsWorker>()
                .setConstraints(constraintsBuilder.build())
                .setInputData(
                    Data
                        .Builder()
                        .putBoolean(RecommendationScheduler.KEY_IS_MANUAL, isManual)
                        .build(),
                ).build()
        }
    }
