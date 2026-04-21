package com.example.clientforwebstorage.network.models

data class Share(
    val id: String,
    val shareCode: String,
    val title: String?,
    val status: String?,
    val resourceIds: List<String>?,
    val resourceCount: Int?,
    val expiredAt: String?,
    val needCode: Boolean,
    val code: String?,
    val allowPreview: Boolean,
    val allowDownload: Boolean,
    val maxAccessCount: Int?,
    val currentAccessCount: Int,
    val createdAt: String,
    val createdBy: String?,
    val revoked: Boolean? = false,
    val alreadyRevoked: Boolean? = false
)
