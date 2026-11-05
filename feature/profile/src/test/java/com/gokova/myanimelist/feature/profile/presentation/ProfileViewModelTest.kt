package com.gokova.myanimelist.feature.profile.presentation

import com.gokova.myanimelist.core.ai.client.AiSearchClient
import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.ai.model.AiSearchInterpretation
import com.gokova.myanimelist.core.datastore.AiPreferences
import com.gokova.myanimelist.core.domain.model.IgnoredAnime
import com.gokova.myanimelist.core.domain.model.ImportIgnoredAnimeResult
import com.gokova.myanimelist.core.domain.model.UserProfile
import com.gokova.myanimelist.core.domain.repository.IgnoredAnimeRepository
import com.gokova.myanimelist.core.domain.repository.ProfileRepository
import com.gokova.myanimelist.core.domain.usecase.ExportIgnoredAnimeUseCase
import com.gokova.myanimelist.core.domain.usecase.IgnoredAnimeBackupInteractor
import com.gokova.myanimelist.core.domain.usecase.ImportIgnoredAnimeUseCase
import com.gokova.myanimelist.core.domain.usecase.ObserveIgnoredAnimeCountUseCase
import com.gokova.myanimelist.core.domain.usecase.ObserveUserProfileUseCase
import com.gokova.myanimelist.core.domain.usecase.RefreshUserProfileUseCase
import com.gokova.myanimelist.feature.profile.R
import com.gokova.myanimelist.feature.profile.domain.usecase.ManageAiApiKeyUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeIgnoredRepository: FakeIgnoredAnimeRepository
    private lateinit var fakeAiPreferences: FakeAiPreferences
    private lateinit var fakeAiSearchClient: FakeAiSearchClient
    private lateinit var observeProfileUseCase: ObserveUserProfileUseCase
    private lateinit var refreshProfileUseCase: RefreshUserProfileUseCase
    private lateinit var observeIgnoredCountUseCase: ObserveIgnoredAnimeCountUseCase
    private lateinit var exportIgnoredUseCase: ExportIgnoredAnimeUseCase
    private lateinit var importIgnoredUseCase: ImportIgnoredAnimeUseCase
    private lateinit var manageAiApiKeyUseCase: ManageAiApiKeyUseCase
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeProfileRepository = FakeProfileRepository()
        fakeIgnoredRepository = FakeIgnoredAnimeRepository()
        fakeAiPreferences = FakeAiPreferences()
        fakeAiSearchClient = FakeAiSearchClient()
        observeProfileUseCase = ObserveUserProfileUseCase(fakeProfileRepository)
        refreshProfileUseCase = RefreshUserProfileUseCase(fakeProfileRepository)
        observeIgnoredCountUseCase = ObserveIgnoredAnimeCountUseCase(fakeIgnoredRepository)
        exportIgnoredUseCase = ExportIgnoredAnimeUseCase(fakeIgnoredRepository)
        importIgnoredUseCase = ImportIgnoredAnimeUseCase(fakeIgnoredRepository)
        manageAiApiKeyUseCase = ManageAiApiKeyUseCase(fakeAiPreferences, fakeAiSearchClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileViewModel =
        ProfileViewModel(
            observeUserProfileUseCase = observeProfileUseCase,
            refreshUserProfileUseCase = refreshProfileUseCase,
            backupInteractor =
                IgnoredAnimeBackupInteractor(
                    observeCount = observeIgnoredCountUseCase,
                    export = exportIgnoredUseCase,
                    import = importIgnoredUseCase,
                ),
            manageAiApiKeyUseCase = manageAiApiKeyUseCase,
            ioDispatcher = testDispatcher,
        )

    @Test
    fun `initial state emits content when profile is cached`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Content)
            assertEquals(cached, (state as ProfileUiState.Content).userProfile)
            assertEquals(0, state.ignoredAnimeCount)
            assertFalse(state.showLogoutDialog)
            collectJob.cancel()
        }

    @Test
    fun `content exposes ignoredAnimeCount from ignored anime repository`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)
            fakeIgnoredRepository.setAnimeNotInterested(101L, "Ignored 1")
            fakeIgnoredRepository.setAnimeNotInterested(102L, "Ignored 2")

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals(2, state.ignoredAnimeCount)
            collectJob.cancel()
        }

    @Test
    fun `exportIgnoredAnime success emits ShowExportSuccessSnackbar`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached
            fakeIgnoredRepository.setAnimeNotInterested(1L, "Anime 1")

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }
            advanceUntilIdle()

            val outputStream = ByteArrayOutputStream()
            viewModel.exportIgnoredAnime { outputStream }
            advanceUntilIdle()

            assertEquals(ProfileUiEvent.ShowExportSuccessSnackbar(1), emittedEvent)
            assertTrue(outputStream.toString(Charsets.UTF_8.name()).contains("Anime 1"))

            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `exportIgnoredAnime failure when stream cannot be opened emits ShowErrorSnackbar`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }
            advanceUntilIdle()

            viewModel.exportIgnoredAnime { null }
            advanceUntilIdle()

            assertEquals(
                ProfileUiEvent.ShowErrorSnackbar(R.string.profile_ignored_export_failed),
                emittedEvent,
            )
            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `importIgnoredAnime success emits ShowImportSuccessSnackbar`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }
            advanceUntilIdle()

            val csv =
                """
                anime_id,title
                1,"Frieren, The Beyond"
                """.trimIndent()
            viewModel.importIgnoredAnime { ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)) }
            advanceUntilIdle()

            assertEquals(
                ProfileUiEvent.ShowImportSuccessSnackbar(
                    totalCount = 1,
                    newCount = 1,
                    updatedCount = 0,
                ),
                emittedEvent,
            )
            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `importIgnoredAnime invalid format emits ShowErrorSnackbar with invalid format message`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }
            advanceUntilIdle()

            val brokenCsv =
                """
                anime_id,title
                1,"Unclosed quote
                """.trimIndent()
            viewModel.importIgnoredAnime {
                ByteArrayInputStream(
                    brokenCsv.toByteArray(Charsets.UTF_8),
                )
            }
            advanceUntilIdle()

            assertEquals(
                ProfileUiEvent.ShowErrorSnackbar(R.string.profile_ignored_import_invalid),
                emittedEvent,
            )
            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `importIgnoredAnime empty file emits ShowErrorSnackbar with empty file message`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }
            advanceUntilIdle()

            viewModel.importIgnoredAnime { ByteArrayInputStream("".toByteArray(Charsets.UTF_8)) }
            advanceUntilIdle()

            assertEquals(
                ProfileUiEvent.ShowErrorSnackbar(R.string.profile_ignored_import_empty),
                emittedEvent,
            )
            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `refresh failure emits error when cache is empty`() =
        runTest(testDispatcher) {
            fakeProfileRepository.refreshResult = Result.failure(IOException("Failed to connect"))

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Error)
            assertEquals(R.string.profile_load_failed, (state as ProfileUiState.Error).messageResId)
            collectJob.cancel()
        }

    @Test
    fun `refresh failure with cached profile keeps content and emits ShowErrorSnackbar event`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.failure(IOException("Failed to connect"))

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Content)
            assertEquals(cached, (state as ProfileUiState.Content).userProfile)
            assertEquals(
                ProfileUiEvent.ShowErrorSnackbar(R.string.profile_load_failed),
                emittedEvent,
            )
            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `cached content exposes refreshing state while refresh is in progress`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "cached_user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshGate = CompletableDeferred()

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val refreshingState = viewModel.uiState.value as ProfileUiState.Content
            assertTrue(refreshingState.isRefreshing)

            fakeProfileRepository.refreshGate?.complete(Unit)
            advanceUntilIdle()

            val loadedState = viewModel.uiState.value as ProfileUiState.Content
            assertFalse(loadedState.isRefreshing)
            collectJob.cancel()
        }

    @Test
    fun `setLogoutDialogVisible shows dialog and hides dialog`() =
        runTest(testDispatcher) {
            fakeProfileRepository.profileFlow.value = UserProfile(id = 1L, name = "user")
            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.setLogoutDialogVisible(true)
            advanceUntilIdle()
            var state = viewModel.uiState.value as ProfileUiState.Content
            assertTrue(state.showLogoutDialog)

            viewModel.setLogoutDialogVisible(false)
            advanceUntilIdle()
            state = viewModel.uiState.value as ProfileUiState.Content
            assertFalse(state.showLogoutDialog)
            collectJob.cancel()
        }

    @Test
    fun `onLogoutConfirmed sends RequestLogout event and closes dialog`() =
        runTest(testDispatcher) {
            fakeProfileRepository.profileFlow.value = UserProfile(id = 1L, name = "user")
            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.setLogoutDialogVisible(true)
            advanceUntilIdle()

            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }

            viewModel.onLogoutConfirmed()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertFalse(state.showLogoutDialog)
            assertEquals(ProfileUiEvent.RequestLogout, emittedEvent)
            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `onBackClicked sends NavigateBack event`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            var emittedEvent: ProfileUiEvent? = null
            val eventJob = launch { emittedEvent = viewModel.uiEvent.first() }

            viewModel.onBackClicked()
            advanceUntilIdle()

            assertEquals(ProfileUiEvent.NavigateBack, emittedEvent)
            eventJob.cancel()
            collectJob.cancel()
        }

    @Test
    fun `saveAiApiKey updates preferences and emits in uiState`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.saveAiApiKey("AIzaSyMyKey")
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals("AIzaSyMyKey", state.aiApiKey)
            assertEquals("AIzaSyMyKey", fakeAiPreferences.keyFlow.value)
            collectJob.cancel()
        }

    @Test
    fun `clearAiApiKey removes key from preferences and uiState`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)
            fakeAiPreferences.keyFlow.value = "existing_key"

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.clearAiApiKey()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertNull(state.aiApiKey)
            assertNull(fakeAiPreferences.keyFlow.value)
            collectJob.cancel()
        }

    @Test
    fun `testAiApiKey updates validation status to Valid on success`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)
            fakeAiSearchClient.validateResult = Result.success(Unit)

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.testAiApiKey("valid_key")
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals(AiKeyValidationStatus.Valid, state.aiKeyValidationStatus)
            collectJob.cancel()
        }

    @Test
    fun `testAiApiKey updates validation status to Invalid on failure`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)
            fakeAiSearchClient.validateResult = Result.failure(Exception("bad key"))

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.testAiApiKey("bad_key")
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals(AiKeyValidationStatus.Invalid, state.aiKeyValidationStatus)
            collectJob.cancel()
        }

    @Test
    fun `testAiApiKey updates validation status to QuotaExceeded on 429`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)
            fakeAiSearchClient.validateResult = Result.failure(AiException.QuotaExceeded())

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.testAiApiKey("key")
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals(AiKeyValidationStatus.QuotaExceeded, state.aiKeyValidationStatus)
            collectJob.cancel()
        }

    @Test
    fun `testAiApiKey updates validation status to ServiceUnavailable on 503`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)
            fakeAiSearchClient.validateResult =
                Result.failure(AiException.NetworkError("HTTP error 503"))

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.testAiApiKey("key")
            advanceUntilIdle()

            val state = viewModel.uiState.value as ProfileUiState.Content
            assertEquals(AiKeyValidationStatus.ServiceUnavailable, state.aiKeyValidationStatus)
            collectJob.cancel()
        }

    @Test
    fun `setAiInfoDialogVisible toggles dialog state`() =
        runTest(testDispatcher) {
            val cached = UserProfile(id = 1L, name = "user")
            fakeProfileRepository.profileFlow.value = cached
            fakeProfileRepository.refreshResult = Result.success(cached)

            viewModel = createViewModel()
            val collectJob = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            viewModel.setAiInfoDialogVisible(true)
            advanceUntilIdle()

            var state = viewModel.uiState.value as ProfileUiState.Content
            assertTrue(state.showAiInfoDialog)

            viewModel.setAiInfoDialogVisible(false)
            advanceUntilIdle()

            state = viewModel.uiState.value as ProfileUiState.Content
            assertFalse(state.showAiInfoDialog)
            collectJob.cancel()
        }

    private class FakeProfileRepository : ProfileRepository {
        val profileFlow = MutableStateFlow<UserProfile?>(null)
        var refreshGate: CompletableDeferred<Unit>? = null
        var refreshResult: Result<UserProfile> =
            Result.success(
                UserProfile(id = 1L, name = "refreshed"),
            )

        override val userProfile: Flow<UserProfile?> = profileFlow

        override suspend fun refreshUserProfile(): Result<UserProfile> {
            refreshGate?.await()
            return refreshResult.onSuccess { profileFlow.value = it }
        }

        override suspend fun clearUserProfile() {
            profileFlow.value = null
        }
    }

    private class FakeIgnoredAnimeRepository : IgnoredAnimeRepository {
        private val ignoredMap = MutableStateFlow<Map<Long, IgnoredAnime>>(emptyMap())

        override fun observeIgnoredAnimeIds(): Flow<List<Long>> =
            ignoredMap.map { it.keys.toList() }

        override suspend fun getIgnoredAnimeIds(): List<Long> = ignoredMap.value.keys.toList()

        override fun observeIsIgnored(animeId: Long): Flow<Boolean> =
            ignoredMap.map { it.containsKey(animeId) }

        override suspend fun isIgnored(animeId: Long): Boolean =
            ignoredMap.value.containsKey(animeId)

        override suspend fun setAnimeNotInterested(
            animeId: Long,
            title: String,
            titleEnglish: String?,
            pictureUrl: String?,
        ) {
            ignoredMap.value += (
                animeId to
                    IgnoredAnime(
                        animeId = animeId,
                        title = title,
                        titleEnglish = titleEnglish,
                        pictureUrl = pictureUrl,
                        createdAt = System.currentTimeMillis(),
                    )
            )
        }

        override suspend fun removeAnimeFromNotInterested(animeId: Long) {
            ignoredMap.value -= animeId
        }

        override fun observeAllIgnoredAnime(): Flow<List<IgnoredAnime>> =
            ignoredMap.map { it.values.toList() }

        override suspend fun getAllIgnoredAnime(): List<IgnoredAnime> =
            ignoredMap.value.values.toList()

        override fun observeIgnoredAnimeCount(): Flow<Int> = ignoredMap.map { it.size }

        override suspend fun upsertIgnoredAnimeList(
            items: List<IgnoredAnime>,
        ): ImportIgnoredAnimeResult {
            if (items.isEmpty()) {
                return ImportIgnoredAnimeResult.Failure.EmptyFile
            }
            val distinct = items.distinctBy { it.animeId }
            val currentIds = ignoredMap.value.keys
            val newCount = distinct.count { it.animeId !in currentIds }
            val updatedCount = distinct.count { it.animeId in currentIds }

            ignoredMap.value += distinct.associateBy { it.animeId }
            return ImportIgnoredAnimeResult.Success(
                totalCount = distinct.size,
                newCount = newCount,
                updatedCount = updatedCount,
            )
        }
    }

    private class FakeAiPreferences : AiPreferences {
        val keyFlow = MutableStateFlow<String?>(null)
        override val geminiApiKey: Flow<String?> = keyFlow

        override suspend fun setGeminiApiKey(key: String) {
            keyFlow.value = key
        }

        override suspend fun clearGeminiApiKey() {
            keyFlow.value = null
        }
    }

    private class FakeAiSearchClient : AiSearchClient {
        var validateResult: Result<Unit> = Result.success(Unit)

        override suspend fun searchCandidates(
            prompt: String,
            apiKey: String,
        ): Result<AiSearchInterpretation> =
            Result.success(
                AiSearchInterpretation("summary", "explanation", emptyList()),
            )

        override suspend fun validateApiKey(apiKey: String): Result<Unit> = validateResult
    }
}
