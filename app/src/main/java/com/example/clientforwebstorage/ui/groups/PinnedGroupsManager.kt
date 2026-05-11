package com.example.clientforwebstorage.ui.groups

import android.content.Context
import android.content.SharedPreferences

class PinnedGroupsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "pinned_groups_prefs"
        private const val KEY_PINNED_SET = "pinned_group_ids"
        private const val KEY_PINNED_TIME = "pinned_time_"

        @Volatile
        private var instance: PinnedGroupsManager? = null

        fun getInstance(context: Context): PinnedGroupsManager {
            return instance ?: synchronized(this) {
                instance ?: PinnedGroupsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun isGroupPinned(groupId: String): Boolean {
        return getPinnedGroupIds().contains(groupId)
    }

    fun getPinnedGroupIds(): Set<String> {
        return prefs.getStringSet(KEY_PINNED_SET, emptySet()) ?: emptySet()
    }

    fun getPinnedGroupIdsOrdered(): List<String> {
        val pinnedIds = getPinnedGroupIds()
        return pinnedIds.sortedByDescending { groupId ->
            prefs.getLong(KEY_PINNED_TIME + groupId, 0L)
        }
    }

    fun pinGroup(groupId: String) {
        val currentPinned = getPinnedGroupIds().toMutableSet()
        currentPinned.add(groupId)
        
        prefs.edit()
            .putStringSet(KEY_PINNED_SET, currentPinned)
            .putLong(KEY_PINNED_TIME + groupId, System.currentTimeMillis())
            .apply()
    }

    fun unpinGroup(groupId: String) {
        val currentPinned = getPinnedGroupIds().toMutableSet()
        currentPinned.remove(groupId)
        
        prefs.edit()
            .putStringSet(KEY_PINNED_SET, currentPinned)
            .remove(KEY_PINNED_TIME + groupId)
            .apply()
    }

    fun togglePinGroup(groupId: String): Boolean {
        return if (isGroupPinned(groupId)) {
            unpinGroup(groupId)
            false
        } else {
            pinGroup(groupId)
            true
        }
    }

    fun getPinTime(groupId: String): Long {
        return prefs.getLong(KEY_PINNED_TIME + groupId, 0L)
    }

    fun clearAllPinned() {
        val pinnedIds = getPinnedGroupIds()
        val editor = prefs.edit()
            .remove(KEY_PINNED_SET)
        
        pinnedIds.forEach { groupId ->
            editor.remove(KEY_PINNED_TIME + groupId)
        }
        
        editor.apply()
    }

    fun getPinnedCount(): Int {
        return getPinnedGroupIds().size
    }

    fun syncWithServer(serverPinnedIds: Set<String>) {
        val localPinned = getPinnedGroupIds().toMutableSet()
        
        serverPinnedIds.forEach { groupId ->
            if (!localPinned.contains(groupId)) {
                localPinned.add(groupId)
            }
        }
        
        prefs.edit()
            .putStringSet(KEY_PINNED_SET, localPinned)
            .apply()
    }
}
