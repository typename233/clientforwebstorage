package com.example.clientforwebstorage.network.models

data class SendMessageRequest(
    val messageType: String = "text",
    val contentText: String? = null,
    val content: Map<String, Any?>? = null,
    val replyToMessageId: String? = null,
    val resourceId: Int? = null,
    val clientMessageId: String? = null
)
