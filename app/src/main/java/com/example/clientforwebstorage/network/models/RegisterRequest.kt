package com.example.clientforwebstorage.network.models

data class RegisterRequest(
    val email: String,
    val verificationCode: String,
    val password: String,
    val nickname: String
)