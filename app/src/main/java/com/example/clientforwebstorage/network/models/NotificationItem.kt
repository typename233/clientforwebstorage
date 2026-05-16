package com.example.clientforwebstorage.network.models

data class NotificationItem(
    val notificationId: String,
    val title: String,
    val content: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String,
    val data: Map<String, Any?>? = null
)
