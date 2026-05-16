package com.example.clientforwebstorage.network.models

data class MemberListResponse(
    val total: Int,
    val items: List<MemberInfo>
)
