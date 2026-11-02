package com.gokova.myanimelist.core.ai.model

data class AiCandidateHint(
    val title: String,
    val expectedYear: Int? = null,
    val reason: String,
)
