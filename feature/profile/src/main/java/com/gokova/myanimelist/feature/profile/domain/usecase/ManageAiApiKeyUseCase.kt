package com.gokova.myanimelist.feature.profile.domain.usecase

import com.gokova.myanimelist.core.ai.client.AiSearchClient
import com.gokova.myanimelist.core.datastore.AiPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageAiApiKeyUseCase
    @Inject
    constructor(
        private val aiPreferences: AiPreferences,
        private val aiSearchClient: AiSearchClient,
    ) {
        val apiKey: Flow<String?> = aiPreferences.geminiApiKey

        suspend fun saveKey(key: String) {
            aiPreferences.setGeminiApiKey(key)
        }

        suspend fun clearKey() {
            aiPreferences.clearGeminiApiKey()
        }

        suspend fun testKey(key: String): Result<Unit> = aiSearchClient.validateApiKey(key)
    }
