package com.example.clientforwebstorage.network.models

data class CreateConversationRequest(
    val conversationType: String,
    val memberUserIds: List<String>,
    val name: String? = null,
    val avatarUrl: String? = null,
    val spaceId: String? = null
)
