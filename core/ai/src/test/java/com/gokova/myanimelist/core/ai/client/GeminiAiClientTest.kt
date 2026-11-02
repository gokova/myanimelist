package com.gokova.myanimelist.core.ai.client

import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.ai.remote.GeminiAiService
import com.gokova.myanimelist.core.ai.remote.GeminiCandidateDto
import com.gokova.myanimelist.core.ai.remote.GeminiContentDto
import com.gokova.myanimelist.core.ai.remote.GeminiGenerateContentRequest
import com.gokova.myanimelist.core.ai.remote.GeminiGenerateContentResponse
import com.gokova.myanimelist.core.ai.remote.GeminiPartDto
import com.gokova.myanimelist.core.ai.remote.GeminiPromptBuilder
import com.gokova.myanimelist.core.ai.remote.GeminiPromptFeedbackDto
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@Suppress("TooManyFunctions")
class GeminiAiClientTest {
    private class FakeGeminiService(
        var responseToReturn: Response<GeminiGenerateContentResponse>? = null,
        var responsesQueue: MutableList<Response<GeminiGenerateContentResponse>> = mutableListOf(),
        var exceptionToThrow: IOException? = null,
    ) : GeminiAiService {
        var callCount: Int = 0
        var lastRequest: GeminiGenerateContentRequest? = null
        var lastApiKey: String? = null

        override suspend fun generateContent(
            apiKey: String,
            request: GeminiGenerateContentRequest,
        ): Response<GeminiGenerateContentResponse> {
            callCount++
            lastApiKey = apiKey
            lastRequest = request
            exceptionToThrow?.let { throw it }
            if (responsesQueue.isNotEmpty()) {
                return responsesQueue.removeAt(0)
            }
            return responseToReturn
                ?: Response.success(GeminiGenerateContentResponse())
        }
    }

