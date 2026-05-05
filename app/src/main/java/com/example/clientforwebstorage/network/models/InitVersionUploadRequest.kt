package com.example.clientforwebstorage.network.models

data class InitVersionUploadRequest(
    val filename: String,
    val totalSize: Long,
    val totalParts: Int,
    val sha256: String? = null
)