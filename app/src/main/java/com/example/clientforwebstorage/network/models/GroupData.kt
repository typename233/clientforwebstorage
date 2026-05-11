package com.example.clientforwebstorage.network.models

data class GroupData(
    val id: String,
    val name: String,
    val description: String? = null,
    val visibility: String = "private",
    val role: String = "owner",
    val memberCount: Int = 0,
    val storageUsed: Long = 0L,
    val storageQuota: Long? = null,
    val avatarUrl: String? = null,
    val createdAt: String? = null,
    val isPinned: Boolean? = null,
    val isMuted: Boolean? = null
)
