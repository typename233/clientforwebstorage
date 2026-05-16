package com.example.clientforwebstorage.network.models

data class UnreadCountResponse(
    val conversationId: String,
    val unreadCount: Int
)
