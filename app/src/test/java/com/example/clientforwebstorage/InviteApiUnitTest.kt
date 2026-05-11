package com.example.clientforwebstorage

import com.example.clientforwebstorage.network.models.InviteData
import com.example.clientforwebstorage.network.models.InviteListData
import com.example.clientforwebstorage.ui.groups.InviteDataCache
import com.example.clientforwebstorage.ui.groups.RecordStatus
import com.example.clientforwebstorage.ui.groups.RecordType
import com.example.clientforwebstorage.ui.groups.UnifiedRecordItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test

class InviteApiUnitTest {

    private val gson = Gson()

    @Test
    fun recordType_displayNames() {
        assertEquals("收到邀请", RecordType.RECEIVED_INVITE.displayName)
        assertEquals("发出邀请", RecordType.SENT_INVITE.displayName)
        assertEquals("入群申请", RecordType.JOIN_REQUEST.displayName)
    }

    @Test
    fun recordType_badgeColors() {
        assertTrue(RecordType.RECEIVED_INVITE.badgeColor != 0)
        assertTrue(RecordType.SENT_INVITE.badgeColor != 0)
        assertTrue(RecordType.JOIN_REQUEST.badgeColor != 0)
        assertNotEquals(RecordType.RECEIVED_INVITE.badgeColor, RecordType.SENT_INVITE.badgeColor)
    }

    @Test
    fun recordStatus_fromApiStatus() {
        assertEquals(RecordStatus.PENDING, RecordStatus.fromApiStatus("pending"))
        assertEquals(RecordStatus.ACCEPTED, RecordStatus.fromApiStatus("accepted"))
        assertEquals(RecordStatus.REJECTED, RecordStatus.fromApiStatus("rejected"))
        assertEquals(RecordStatus.APPROVED, RecordStatus.fromApiStatus("approved"))
        assertEquals(RecordStatus.EXPIRED, RecordStatus.fromApiStatus("expired"))
        assertEquals(RecordStatus.CANCELLED, RecordStatus.fromApiStatus("cancelled"))
        assertEquals(RecordStatus.UNKNOWN, RecordStatus.fromApiStatus("unknown_status"))
        assertEquals(RecordStatus.UNKNOWN, RecordStatus.fromApiStatus(""))
    }

    @Test
    fun recordStatus_caseInsensitive() {
        assertEquals(RecordStatus.PENDING, RecordStatus.fromApiStatus("PENDING"))
        assertEquals(RecordStatus.ACCEPTED, RecordStatus.fromApiStatus("Accepted"))
        assertEquals(RecordStatus.CANCELLED, RecordStatus.fromApiStatus("CANCELLED"))
    }

    @Test
    fun unifiedRecordItem_receivedInvite() {
        val item = UnifiedRecordItem(
            id = "inv_001",
            type = RecordType.RECEIVED_INVITE,
            title = "来自 user_002 的邀请",
            groupId = "grp_001",
            groupName = "项目组",
            role = "editor",
            status = RecordStatus.PENDING,
            rawStatus = "pending",
            createdAt = "2026-05-11 10:30:00",
            expiredAt = "2026-06-11 23:59:59",
            inviterUserId = "user_002"
        )
        assertTrue(item.isPendingActionable)
        assertFalse(item.isJoinRequestPending)
        assertEquals("inv_001", item.id)
        assertEquals("editor", item.role)
    }

    @Test
    fun unifiedRecordItem_sentInvite() {
        val item = UnifiedRecordItem(
            id = "sent_001",
            type = RecordType.SENT_INVITE,
            title = "user@example.com",
            groupId = "grp_001",
            groupName = "测试组",
            role = "viewer",
            status = RecordStatus.PENDING,
            rawStatus = "pending",
            createdAt = "2026-05-10 14:00:00",
            actionLabel = "撤销"
        )
        assertTrue(item.isPendingActionable)
        assertFalse(item.isJoinRequestPending)
        assertEquals("撤销", item.actionLabel)
    }

    @Test
    fun unifiedRecordItem_joinRequest() {
        val item = UnifiedRecordItem(
            id = "jr_001",
            type = RecordType.JOIN_REQUEST,
            title = "申请加入「开发组」",
            groupId = "grp_dev",
            groupName = "开发组",
            role = null,
            status = RecordStatus.PENDING,
            rawStatus = "pending",
            createdAt = "2026-05-09 08:00:00"
        )
        assertFalse(item.isPendingActionable)
        assertTrue(item.isJoinRequestPending)
        assertNull(item.role)
    }

