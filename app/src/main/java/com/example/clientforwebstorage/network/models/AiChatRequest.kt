package com.example.clientforwebstorage.network.models

data class AiChatRequest(
    val message: String,
    val conversationId: String? = null,
    val useHistory: Boolean = true
)
