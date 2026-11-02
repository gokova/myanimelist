package com.gokova.myanimelist.core.ai.model

sealed class AiException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class MissingApiKey(
        message: String = "AI API key is missing",
    ) : AiException(message)

    class InvalidApiKey(
        message: String = "Invalid or expired AI API key",
    ) : AiException(message)

    class QuotaExceeded(
        message: String = "AI quota exceeded or rate limit reached",
    ) : AiException(message)

    class SafetyBlocked(
        message: String = "Prompt or response was blocked by safety policy",
    ) : AiException(message)

    class InvalidResponse(
        message: String = "Received invalid or unparseable response from AI",
        cause: Throwable? = null,
    ) : AiException(message, cause)

    class NetworkError(
        message: String,
        cause: Throwable? = null,
    ) : AiException(message, cause)
}
