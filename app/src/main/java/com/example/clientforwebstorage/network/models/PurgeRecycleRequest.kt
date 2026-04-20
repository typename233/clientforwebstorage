package com.example.clientforwebstorage.network.models

data class PurgeRecycleRequest(
    val purgeAll: Boolean,
    val resourceIds: List<String>?
)