    @Test
    fun `searchCandidates returns MissingApiKey when key is blank`() =
        runTest {
            val service = FakeGeminiService(Response.success(GeminiGenerateContentResponse()))
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("psychological anime", "   ")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiException.MissingApiKey)
        }

    @Test
    fun `searchCandidates returns InvalidResponse when prompt is blank`() =
        runTest {
            val service = FakeGeminiService(Response.success(GeminiGenerateContentResponse()))
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("   ", "valid_key")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiException.InvalidResponse)
        }

    @Test
    fun `searchCandidates successfully parses structured JSON response`() =
        runTest {
            val jsonPayload =
                """
                {
                  "intentSummary": "Dark robot mysteries",
                  "explanation": "Selected anime with themes of artificial intelligence and tension.",
                  "candidates": [
                    {
                      "title": "Monster",
                      "expectedYear": 2004,
                      "reason": "Top psychological mystery with brilliant pacing."
                    },
                    {
                      "title": "Pluto",
                      "expectedYear": 2023,
                      "reason": "Direct thematic match about murder and robotics."
                    }
                  ]
                }
                """.trimIndent()

            val successBody =
                GeminiGenerateContentResponse(
                    candidates =
                        listOf(
                            GeminiCandidateDto(
                                content =
                                    GeminiContentDto(
                                        parts = listOf(GeminiPartDto(text = jsonPayload)),
                                    ),
                                finishReason = "STOP",
                            ),
                        ),
                )
            val service = FakeGeminiService(Response.success(successBody))
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("dark robot anime", "valid_key")

            assertTrue(result.isSuccess)
            val interpretation = result.getOrThrow()
            assertEquals("Dark robot mysteries", interpretation.intentSummary)
            assertEquals(2, interpretation.candidates.size)
            assertEquals("Monster", interpretation.candidates[0].title)
            assertEquals(2004, interpretation.candidates[0].expectedYear)
            assertEquals("Pluto", interpretation.candidates[1].title)
        }

    @Test
    fun `searchCandidates redacts raw response and provides generic error on malformed json`() =
        runTest {
            val malformedPayload = "malformed sensitive json with secret user prompt"
            val successBody =
                GeminiGenerateContentResponse(
                    candidates =
                        listOf(
                            GeminiCandidateDto(
                                content =
                                    GeminiContentDto(
                                        parts = listOf(GeminiPartDto(text = malformedPayload)),
                                    ),
                                finishReason = "STOP",
                            ),
                        ),
                )
            val service = FakeGeminiService(Response.success(successBody))
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("prompt", "key")

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is AiException.InvalidResponse)
            assertFalse(exception?.message?.contains("sensitive") == true)
        }

    @Test
    fun `searchCandidates caps candidate lengths and filters out invalid year and blank title`() =
        runTest {
            val longTitle = "A".repeat(200)
            val longReason = "R".repeat(400)
            val jsonPayload =
                """
                {
                  "intentSummary": "Summary",
                  "explanation": "Explanation",
                  "candidates": [
                    {
                      "title": "$longTitle",
                      "expectedYear": 9999,
                      "reason": "$longReason"
                    },
                    {
                      "title": "   ",
                      "expectedYear": 2020,
                      "reason": "Blank title"
                    }
                  ]
                }
                """.trimIndent()

            val successBody =
                GeminiGenerateContentResponse(
                    candidates =
                        listOf(
                            GeminiCandidateDto(
                                content =
                                    GeminiContentDto(
                                        parts = listOf(GeminiPartDto(text = jsonPayload)),
                                    ),
                                finishReason = "STOP",
                            ),
                        ),
                )
            val service = FakeGeminiService(Response.success(successBody))
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("prompt", "key")

            assertTrue(result.isSuccess)
            val candidates = result.getOrThrow().candidates
            assertEquals(1, candidates.size)
            assertEquals(150, candidates[0].title.length)
            assertEquals(300, candidates[0].reason.length)
            assertNull(candidates[0].expectedYear)
        }

    @Test
    fun `searchCandidates rejects responses exceeding max length`() =
        runTest {
            val oversizedPayload = "x".repeat(20_000)
            val successBody =
                GeminiGenerateContentResponse(
                    candidates =
                        listOf(
                            GeminiCandidateDto(
                                content =
                                    GeminiContentDto(
                                        parts = listOf(GeminiPartDto(text = oversizedPayload)),
                                    ),
                                finishReason = "STOP",
                            ),
                        ),
                )
            val service = FakeGeminiService(Response.success(successBody))
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("prompt", "key")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiException.InvalidResponse)
        }

    @Test
    fun `searchCandidates maps HTTP 400 to InvalidApiKey`() =
        runTest {
            val errorResponse =
                Response.error<GeminiGenerateContentResponse>(
                    400,
                    "API key not valid".toResponseBody(),
                )
            val service = FakeGeminiService(errorResponse)
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("dark anime", "bad_key")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiException.InvalidApiKey)
        }

    @Test
    fun `searchCandidates maps HTTP 429 to QuotaExceeded`() =
        runTest {
            val errorResponse =
                Response.error<GeminiGenerateContentResponse>(
                    429,
                    "Quota exceeded".toResponseBody(),
                )
            val service = FakeGeminiService(errorResponse)
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("dark anime", "valid_key")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiException.QuotaExceeded)
        }

    @Test
    fun `searchCandidates maps safety block to SafetyBlocked`() =
        runTest {
            val safetyBody =
                GeminiGenerateContentResponse(
                    promptFeedback = GeminiPromptFeedbackDto(blockReason = "SAFETY"),
                )
            val service = FakeGeminiService(Response.success(safetyBody))
            val client = GeminiAiClient(service)

            val result = client.searchCandidates("harmful query", "valid_key")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiException.SafetyBlocked)
        }

    @Test
    fun `validateApiKey returns success on 200`() =
        runTest {
            val service = FakeGeminiService(Response.success(GeminiGenerateContentResponse()))
            val client = GeminiAiClient(service)

            val result = client.validateApiKey("good_key")

            assertTrue(result.isSuccess)
        }

    @Test
    fun `validateApiKey returns QuotaExceeded on 429`() =
        runTest {
            val errorResponse =
                Response.error<GeminiGenerateContentResponse>(429, "".toResponseBody())
            val service = FakeGeminiService(errorResponse)
            val client = GeminiAiClient(service)

            val result = client.validateApiKey("key")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiException.QuotaExceeded)
        }

    @Test
    fun `validateApiKey retries on HTTP 500 and succeeds on attempt 2`() =
        runTest {
            val errorResponse =
                Response.error<GeminiGenerateContentResponse>(500, "Server error".toResponseBody())
            val successResponse = Response.success(GeminiGenerateContentResponse())
            val service =
                FakeGeminiService(
                    responsesQueue = mutableListOf(errorResponse, successResponse),
                )
            val client = GeminiAiClient(service)

            val result = client.validateApiKey("key")

            assertTrue(result.isSuccess)
            assertEquals(2, service.callCount)
        }

    @Test
    fun `validateApiKey retries on HTTP 502 and succeeds on attempt 2`() =
        runTest {
            val errorResponse =
                Response.error<GeminiGenerateContentResponse>(502, "Bad Gateway".toResponseBody())
            val successResponse = Response.success(GeminiGenerateContentResponse())
            val service =
                FakeGeminiService(
                    responsesQueue = mutableListOf(errorResponse, successResponse),
                )
            val client = GeminiAiClient(service)

            val result = client.validateApiKey("key")

            assertTrue(result.isSuccess)
            assertEquals(2, service.callCount)
        }

    @Test
    fun `validateApiKey retries on HTTP 503 and succeeds on attempt 2`() =
        runTest {
            val errorResponse =
                Response.error<GeminiGenerateContentResponse>(
                    503,
                    "Service Unavailable".toResponseBody(),
                )
            val successResponse = Response.success(GeminiGenerateContentResponse())
            val service =
                FakeGeminiService(
                    responsesQueue = mutableListOf(errorResponse, successResponse),
                )
            val client = GeminiAiClient(service)

            val result = client.validateApiKey("key")

            assertTrue(result.isSuccess)
            assertEquals(2, service.callCount)
        }

    @Test
    fun `validateApiKey retries on HTTP 429 and succeeds on attempt 2`() =
        runTest {
            val errorResponse =
                Response.error<GeminiGenerateContentResponse>(
                    429,
                    "Rate limit".toResponseBody(),
                )
            val successResponse = Response.success(GeminiGenerateContentResponse())
            val service =
                FakeGeminiService(
                    responsesQueue = mutableListOf(errorResponse, successResponse),
                )
            val client = GeminiAiClient(service)

            val result = client.validateApiKey("key")

            assertTrue(result.isSuccess)
            assertEquals(2, service.callCount)
        }

    @Test
    fun `validateApiKey does not retry on HTTP 400 bad request`() =
        runTest {
            val errorResponse =
                Response.error<GeminiGenerateContentResponse>(
                    400,
                    "Bad Request".toResponseBody(),
                )
            val service = FakeGeminiService(errorResponse)
            val client = GeminiAiClient(service)

            val result = client.validateApiKey("bad_key")

            assertTrue(result.isFailure)
            assertEquals(1, service.callCount)
        }

    @Test
    fun `PromptBuilder sanitizes prompt and caps length`() {
        val longPrompt = "a".repeat(600)
        val request = GeminiPromptBuilder.buildSearchRequest(longPrompt)

        val text = request.contents[0].parts[0].text
        assertEquals(GeminiPromptBuilder.MAX_PROMPT_LENGTH, text.length)
    }
}
