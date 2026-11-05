package com.gokova.myanimelist.core.ui.theme

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTokensTest {
    @Test
    fun posterMotionTokens_meetSpecificationRanges() {
        assertTrue(
            "Poster enter duration must be between 180ms and 220ms",
            MotionTokens.DURATION_POSTER_ENTER in 180..220,
        )
        assertTrue(
            "Poster exit duration must be between 120ms and 160ms",
            MotionTokens.DURATION_POSTER_EXIT in 120..160,
        )
        assertTrue(
            "Poster min scale should be between 0.95 and 1.0",
            MotionTokens.SCALE_POSTER_MIN in 0.95f..1.0f,
        )
    }

    @Test
    fun subsequentPhaseMotionTokens_meetSpecificationRanges() {
        assertTrue(
            "Content fade duration must be between 100ms and 150ms",
            MotionTokens.DURATION_CONTENT_FADE in 100..150,
        )
        assertTrue(
            "Progress segment duration must be between 150ms and 200ms",
            MotionTokens.DURATION_PROGRESS_SEGMENT in 150..200,
        )
        assertTrue(
            "Item placement duration must be between 180ms and 220ms",
            MotionTokens.DURATION_ITEM_PLACEMENT in 180..220,
        )
        assertTrue(
            "Chart recenter duration must be between 200ms and 250ms",
            MotionTokens.DURATION_CHART_RECENTER in 200..250,
        )
    }

    @Test
    fun easings_areNotNull() {
        assertNotNull(MotionTokens.EasingStandard)
        assertNotNull(MotionTokens.EasingEnter)
        assertNotNull(MotionTokens.EasingExit)
    }
}
