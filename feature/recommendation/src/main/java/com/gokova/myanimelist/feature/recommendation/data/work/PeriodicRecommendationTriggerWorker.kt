package com.gokova.myanimelist.feature.recommendation.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gokova.myanimelist.core.domain.logging.AppLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PeriodicRecommendationTriggerWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val scheduler: RecommendationScheduler,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            AppLog.domain.i { "PeriodicRecommendationTriggerWorker started (workId=$id)" }
            scheduler.enqueueFetchAndEvaluation(isManual = false, replaceExisting = false)
            return Result.success()
        }
    }
