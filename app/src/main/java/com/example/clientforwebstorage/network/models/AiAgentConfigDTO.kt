package com.example.clientforwebstorage.network.models

data class AiAgentConfigDTO(
    val modelName: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val systemPrompt: String? = null,
    val enabled: Boolean? = null
)
