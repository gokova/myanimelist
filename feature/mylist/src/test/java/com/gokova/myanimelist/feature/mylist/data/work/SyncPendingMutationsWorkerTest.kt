package com.gokova.myanimelist.feature.mylist.data.work

import android.content.Context
import android.content.ContextWrapper
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.gokova.myanimelist.core.domain.usecase.SyncPendingMutationsUseCase
import com.gokova.myanimelist.feature.mylist.testing.FakePendingMutationRepository
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SyncPendingMutationsWorkerTest {
    private lateinit var pendingRepo: FakePendingMutationRepository
    private lateinit var syncUseCase: SyncPendingMutationsUseCase

    @Before
    fun setUp() {
        pendingRepo = FakePendingMutationRepository()
        syncUseCase = SyncPendingMutationsUseCase(pendingRepo)
    }

    @Test
    fun `doWork returns success when usecase succeeds`() =
        runTest {
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(Result.success(), result)
            assertEquals(true, pendingRepo.syncAllCalled)
        }

    @Test
    fun `doWork returns retry on IOException`() =
        runTest {
            pendingRepo.syncAllResult = kotlin.Result.failure(IOException("Network error"))
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(Result.retry(), result)
        }

    @Test
    fun `doWork returns retry on HTTP 500 server error`() =
        runTest {
            val http500 = HttpException(Response.error<Any>(500, "".toResponseBody(null)))
            pendingRepo.syncAllResult = kotlin.Result.failure(http500)
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(Result.retry(), result)
        }

    @Test
    fun `doWork returns retry on HTTP 429 rate limit error`() =
        runTest {
            val http429 = HttpException(Response.error<Any>(429, "".toResponseBody(null)))
            pendingRepo.syncAllResult = kotlin.Result.failure(http429)
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(Result.retry(), result)
        }

    @Test
    fun `doWork returns failure on HTTP 401 unauthorized error`() =
        runTest {
            val http401 = HttpException(Response.error<Any>(401, "".toResponseBody(null)))
            pendingRepo.syncAllResult = kotlin.Result.failure(http401)
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }

    @Test
    fun `doWork returns failure on generic exception`() =
        runTest {
            pendingRepo.syncAllResult =
                kotlin.Result.failure(IllegalStateException("Unknown state"))
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }

    private fun createWorker(): SyncPendingMutationsWorker {
        val fakeContext =
            object : ContextWrapper(null) {
                override fun getApplicationContext(): Context = this
            }
        val builder =
            TestListenableWorkerBuilder<SyncPendingMutationsWorker>(fakeContext)
                .setWorkerFactory(
                    object : WorkerFactory() {
                        override fun createWorker(
                            appContext: Context,
                            workerClassName: String,
                            workerParameters: WorkerParameters,
                        ): ListenableWorker =
                            SyncPendingMutationsWorker(
                                appContext,
                                workerParameters,
                                syncUseCase,
                            )
                    },
                )
        return builder.build()
    }
}
