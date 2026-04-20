package com.example.clientforwebstorage.network.models

import com.google.gson.annotations.SerializedName

data class RecycleResource(
    val id: String,
    val name: String,
    val type: String,
    val size: Long,
    val extension: String?,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("deletedAt")
    val deletedAt: String,
    @SerializedName("purgeAt")
    val purgeAt: String,
    @SerializedName("originalParentId")
    val originalParentId: String?
)
