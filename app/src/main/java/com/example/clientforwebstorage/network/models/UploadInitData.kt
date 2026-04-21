package com.example.clientforwebstorage.network.models

data class UploadInitData(
    val uploadId: String,
    val uploadUrl: String?,
    val partSize: Long?
)