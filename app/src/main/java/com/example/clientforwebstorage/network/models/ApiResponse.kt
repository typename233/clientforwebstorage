package com.example.clientforwebstorage.network.models

data class ApiResponse(
    val code: Int,
    val message: String,
    val data: Any?
)