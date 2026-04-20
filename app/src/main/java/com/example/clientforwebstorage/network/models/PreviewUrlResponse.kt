package com.example.clientforwebstorage.network.models

data class PreviewUrlResponse(
    val url: String,
    val expiresAt: String?,
    val ttlSeconds: Long?,
    val resourceId: String?,
    val filename: String?,
    val size: Long?,
    val mimeType: String?,
    val mode: String?
)
