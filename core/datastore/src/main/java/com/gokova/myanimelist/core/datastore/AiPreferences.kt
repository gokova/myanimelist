package com.gokova.myanimelist.core.datastore

import kotlinx.coroutines.flow.Flow

interface AiPreferences {
    val geminiApiKey: Flow<String?>

    suspend fun setGeminiApiKey(key: String)

    suspend fun clearGeminiApiKey()
}
