package com.gokova.myanimelist.core.ai.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiGenerateContentRequest(
    @SerialName("contents") val contents: List<GeminiContentDto>,
    @SerialName("system_instruction") val systemInstruction: GeminiContentDto? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfigDto? = null,
)

@Serializable
data class GeminiContentDto(
    @SerialName("role") val role: String? = null,
    @SerialName("parts") val parts: List<GeminiPartDto>,
)

@Serializable
data class GeminiPartDto(
    @SerialName("text") val text: String,
)

@Serializable
data class GeminiGenerationConfigDto(
    @SerialName("response_mime_type") val responseMimeType: String = "application/json",
    @SerialName("response_schema") val responseSchema: JsonObject? = null,
    @SerialName("max_output_tokens") val maxOutputTokens: Int = 1024,
    @SerialName("temperature") val temperature: Double? = null,
)

@Serializable
data class GeminiGenerateContentResponse(
    @SerialName("candidates") val candidates: List<GeminiCandidateDto>? = null,
    @SerialName("promptFeedback") val promptFeedback: GeminiPromptFeedbackDto? = null,
)

@Serializable
data class GeminiCandidateDto(
    @SerialName("content") val content: GeminiContentDto? = null,
    @SerialName("finishReason") val finishReason: String? = null,
)

@Serializable
data class GeminiPromptFeedbackDto(
    @SerialName("blockReason") val blockReason: String? = null,
)

@Serializable
data class GeminiStructuredInterpretationDto(
    @SerialName("intentSummary") val intentSummary: String,
    @SerialName("explanation") val explanation: String,
    @SerialName("candidates") val candidates: List<GeminiStructuredCandidateDto> = emptyList(),
)

@Serializable
data class GeminiStructuredCandidateDto(
    @SerialName("title") val title: String,
    @SerialName("expectedYear") val expectedYear: Int? = null,
    @SerialName("reason") val reason: String,
)
