package com.example.clientforwebstorage.network.models

data class CreateTagRequest(
    val name: String,
    val color: String? = null
)