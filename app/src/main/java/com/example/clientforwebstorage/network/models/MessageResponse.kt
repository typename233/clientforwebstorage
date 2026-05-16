package com.example.clientforwebstorage.network.models

data class MessageResponse(
    val id: String,
    val messageType: String,
    val contentText: String? = null,
    val content: Map<String, Any?>? = null,
    val status: String? = null,
    val clientMessageId: String? = null,
    val replyToMessageId: Int? = null,
    val resourceId: Int? = null,
    val editedAt: String? = null,
    val createdAt: String,
    val sender: Map<String, Any?>
)