    @Test
    fun unifiedRecordItem_extraData() {
        val item = UnifiedRecordItem(
            id = "test_extra",
            type = RecordType.RECEIVED_INVITE,
            title = "测试",
            status = RecordStatus.PENDING,
            rawStatus = "pending",
            createdAt = "2026-01-01 00:00:00",
            extraData = mapOf(
                UnifiedRecordItem.EXTRA_GROUP_ID to "g1",
                UnifiedRecordItem.EXTRA_INVITE_ID to "i1",
                UnifiedRecordItem.EXTRA_REQUEST_ID to "r1",
                UnifiedRecordItem.EXTRA_INVITER_USER_ID to "u1"
            )
        )
        assertEquals("g1", item.extraData[UnifiedRecordItem.EXTRA_GROUP_ID])
        assertEquals("i1", item.extraData[UnifiedRecordItem.EXTRA_INVITE_ID])
        assertEquals("r1", item.extraData[UnifiedRecordItem.EXTRA_REQUEST_ID])
        assertEquals("u1", item.extraData[UnifiedRecordItem.EXTRA_INVITER_USER_ID])
    }

    @Test
    fun unifiedRecordItem_dataClassCopy() {
        val original = UnifiedRecordItem(
            id = "copy_test",
            type = RecordType.RECEIVED_INVITE,
            title = "原始标题",
            status = RecordStatus.PENDING,
            rawStatus = "pending",
            createdAt = "2026-01-01 00:00:00"
        )
        val updated = original.copy(status = RecordStatus.ACCEPTED, rawStatus = "accepted")
        assertEquals(RecordStatus.ACCEPTED, updated.status)
        assertEquals(RecordStatus.PENDING, original.status)
    }

