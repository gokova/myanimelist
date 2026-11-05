package com.gokova.myanimelist.core.ai.model

data class AiSearchInterpretation(
    val intentSummary: String,
    val explanation: String,
    val candidates: List<AiCandidateHint>,
)
