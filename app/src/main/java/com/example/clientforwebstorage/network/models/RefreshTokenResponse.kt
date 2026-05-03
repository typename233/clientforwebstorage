package com.example.clientforwebstorage.network.models

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Int?
)
