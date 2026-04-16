package com.example.clientforwebstorage.network.models

data class LoginRequest(
    val email: String,
    val password: String,
    val clientType: String = "android",
    val deviceId: String = "android-123"
)