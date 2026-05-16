package com.example.clientforwebstorage.network.models

data class ConversationListResponse(
    val total: Int,
    val items: List<ConversationSummary>
)
