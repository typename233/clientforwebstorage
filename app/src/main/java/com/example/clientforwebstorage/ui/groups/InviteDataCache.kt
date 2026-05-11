package com.example.clientforwebstorage.ui.groups

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InviteDataCache {

    private val receivedInvitesCache = mutableListOf<UnifiedRecordItem>()
    private val sentInvitesCache = mutableListOf<UnifiedRecordItem>()
    private val joinRequestsCache = mutableListOf<UnifiedRecordItem>()

    private var receivedInvitesTotal = 0
    private var sentInvitesTotal = 0
    private var joinRequestsTotal = 0

    private var receivedLastFetchTime: Long = 0
    private var sentLastFetchTime: Long = 0
    private var joinRequestsLastFetchTime: Long = 0

    private const val CACHE_TTL_MS = 60_000L

    fun setReceivedInvites(items: List<UnifiedRecordItem>, total: Int) {
        synchronized(receivedInvitesCache) {
            receivedInvitesCache.clear()
            receivedInvitesCache.addAll(items)
            receivedInvitesTotal = total
            receivedLastFetchTime = System.currentTimeMillis()
        }
    }

    fun setSentInvites(items: List<UnifiedRecordItem>, total: Int) {
        synchronized(sentInvitesCache) {
            sentInvitesCache.clear()
            sentInvitesCache.addAll(items)
            sentInvitesTotal = total
            sentLastFetchTime = System.currentTimeMillis()
        }
    }

    fun setJoinRequests(items: List<UnifiedRecordItem>, total: Int) {
        synchronized(joinRequestsCache) {
            joinRequestsCache.clear()
            joinRequestsCache.addAll(items)
            joinRequestsTotal = total
            joinRequestsLastFetchTime = System.currentTimeMillis()
        }
    }

    fun getReceivedInvites(): List<UnifiedRecordItem> = synchronized(receivedInvitesCache) { receivedInvitesCache.toList() }
    fun getSentInvites(): List<UnifiedRecordItem> = synchronized(sentInvitesCache) { sentInvitesCache.toList() }
    fun getJoinRequests(): List<UnifiedRecordItem> = synchronized(joinRequestsCache) { joinRequestsCache.toList() }

    fun getAllMerged(): List<UnifiedRecordItem> {
        return (getReceivedInvites() + getSentInvites() + getJoinRequests())
            .sortedByDescending { it.createdAt }
    }

    fun getPendingRecords(): List<UnifiedRecordItem> {
        return getAllMerged().filter { it.status == RecordStatus.PENDING }
    }

    fun isReceivedExpired(): Boolean = System.currentTimeMillis() - receivedLastFetchTime > CACHE_TTL_MS
    fun isSentExpired(): Boolean = System.currentTimeMillis() - sentLastFetchTime > CACHE_TTL_MS
    fun isJoinRequestsExpired(): Boolean = System.currentTimeMillis() - joinRequestsLastFetchTime > CACHE_TTL_MS

    fun isReceivedEmpty(): Boolean = synchronized(receivedInvitesCache) { receivedInvitesCache.isEmpty() }
    fun isSentEmpty(): Boolean = synchronized(sentInvitesCache) { sentInvitesCache.isEmpty() }
    fun isJoinRequestsEmpty(): Boolean = synchronized(joinRequestsCache) { joinRequestsCache.isEmpty() }

    fun updateRecordStatus(id: String, newStatus: RecordStatus): Boolean {
        var found = false
        synchronized(receivedInvitesCache) {
            val idx = receivedInvitesCache.indexOfFirst { it.id == id }
            if (idx >= 0) {
                receivedInvitesCache[idx] = receivedInvitesCache[idx].copy(status = newStatus, rawStatus = newStatus.name.lowercase())
                found = true
            }
        }
        if (!found) {
            synchronized(sentInvitesCache) {
                val idx = sentInvitesCache.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    sentInvitesCache[idx] = sentInvitesCache[idx].copy(status = newStatus, rawStatus = newStatus.name.lowercase())
                    found = true
                }
            }
        }
        if (!found) {
            synchronized(joinRequestsCache) {
                val idx = joinRequestsCache.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    joinRequestsCache[idx] = joinRequestsCache[idx].copy(status = newStatus, rawStatus = newStatus.name.lowercase())
                    found = true
                }
            }
        }
        return found
    }

    fun clearAll() {
        synchronized(receivedInvitesCache) { receivedInvitesCache.clear(); receivedLastFetchTime = 0 }
        synchronized(sentInvitesCache) { sentInvitesCache.clear(); sentLastFetchTime = 0 }
        synchronized(joinRequestsCache) { joinRequestsCache.clear(); joinRequestsLastFetchTime = 0 }
    }
}
