package com.gokova.myanimelist.core.ai.client

import com.gokova.myanimelist.core.ai.model.AiCandidateHint
import com.gokova.myanimelist.core.ai.model.AiException
import com.gokova.myanimelist.core.ai.model.AiSearchInterpretation
import com.gokova.myanimelist.core.ai.remote.GeminiAiService
import com.gokova.myanimelist.core.ai.remote.GeminiGenerateContentResponse
import com.gokova.myanimelist.core.ai.remote.GeminiPromptBuilder
import com.gokova.myanimelist.core.ai.remote.GeminiStructuredInterpretationDto
import com.gokova.myanimelist.core.domain.logging.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class GeminiAiClient
    @Inject
    constructor(
        private val apiService: GeminiAiService,
    ) : AiSearchClient {
        private val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        @Suppress("ReturnCount")
        override suspend fun searchCandidates(
            prompt: String,
            apiKey: String,
        ): Result<AiSearchInterpretation> {
            if (apiKey.isBlank()) {
                return Result.failure(AiException.MissingApiKey())
            }
            if (prompt.isBlank()) {
                return Result.failure(
                    AiException.InvalidResponse("Search prompt cannot be blank"),
                )
            }

            return try {
                val request = GeminiPromptBuilder.buildSearchRequest(prompt)
                val response =
                    executeWithRetry {
                        apiService.generateContent(
                            apiKey = apiKey.trim(),
                            request = request,
                        )
                    }
                parseSearchResponse(response)
            } catch (ce: CancellationException) {
                throw ce
            } catch (ioe: IOException) {
                Result.failure(AiException.NetworkError("Network communication error", ioe))
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                val error =
                    e as? AiException ?: AiException.InvalidResponse(
                        e.message ?: "Unknown error",
                    )
                Result.failure(error)
            }
        }

        override suspend fun validateApiKey(apiKey: String): Result<Unit> {
            if (apiKey.isBlank()) {
                return Result.failure(AiException.MissingApiKey())
            }

            return try {
                val request = GeminiPromptBuilder.buildValidationRequest()
                val response =
                    executeWithRetry {
                        apiService.generateContent(
                            apiKey = apiKey.trim(),
                            request = request,
                        )
                    }
                val code = response.code()
                when {
                    response.isSuccessful -> Result.success(Unit)
                    code in listOf(HTTP_BAD_REQUEST, HTTP_UNAUTHORIZED, HTTP_FORBIDDEN) -> {
                        AppLog.network.w { "Gemini API key validation failed with HTTP $code" }
                        Result.failure(AiException.InvalidApiKey())
                    }
                    code == HTTP_TOO_MANY_REQUESTS -> {
                        AppLog.network.w { "Gemini API quota exceeded (HTTP 429)" }
                        Result.failure(AiException.QuotaExceeded())
                    }
                    else -> {
                        AppLog.network.w { "Gemini validation returned HTTP $code" }
                        Result.failure(AiException.NetworkError("HTTP error $code"))
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (ioe: IOException) {
                Result.failure(AiException.NetworkError("Network communication error", ioe))
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Result.failure(
                    AiException.NetworkError(e.message ?: "Unknown validation error", e),
                )
            }
        }

        @Suppress("ReturnCount")
        private fun parseSearchResponse(
            response: Response<GeminiGenerateContentResponse>,
        ): Result<AiSearchInterpretation> {
            if (!response.isSuccessful) {
                return handleHttpError(response.code())
            }

            val body =
                response.body() ?: return Result.failure(
                    AiException.InvalidResponse("Empty response body from Gemini"),
                )

            if (body.promptFeedback?.blockReason != null) {
                return Result.failure(
                    AiException.SafetyBlocked("Blocked: ${body.promptFeedback.blockReason}"),
                )
            }

            val candidate =
                body.candidates?.firstOrNull() ?: return Result.failure(
                    AiException.InvalidResponse("No candidates returned from Gemini"),
                )

            if (candidate.finishReason == FINISH_REASON_SAFETY) {
                return Result.failure(AiException.SafetyBlocked())
            }

            val text =
                candidate.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text ?: return Result.failure(
                    AiException.InvalidResponse("Missing text in Gemini candidate content"),
                )

            if (text.length > MAX_RESPONSE_LENGTH) {
                return Result.failure(
                    AiException.InvalidResponse("Gemini response exceeds maximum size"),
                )
            }

            return try {
                val structured = json.decodeFromString<GeminiStructuredInterpretationDto>(text)
                Result.success(mapStructuredResponse(structured))
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Result.failure(
                    AiException.InvalidResponse(
                        "Failed to parse structured response from Gemini",
                        e,
                    ),
                )
            }
        }

        private fun mapStructuredResponse(
            dto: GeminiStructuredInterpretationDto,
        ): AiSearchInterpretation {
            val domainCandidates =
                dto.candidates
                    .filter { it.title.isNotBlank() }
                    .take(GeminiPromptBuilder.MAX_CANDIDATES)
                    .map { item ->
                        val validYear =
                            item.expectedYear?.takeIf { year ->
                                year in MIN_VALID_YEAR..MAX_VALID_YEAR
                            }
                        AiCandidateHint(
                            title = item.title.trim().take(MAX_TITLE_LENGTH),
                            expectedYear = validYear,
                            reason = item.reason.trim().take(MAX_REASON_LENGTH),
                        )
                    }

            return AiSearchInterpretation(
                intentSummary = dto.intentSummary.trim().take(MAX_SUMMARY_LENGTH),
                explanation = dto.explanation.trim().take(MAX_EXPLANATION_LENGTH),
                candidates = domainCandidates,
            )
        }

        private fun handleHttpError(code: Int): Result<AiSearchInterpretation> {
            AppLog.network.w { "Gemini search request failed with HTTP $code" }
            return when (code) {
                HTTP_BAD_REQUEST, HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> {
                    Result.failure(AiException.InvalidApiKey())
                }
                HTTP_TOO_MANY_REQUESTS -> Result.failure(AiException.QuotaExceeded())
                else ->
                    Result.failure(
                        AiException.NetworkError("Gemini server error (HTTP $code)"),
                    )
            }
        }

        private suspend fun <T> executeWithRetry(block: suspend () -> Response<T>): Response<T> {
            var delayMs = RETRY_DELAY_MS
            var lastException: IOException? = null
            for (attempt in 1..MAX_RETRY_ATTEMPTS) {
                try {
                    val response = block()
                    val code = response.code()
                    val isRetryable =
                        code == HTTP_TOO_MANY_REQUESTS || code in HTTP_SERVER_ERROR_RANGE
                    if (response.isSuccessful || !isRetryable || attempt == MAX_RETRY_ATTEMPTS) {
                        return response
                    }
                    AppLog.network.w {
                        "Gemini call returned $code (attempt $attempt/$MAX_RETRY_ATTEMPTS), " +
                            "retrying in ${delayMs}ms"
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (ioe: IOException) {
                    lastException = ioe
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        break
                    }
                    AppLog.network.w {
                        "Gemini request failed with ${ioe.javaClass.simpleName} " +
                            "(attempt $attempt/$MAX_RETRY_ATTEMPTS), retrying in ${delayMs}ms"
                    }
                }
                delay(delayMs.milliseconds)
                delayMs *= 2
            }
            throw lastException ?: IOException("Request failed after $MAX_RETRY_ATTEMPTS attempts")
        }

        companion object {
            private const val HTTP_BAD_REQUEST = 400
            private const val HTTP_UNAUTHORIZED = 401
            private const val HTTP_FORBIDDEN = 403
            private const val HTTP_TOO_MANY_REQUESTS = 429
            private val HTTP_SERVER_ERROR_RANGE = 500..599
            private const val MAX_RETRY_ATTEMPTS = 3
            private const val RETRY_DELAY_MS = 1000L
            private const val FINISH_REASON_SAFETY = "SAFETY"
            private const val MAX_RESPONSE_LENGTH = 16_384
            private const val MAX_TITLE_LENGTH = 150
            private const val MAX_REASON_LENGTH = 300
            private const val MAX_SUMMARY_LENGTH = 150
            private const val MAX_EXPLANATION_LENGTH = 500
            private const val MIN_VALID_YEAR = 1917
            private const val MAX_VALID_YEAR = 2035
        }
    }
