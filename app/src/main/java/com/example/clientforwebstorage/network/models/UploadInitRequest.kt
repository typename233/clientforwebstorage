package com.example.clientforwebstorage.network.models

data class UploadInitRequest(
    val parentId: String?,
    val filename: String,
    val size: Long,
    val sha256: String?,
    val partSize: Long
)
