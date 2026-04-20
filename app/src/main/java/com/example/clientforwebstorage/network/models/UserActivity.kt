package com.example.clientforwebstorage.network.models

data class UserActivity(
    val eventId: String,
    val actorType: String,
    val eventType: String,
    val targetType: String,
    val targetId: String,
    val result: String,
    val reason: String?,
    val metadata: String,
    val createdAt: String
)
