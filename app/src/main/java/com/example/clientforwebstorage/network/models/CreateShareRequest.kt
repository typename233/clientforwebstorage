package com.example.clientforwebstorage.network.models

data class CreateShareRequest(
    val resourceIds: List<String>,
    val expiredAt: String?,
    val needCode: Boolean,
    val code: String?,
    val allowPreview: Boolean,
    val allowDownload: Boolean,
    val maxAccessCount: Int?
)
