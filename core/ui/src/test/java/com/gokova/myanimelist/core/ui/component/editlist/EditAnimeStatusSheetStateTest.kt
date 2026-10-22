package com.gokova.myanimelist.core.ui.component.editlist

import org.junit.Assert.assertEquals
import org.junit.Test

class EditAnimeStatusSheetStateTest {
    @Test
    fun `withStatus to completed sets episodes to totalEpisodes when total is known`() {
        val state =
            EditAnimeStatusSheetState(
                animeId = 1L,
                title = "Monster",
                status = "watching",
                episodesWatched = 10,
                totalEpisodes = 74,
            )

        val updated = state.withStatus("completed")

        assertEquals("completed", updated.status)
        assertEquals(74, updated.episodesWatched)
    }

    @Test
    fun `withStatus to completed preserves episodesWatched when totalEpisodes is null`() {
        val state =
            EditAnimeStatusSheetState(
                animeId = 2L,
                title = "One Piece",
                status = "watching",
                episodesWatched = 1050,
                totalEpisodes = null,
            )

        val updated = state.withStatus("completed")

        assertEquals("completed", updated.status)
        assertEquals(1050, updated.episodesWatched)
    }

    @Test
    fun `withStatus to completed preserves episodesWatched when totalEpisodes is zero`() {
        val state =
            EditAnimeStatusSheetState(
                animeId = 2L,
                title = "One Piece",
                status = "watching",
                episodesWatched = 1050,
                totalEpisodes = 0,
            )

        val updated = state.withStatus("completed")

        assertEquals("completed", updated.status)
        assertEquals(1050, updated.episodesWatched)
    }

    @Test
    fun `withStatus to other statuses preserves episodesWatched`() {
        val state =
            EditAnimeStatusSheetState(
                animeId = 1L,
                title = "Monster",
                status = "completed",
                episodesWatched = 74,
                totalEpisodes = 74,
            )

        val updated = state.withStatus("watching")

        assertEquals("watching", updated.status)
        assertEquals(74, updated.episodesWatched)
    }

    @Test
    fun `withStatus to plan_to_watch preserves episodesWatched`() {
        val state =
            EditAnimeStatusSheetState(
                animeId = 1L,
                title = "Monster",
                status = "watching",
                episodesWatched = 15,
                totalEpisodes = 74,
            )

        val updated = state.withStatus("plan_to_watch")

        assertEquals("plan_to_watch", updated.status)
        assertEquals(15, updated.episodesWatched)
    }
}
