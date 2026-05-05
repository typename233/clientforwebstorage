package com.example.clientforwebstorage.network.models

data class UpdateShareRequest(
    val allowPreview: Boolean? = null,
    val allowDownload: Boolean? = null,
    val needLogin: Boolean? = null,
    val allowSaveToMine: Boolean? = null,
    val maxAccessCount: Int? = null,
    val expiredAt: String? = null,
    val accessScope: Any? = null
)