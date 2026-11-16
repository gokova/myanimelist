package com.gokova.myanimelist.feature.mylist.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface PendingMutationScheduler {
    fun scheduleSync()
}

@Singleton
class PendingMutationSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PendingMutationScheduler {
        private val workManager by lazy { WorkManager.getInstance(context) }

        override fun scheduleSync() {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val workRequest =
                OneTimeWorkRequestBuilder<SyncPendingMutationsWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS,
                    ).build()

            workManager.enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest,
            )
        }

        companion object {
            const val SYNC_WORK_NAME = "sync_pending_anime_mutations"
        }
    }
