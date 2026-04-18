package com.example.clientforwebstorage.network.models

data class CreateFolderRequest(
    val parentId: String?,
    val name: String
)
