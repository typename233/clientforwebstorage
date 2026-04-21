package com.example.clientforwebstorage.network.models

data class RevokeShareResponse(
    val id: String,
    val revoked: Boolean,
    val alreadyRevoked: Boolean
)
