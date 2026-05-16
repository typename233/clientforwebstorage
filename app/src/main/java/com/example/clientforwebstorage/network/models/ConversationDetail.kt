package com.example.clientforwebstorage.network.models

data class ConversationDetail(
    val id: String,
    val conversationType: String,
    val name: String,
    val avatarUrl: String? = null,
    val status: String? = null,
    val spaceId: Int? = null,
    val ownerUserId: String? = null,
    val isMuted: Boolean = false,
    val muteUntil: String? = null,
    val lastMessageAt: String? = null,
    val members: List<MemberInfo> = emptyList()
)
