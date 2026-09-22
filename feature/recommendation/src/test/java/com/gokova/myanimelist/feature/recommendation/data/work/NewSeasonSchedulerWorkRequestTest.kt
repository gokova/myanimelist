package com.gokova.myanimelist.feature.recommendation.data.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkRequest
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class NewSeasonSchedulerWorkRequestTest {
    @Test
    fun `periodic work request builds without exception on idle constraints`() {
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

        assertNotNull(periodicRequest)
    }

    @Test
    fun `one time work request builds successfully with linear backoff`() {
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

        assertNotNull(oneTimeRequest)
    }
}
