package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface RecommendationScheduler {
    fun schedulePeriodicWork()

    fun triggerImmediateCalculation()

    fun scheduleInitialCalculation()

    fun enqueueEvaluation(isManual: Boolean)

    companion object {
        const val KEY_IS_MANUAL = "is_manual"
        const val PERIODIC_WORK_NAME = "periodic_recommendation_fetch"
        const val FETCH_WORK_NAME = "one_time_recommendation_fetch"
        const val EVALUATE_WORK_NAME = "one_time_recommendation_evaluate"
        const val PERIODIC_INTERVAL_DAYS = 90L
    }
}

@Singleton
class RecommendationSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : RecommendationScheduler {
        private val workManager by lazy { WorkManager.getInstance(context) }

        override fun schedulePeriodicWork() {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val periodicRequest =
                PeriodicWorkRequestBuilder<FetchCandidatesWorker>(
                    RecommendationScheduler.PERIODIC_INTERVAL_DAYS,
                    TimeUnit.DAYS,
                ).setConstraints(constraints)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                RecommendationScheduler.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest,
            )
        }

        override fun triggerImmediateCalculation() {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val inputData =
                Data
                    .Builder()
                    .putBoolean(RecommendationScheduler.KEY_IS_MANUAL, true)
                    .build()

            val oneTimeRequest =
                OneTimeWorkRequestBuilder<FetchCandidatesWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .build()

            workManager.enqueueUniqueWork(
                RecommendationScheduler.FETCH_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest,
            )
        }

        override fun scheduleInitialCalculation() {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val inputData =
                Data
                    .Builder()
                    .putBoolean(RecommendationScheduler.KEY_IS_MANUAL, true)
                    .build()

            val oneTimeRequest =
                OneTimeWorkRequestBuilder<FetchCandidatesWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .build()

            workManager.enqueueUniqueWork(
                RecommendationScheduler.FETCH_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                oneTimeRequest,
            )
        }

        override fun enqueueEvaluation(isManual: Boolean) {
            val constraintsBuilder = Constraints.Builder()
            if (!isManual) {
                constraintsBuilder.setRequiresDeviceIdle(true)
            }

            val inputData =
                Data
                    .Builder()
                    .putBoolean(RecommendationScheduler.KEY_IS_MANUAL, isManual)
                    .build()

            val evaluateRequest =
                OneTimeWorkRequestBuilder<EvaluateRecommendationsWorker>()
                    .setConstraints(constraintsBuilder.build())
                    .setInputData(inputData)
                    .build()

            workManager.enqueueUniqueWork(
                RecommendationScheduler.EVALUATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                evaluateRequest,
            )
        }
    }
