package com.example.clientforwebstorage.network.models

data class InviteData(
    val inviteId: String,
    val groupId: String,
    val groupName: String,
    val inviterUserId: String,
    val role: String,
    val status: String,
    val expiredAt: String,
    val createdAt: String
)
