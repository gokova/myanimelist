package com.gokova.myanimelist.feature.recommendation.data.work

import androidx.work.BackoffPolicy
import androidx.work.NetworkType
import androidx.work.WorkRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RecommendationSchedulerWorkRequestTest {
    @Test
    fun `manual candidate fetch is unconstrained and uses exponential backoff`() {
        val request = buildRecommendationFetchRequest(isManual = true)

        assertEquals(
            NetworkType.NOT_REQUIRED,
            request.workSpec.constraints.requiredNetworkType,
        )
        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
        assertEquals(WorkRequest.MIN_BACKOFF_MILLIS, request.workSpec.backoffDelayDuration)
        assertEquals(
            true,
            request.workSpec.input.getBoolean(RecommendationScheduler.KEY_IS_MANUAL, false),
        )
    }

    @Test
    fun `periodic trigger is network constrained but not idle constrained`() {
        val periodicRequest = buildRecommendationPeriodicRequest()

        assertEquals(
            NetworkType.CONNECTED,
            periodicRequest.workSpec.constraints.requiredNetworkType,
        )
        assertFalse(periodicRequest.workSpec.constraints.requiresDeviceIdle())
    }
}
