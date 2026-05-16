package com.example.clientforwebstorage.network.models

data class AddMembersRequest(
    val userIds: List<String>? = null,
    val emails: List<String>? = null,
    val phones: List<String>? = null,
    val role: String? = null
)