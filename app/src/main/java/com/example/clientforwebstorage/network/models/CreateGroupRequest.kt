package com.example.clientforwebstorage.network.models

data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val visibility: String = "private",
    val quotaBytes: Long? = null
)
