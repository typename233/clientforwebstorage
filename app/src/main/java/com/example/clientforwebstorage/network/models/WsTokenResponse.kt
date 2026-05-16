package com.example.clientforwebstorage.network.models

data class WsTokenResponse(
    val token: String,
    val expiresIn: Int
)
