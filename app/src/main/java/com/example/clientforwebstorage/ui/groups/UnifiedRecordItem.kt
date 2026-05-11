package com.example.clientforwebstorage.ui.groups

enum class RecordType(val displayName: String, val badgeColor: Int) {
    RECEIVED_INVITE("收到邀请", 0xFF1976D2.toInt()),
    SENT_INVITE("发出邀请", 0xFF7B1FA2.toInt()),
    JOIN_REQUEST("入群申请", 0xFF388E3C.toInt())
}

enum class RecordStatus(val displayText: String, val color: Int) {
    PENDING("待处理", 0xFF1976D2.toInt()),
    ACCEPTED("已接受", 0xFF4CAF50.toInt()),
    REJECTED("已拒绝", 0xFF9E9E9E.toInt()),
    APPROVED("已通过", 0xFF4CAF50.toInt()),
    EXPIRED("已过期", 0xFF9E9E9E.toInt()),
    CANCELLED("已撤销", 0xFF9E9E9E.toInt()),
    UNKNOWN("未知", 0xFF666666.toInt());

    companion object {
        fun fromApiStatus(status: String): RecordStatus = when (status.lowercase()) {
            "pending" -> PENDING
            "accepted" -> ACCEPTED
            "rejected" -> REJECTED
            "approved" -> APPROVED
            "expired" -> EXPIRED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

data class UnifiedRecordItem(
    val id: String,
    val type: RecordType,
    val title: String,
    val groupId: String?,
    val groupName: String?,
    val role: String?,
    val status: RecordStatus,
    val rawStatus: String,
    val createdAt: String,
    val expiredAt: String? = null,
    val inviterUserId: String? = null,
    val actionLabel: String? = null,
    val extraData: Map<String, String> = emptyMap()
) {
    val isPendingActionable: Boolean
        get() = status == RecordStatus.PENDING && type != RecordType.JOIN_REQUEST

    val isJoinRequestPending: Boolean
        get() = status == RecordStatus.PENDING && type == RecordType.JOIN_REQUEST

    companion object {
        const val EXTRA_GROUP_ID = "groupId"
        const val EXTRA_INVITE_ID = "inviteId"
        const val EXTRA_REQUEST_ID = "requestId"
        const val EXTRA_INVITER_USER_ID = "inviterUserId"
    }
}
