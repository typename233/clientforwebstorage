package com.example.clientforwebstorage.ui.groups

data class GroupItem(
    val id: String,
    val name: String,
    val role: String,
    val memberCount: Int,
    val storageUsed: String,
    val visibility: String
)
