package com.example.clientforwebstorage.network.models

import com.google.gson.annotations.SerializedName

data class Resource(
    val id: String,
    @SerializedName("parentId")
    val parentId: String?,
    val type: String,
    val name: String,
    val size: Long,
    val extension: String?,
    val updatedAt: String
)
