package com.example.clientforwebstorage.network.models

data class LoginData(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Int?,
    val user: UserProfileData?
)
