package com.example.clientforwebstorage.network.models

data class MemberInfo(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val role: String,
    val joinedAt: String? = null
)
