package com.example.clientforwebstorage.network

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR_URL = "avatar_url"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveTokens(accessToken: String, refreshToken: String?) {
        prefs?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) {
                putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            apply()
        }
    }

    fun saveUserProfile(nickname: String?, avatarUrl: String?) {
        prefs?.edit()?.apply {
            if (nickname != null) putString(KEY_NICKNAME, nickname)
            if (avatarUrl != null) putString(KEY_AVATAR_URL, avatarUrl)
            apply()
        }
    }

    fun getNickname(): String? {
        return prefs?.getString(KEY_NICKNAME, null)
    }

    fun getAvatarUrl(): String? {
        return prefs?.getString(KEY_AVATAR_URL, null)
    }

    fun getAccessToken(): String? {
        return prefs?.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs?.getString(KEY_REFRESH_TOKEN, null)
    }

    fun clearTokens() {
        prefs?.edit()?.clear()?.apply()
    }

    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }
}
