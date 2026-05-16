package com.example.clientforwebstorage.network.models

data class MessageListResponse(
    val items: List<MessageResponse>,
    val nextCursor: String? = null
)