    @Test
    fun inviteDataCache_setAndGet() {
        InviteDataCache.clearAll()

        val receivedItems = listOf(
            UnifiedRecordItem("r1", RecordType.RECEIVED_INVITE, "收到1", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-05-11 10:00:00"),
            UnifiedRecordItem("r2", RecordType.RECEIVED_INVITE, "收到2", status = RecordStatus.ACCEPTED, rawStatus = "accepted", createdAt = "2026-05-10 10:00:00")
        )
        val sentItems = listOf(
            UnifiedRecordItem("s1", RecordType.SENT_INVITE, "发出1", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-05-11 12:00:00")
        )
        val joinItems = listOf(
            UnifiedRecordItem("j1", RecordType.JOIN_REQUEST, "申请1", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-05-11 08:00:00")
        )

        InviteDataCache.setReceivedInvites(receivedItems, 2)
        InviteDataCache.setSentInvites(sentItems, 1)
        InviteDataCache.setJoinRequests(joinItems, 1)

        assertEquals(2, InviteDataCache.getReceivedInvites().size)
        assertEquals(1, InviteDataCache.getSentInvites().size)
        assertEquals(1, InviteDataCache.getJoinRequests().size)

        InviteDataCache.clearAll()
        assertTrue(InviteDataCache.isReceivedEmpty())
        assertTrue(InviteDataCache.isSentEmpty())
        assertTrue(InviteDataCache.isJoinRequestsEmpty())
    }

    @Test
    fun inviteDataCache_getAllMerged_sortedByTimeDesc() {
        InviteDataCache.clearAll()

        InviteDataCache.setReceivedInvites(listOf(
            UnifiedRecordItem("r_old", RecordType.RECEIVED_INVITE, "旧", status = RecordStatus.ACCEPTED, rawStatus = "accepted", createdAt = "2026-01-01 00:00:00"),
            UnifiedRecordItem("r_new", RecordType.RECEIVED_INVITE, "新", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-12-31 23:59:59")
        ), 2)
        InviteDataCache.setSentInvites(listOf(
            UnifiedRecordItem("s_mid", RecordType.SENT_INVITE, "中", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-06-15 10:00:00")
        ), 1)
        InviteDataCache.setJoinRequests(listOf(
            UnifiedRecordItem("j_recent", RecordType.JOIN_REQUEST, "最近", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-11-20 15:00:00")
        ), 1)

        val merged = InviteDataCache.getAllMerged()
        assertEquals(4, merged.size)
        assertEquals("r_new", merged[0].id)
        assertEquals("j_recent", merged[1].id)
        assertEquals("s_mid", merged[2].id)
        assertEquals("r_old", merged[3].id)

        InviteDataCache.clearAll()
    }

    @Test
    fun inviteDataCache_getPendingRecords() {
        InviteDataCache.clearAll()

        InviteDataCache.setReceivedInvites(listOf(
            UnifiedRecordItem("p1", RecordType.RECEIVED_INVITE, "待处理1", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-05-11 10:00:00"),
            UnifiedRecordItem("a1", RecordType.RECEIVED_INVITE, "已接受", status = RecordStatus.ACCEPTED, rawStatus = "accepted", createdAt = "2026-05-10 10:00:00")
        ), 2)
        InviteDataCache.setSentInvites(listOf(
            UnifiedRecordItem("p2", RecordType.SENT_INVITE, "待处理2", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-05-11 12:00:00"),
            UnifiedRecordItem("c1", RecordType.SENT_INVITE, "已撤销", status = RecordStatus.CANCELLED, rawStatus = "cancelled", createdAt = "2026-05-09 10:00:00")
        ), 2)
        InviteDataCache.setJoinRequests(listOf(
            UnifiedRecordItem("p3", RecordType.JOIN_REQUEST, "待审批", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-05-11 08:00:00")
        ), 1)

        val pending = InviteDataCache.getPendingRecords()
        assertEquals(3, pending.size)
        assertTrue(pending.all { it.status == RecordStatus.PENDING })

        InviteDataCache.clearAll()
    }

    @Test
    fun inviteDataCache_updateRecordStatus() {
        InviteDataCache.clearAll()

        InviteDataCache.setReceivedInvites(listOf(
            UnifiedRecordItem("target", RecordType.RECEIVED_INVITE, "目标", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-05-11 10:00:00")
        ), 1)

        val result = InviteDataCache.updateRecordStatus("target", RecordStatus.ACCEPTED)
        assertTrue(result)

        val updated = InviteDataCache.getReceivedInvites()[0]
        assertEquals(RecordStatus.ACCEPTED, updated.status)
        assertEquals("accepted", updated.rawStatus)

        val notFoundResult = InviteDataCache.updateRecordStatus("non_existent", RecordStatus.REJECTED)
        assertFalse(notFoundResult)

        InviteDataCache.clearAll()
    }

    @Test
    fun inviteData_cacheExpiry() {
        InviteDataCache.clearAll()
        assertTrue(InviteDataCache.isReceivedExpired())

        InviteDataCache.setReceivedInvites(listOf(
            UnifiedRecordItem("fresh", RecordType.RECEIVED_INVITE, "新鲜", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-05-11 10:00:00")
        ), 1)
        assertFalse(InviteDataCache.isReceivedExpired())
        assertTrue(InviteDataCache.isSentExpired())
        assertTrue(InviteDataCache.isJoinRequestsExpired())

        InviteDataCache.clearAll()
    }

    @Test
    fun inviteData_jsonSerialization() {
        val original = InviteListData(
            total = 42,
            items = listOf(
                InviteData(
                    inviteId = "inv_ser_001",
                    groupId = "grp_ser_001",
                    groupName = "序列化测试",
                    inviterUserId = "user_ser_001",
                    role = "owner",
                    status = "pending",
                    expiredAt = "2099-12-31 23:59:59",
                    createdAt = "2026-05-11 09:00:00"
                )
            )
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, InviteListData::class.java)

        assertEquals(original.total, deserialized.total)
        assertEquals(1, deserialized.items.size)
        assertEquals("inv_ser_001", deserialized.items[0].inviteId)
        assertEquals("owner", deserialized.items[0].role)
        assertEquals("pending", deserialized.items[0].status)
    }

    @Test
    fun inviteData_threeTypesMixedSorting() {
        val records = listOf(
            UnifiedRecordItem("a", RecordType.RECEIVED_INVITE, "A", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-03-01 00:00:00"),
            UnifiedRecordItem("b", RecordType.SENT_INVITE, "B", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-06-15 12:00:00"),
            UnifiedRecordItem("c", RecordType.JOIN_REQUEST, "C", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-01-01 00:00:00"),
            UnifiedRecordItem("d", RecordType.RECEIVED_INVITE, "D", status = RecordStatus.ACCEPTED, rawStatus = "accepted", createdAt = "2026-12-31 23:59:59"),
            UnifiedRecordItem("e", RecordType.SENT_INVITE, "E", status = RecordStatus.PENDING, rawStatus = "pending", createdAt = "2026-09-01 08:00:00")
        )

        val sorted = records.sortedByDescending { it.createdAt }
        assertEquals("d", sorted[0].id)
        assertEquals("e", sorted[1].id)
        assertEquals("b", sorted[2].id)
        assertEquals("a", sorted[3].id)
        assertEquals("c", sorted[4].id)
    }
}
