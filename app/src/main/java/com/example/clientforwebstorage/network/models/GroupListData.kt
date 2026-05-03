package com.example.clientforwebstorage.network.models

data class GroupListData(
    val items: List<GroupData> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20
)
