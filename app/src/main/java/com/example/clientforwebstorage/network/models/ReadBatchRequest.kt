package com.example.clientforwebstorage.network.models

data class ReadBatchRequest(
    val conversationId: String,
    val messageIds: List<String>
)
