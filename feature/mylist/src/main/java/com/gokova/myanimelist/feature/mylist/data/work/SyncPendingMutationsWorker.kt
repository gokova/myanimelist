package com.gokova.myanimelist.feature.mylist.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gokova.myanimelist.core.domain.logging.AppLog
import com.gokova.myanimelist.core.domain.usecase.SyncPendingMutationsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

@HiltWorker
class SyncPendingMutationsWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val syncPendingMutationsUseCase: SyncPendingMutationsUseCase,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            try {
                AppLog.data.i { "SyncPendingMutationsWorker started (workId=$id)" }
                val result = syncPendingMutationsUseCase()
                result.fold(
                    onSuccess = {
                        AppLog.data.i { "SyncPendingMutationsWorker completed successfully" }
                        Result.success()
                    },
                    onFailure = { error ->
                        AppLog.data.w(error) { "SyncPendingMutationsWorker encountered an error" }
                        when (error) {
                            is IOException -> Result.retry()
                            is HttpException -> {
                                if (error.code() in SERVER_ERROR_RANGE ||
                                    error.code() == HTTP_TOO_MANY_REQUESTS
                                ) {
                                    Result.retry()
                                } else {
                                    Result.failure()
                                }
                            }
                            else -> Result.failure()
                        }
                    },
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                AppLog.data.e(e) { "Unhandled exception in SyncPendingMutationsWorker" }
                Result.failure()
            }

        companion object {
            private const val HTTP_TOO_MANY_REQUESTS = 429
            private val SERVER_ERROR_RANGE = 500..599
        }
    }
