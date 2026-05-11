package com.example.clientforwebstorage.network.models

data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null,
    val visibility: String? = null,
    val quotaBytes: Long? = null,
    val maxMembers: Int? = null,
    val avatarUrl: String? = null,
    val isPinned: Boolean? = null,
    val isMuted: Boolean? = null
)