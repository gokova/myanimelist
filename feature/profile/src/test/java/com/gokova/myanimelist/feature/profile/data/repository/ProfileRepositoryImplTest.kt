package com.gokova.myanimelist.feature.profile.data.repository

import com.gokova.myanimelist.core.datastore.AuthPreferences
import com.gokova.myanimelist.core.datastore.UserPreferences
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.network.api.MalApiService
import com.gokova.myanimelist.core.network.model.AnimeDetailsDto
import com.gokova.myanimelist.core.network.model.AnimeListResponseDto
import com.gokova.myanimelist.core.network.model.MyListStatusDto
import com.gokova.myanimelist.core.network.model.UserDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ProfileRepositoryImplTest {
    private lateinit var fakePreferences: FakeUserPreferences
    private lateinit var fakeAuthPreferences: FakeAuthPreferences
    private lateinit var fakeApi: FakeMalApiService
    private lateinit var repository: ProfileRepositoryImpl

    @Before
    fun setUp() {
        fakeAuthPreferences = FakeAuthPreferences()
        fakePreferences = FakeUserPreferences(fakeAuthPreferences)
        fakeApi = FakeMalApiService()
        repository =
            ProfileRepositoryImpl(
                apiService = fakeApi,
                userPreferences = fakePreferences,
                authPreferences = fakeAuthPreferences,
            )
    }

    @Test
    fun `refreshUserProfile saves to preferences on success`() =
        runTest {
            fakeApi.userResponse =
                UserDto(
                    id = 123L,
                    name = "john_doe",
                    picture = "https://example.com/pic.jpg",
                )

            val result = repository.refreshUserProfile()

            assertTrue(result.isSuccess)
            val profile = result.getOrThrow()
            assertEquals(123L, profile.id)
            assertEquals("john_doe", profile.name)

            val saved = fakePreferences.userProfile.first()
            assertEquals(profile, saved)
        }

    @Test
    fun `refreshUserProfile returns failure on api exception`() =
        runTest {
            fakeApi.exceptionToThrow = IOException("Network down")

            val result = repository.refreshUserProfile()

            assertTrue(result.isFailure)
            assertNull(fakePreferences.userProfile.first())
        }

    @Test
    fun `refreshUserProfile retries on transient IOException and succeeds`() =
        runTest {
            var calls = 0
            fakeApi.onGetUserProfile = {
                calls++
                if (calls == 1) {
                    throw IOException("Temporary glitch")
                }
                UserDto(id = 555L, name = "retried_user")
            }

            val result = repository.refreshUserProfile()

            assertTrue(result.isSuccess)
            assertEquals("retried_user", result.getOrThrow().name)
            assertEquals(2, calls)
        }

    @Test
    fun `clearUserProfile clears preferences`() =
        runTest {
            fakePreferences.saveUserProfile(UserProfile(id = 1L, name = "user1"))

            repository.clearUserProfile()

            assertNull(fakePreferences.userProfile.first())
        }

    @Test
    fun `refreshUserProfile rethrows cancellation exception`() =
        runTest {
            fakeApi.exceptionToThrow = CancellationException("Coroutines cancelled")

            try {
                repository.refreshUserProfile()
                fail("Expected CancellationException to be rethrown")
            } catch (_: CancellationException) {
                // Expected behavior per AGENTS.md
            }
        }

    @Test
    fun `refreshUserProfile rejects persistence when session is cleared during request`() =
        runTest {
            fakeApi.onGetUserProfile = {
                fakeAuthPreferences.isLoggedInFlow.value = false
                fakeAuthPreferences.sessionIdFlow.value = null
                UserDto(id = 999L, name = "stale_user")
            }

            val result = repository.refreshUserProfile()

            assertTrue(result.isFailure)
            assertNull(fakePreferences.userProfile.first())
        }

    @Test
    fun `refreshUserProfile rejects persistence when session changes during request`() =
        runTest {
            fakeApi.onGetUserProfile = {
                fakeAuthPreferences.sessionIdFlow.value = "different_session_id"
                UserDto(id = 999L, name = "stale_user")
            }

            val result = repository.refreshUserProfile()

            assertTrue(result.isFailure)
            assertNull(fakePreferences.userProfile.first())
        }

    private class FakeAuthPreferences : AuthPreferences {
        val isLoggedInFlow = MutableStateFlow(true)
        val sessionIdFlow = MutableStateFlow<String?>("initial_session")
        override val accessToken: Flow<String?> = MutableStateFlow("token")
        override val refreshToken: Flow<String?> = MutableStateFlow("refresh")
        override val isLoggedIn: Flow<Boolean> = isLoggedInFlow
        override val codeVerifier: Flow<String?> = MutableStateFlow(null)
        override val oauthState: Flow<String?> = MutableStateFlow(null)
        override val sessionId: Flow<String?> = sessionIdFlow

        override suspend fun saveTokens(
            accessToken: String,
            refreshToken: String,
            isNewSession: Boolean,
        ) {
            // No-op in test fake
        }

        override suspend fun clearTokens() {
            isLoggedInFlow.value = false
            sessionIdFlow.value = null
        }

        override suspend fun saveOAuthSession(
            verifier: String,
            state: String,
        ) {
            // No-op in test fake
        }

        override suspend fun clearOAuthSession() {
            // No-op in test fake
        }

        override suspend fun warmCache() {
            // No-op in test fake
        }

        override fun getAccessTokenSync(): String? = if (isLoggedInFlow.value) "token" else null

        override fun getRefreshTokenSync(): String? = if (isLoggedInFlow.value) "refresh" else null
    }

    private class FakeUserPreferences(
        private val authPreferences: FakeAuthPreferences? = null,
    ) : UserPreferences {
        private val profileFlow = MutableStateFlow<UserProfile?>(null)
        override val userProfile: Flow<UserProfile?> = profileFlow

        override suspend fun saveUserProfile(
            profile: UserProfile,
            expectedSessionId: String?,
        ): Boolean {
            if (authPreferences != null) {
                val currentSessionId = authPreferences.sessionId.first()
                if (currentSessionId == null ||
                    (expectedSessionId != null && currentSessionId != expectedSessionId)
                ) {
                    return false
                }
            }
            profileFlow.value = profile
            return true
        }

        override suspend fun clearUserProfile() {
            profileFlow.value = null
        }
    }

    private class FakeMalApiService : MalApiService {
        var userResponse: UserDto = UserDto(id = 1L, name = "default")
        var exceptionToThrow: Exception? = null
        var onGetUserProfile: (() -> UserDto)? = null

        override suspend fun getUserProfile(
            userId: String,
            fields: String,
        ): UserDto {
            onGetUserProfile?.let { return it() }
            exceptionToThrow?.let { throw it }
            return userResponse
        }

        override suspend fun getUserAnimeList(
            fields: String,
            limit: Int,
            offset: Int,
            nsfw: Boolean,
        ): AnimeListResponseDto = throw UnsupportedOperationException()

        override suspend fun getAnimeListNextPage(url: String): AnimeListResponseDto =
            throw UnsupportedOperationException()

        override suspend fun searchAnime(
            query: String,
            limit: Int,
            offset: Int,
            fields: String,
            nsfw: Boolean,
        ): AnimeListResponseDto = throw UnsupportedOperationException()

        override suspend fun getAnimeRanking(
            rankingType: String,
            limit: Int,
            offset: Int,
            fields: String,
            nsfw: Boolean,
        ): AnimeListResponseDto = throw UnsupportedOperationException()

        override suspend fun getSeasonalAnime(
            year: Int,
            season: String,
            limit: Int,
            offset: Int,
            fields: String,
        ): AnimeListResponseDto = throw UnsupportedOperationException()

        override suspend fun getAnimeDetails(
            animeId: Long,
            fields: String,
        ): AnimeDetailsDto = throw UnsupportedOperationException()

        override suspend fun updateMyListStatus(
            animeId: Long,
            status: String,
            numWatchedEpisodes: Int,
            score: Int,
        ): MyListStatusDto = throw UnsupportedOperationException()
    }
}
