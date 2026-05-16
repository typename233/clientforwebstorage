package com.example.clientforwebstorage.ui.files

import android.content.Context
import android.content.SharedPreferences

object FavoritesManager {

    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorite_ids"

    private lateinit var prefs: SharedPreferences
    private var favoriteIds: MutableSet<String> = mutableSetOf()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        favoriteIds = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    fun isFavorite(resourceId: String): Boolean {
        return favoriteIds.contains(resourceId)
    }

    fun addFavorite(resourceId: String) {
        favoriteIds.add(resourceId)
        saveFavorites()
    }

    fun removeFavorite(resourceId: String) {
        favoriteIds.remove(resourceId)
        saveFavorites()
    }

    fun getAllFavoriteIds(): Set<String> {
        return favoriteIds.toSet()
    }

    fun toggleFavorite(resourceId: String): Boolean {
        return if (isFavorite(resourceId)) {
            removeFavorite(resourceId)
            false
        } else {
            addFavorite(resourceId)
            true
        }
    }

    private fun saveFavorites() {
        prefs.edit().putStringSet(KEY_FAVORITES, favoriteIds).apply()
    }

    fun clearAll() {
        favoriteIds.clear()
        saveFavorites()
    }
}
