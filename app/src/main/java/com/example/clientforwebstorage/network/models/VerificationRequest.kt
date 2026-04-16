package com.example.clientforwebstorage.network.models

data class VerificationRequest(
    val channel: String = "email",
    val email: String
)