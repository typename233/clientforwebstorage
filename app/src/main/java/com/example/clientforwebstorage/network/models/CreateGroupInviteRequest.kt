package com.example.clientforwebstorage.network.models

data class CreateGroupInviteRequest(
    val inviteeUserId: String? = null,
    val inviteeEmail: String? = null,
    val role: String,
    val expireHours: Int = 72
)