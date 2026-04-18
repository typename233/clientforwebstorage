package com.example.clientforwebstorage.network.models

data class UploadCompleteRequest(
    val parts: List<CompletedPart>
)
