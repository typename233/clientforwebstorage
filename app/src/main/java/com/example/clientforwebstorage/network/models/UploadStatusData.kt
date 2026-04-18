package com.example.clientforwebstorage.network.models

data class UploadStatusData(
    val uploadId: String,
    val status: String,
    val uploadedParts: List<Int>?
)
