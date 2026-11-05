package com.gokova.myanimelist.core.ai.client

import com.gokova.myanimelist.core.ai.model.AiSearchInterpretation

interface AiSearchClient {
    suspend fun searchCandidates(
        prompt: String,
        apiKey: String,
    ): Result<AiSearchInterpretation>

    suspend fun validateApiKey(apiKey: String): Result<Unit>
}
