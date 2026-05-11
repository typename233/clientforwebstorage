package com.example.clientforwebstorage.network.models

data class InviteListData(
    val total: Int = 0,
    val items: List<InviteData> = emptyList()
)
