package com.example.clientforwebstorage.network.models

data class ConversationSummary(
    val id: String,
    val conversationType: String,
    val name: String,
    val avatarUrl: String? = null,
    val status: String? = null,
    val lastMessageAt: String? = null,
    val isMuted: Boolean = false,
    val muteUntil: String? = null,
    val unreadCount: Int = 0,
    val lastMessage: Map<String, Any?>? = null,
    val role: String? = null,
    val memberCount: Int? = null
)
