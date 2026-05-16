package com.example.clientforwebstorage.network

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.RefreshTokenRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

object TokenManager {

    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR_URL = "avatar_url"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val REFRESH_INTERVAL_MS = 25 * 60 * 1000L

    private var prefs: SharedPreferences? = null
    private var refreshHandler: Handler? = null
    private var isRefreshing = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        refreshHandler = Handler(Looper.getMainLooper())
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

    fun saveUserProfile(nickname: String?, email: String?, avatarUrl: String?) {
        prefs?.edit()?.apply {
            if (nickname != null) putString(KEY_NICKNAME, nickname)
            if (email != null) putString(KEY_EMAIL, email)
            if (avatarUrl != null) putString(KEY_AVATAR_URL, avatarUrl)
            apply()
        }
    }

    fun getNickname(): String? = prefs?.getString(KEY_NICKNAME, null)

    fun getEmail(): String? = prefs?.getString(KEY_EMAIL, null)

    fun getAvatarUrl(): String? = prefs?.getString(KEY_AVATAR_URL, null)

    fun saveUserId(userId: String) {
        prefs?.edit()?.putString(KEY_USER_ID, userId)?.apply()
    }

    fun getUserId(): String? = prefs?.getString(KEY_USER_ID, null)

    fun getAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs?.getString(KEY_REFRESH_TOKEN, null)

    fun clearTokens() {
        prefs?.edit()?.clear()?.apply()
        stopPeriodicRefresh()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun refreshToken(): Boolean {
        if (!isRefreshing.compareAndSet(false, true)) return false
        try {
            val token = getRefreshToken() ?: return false
            val latch = CountDownLatch(1)
            var success = false
            RetrofitClient.api.refreshToken(RefreshTokenRequest(token))
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        success = handleRefreshResponse(response)
                        latch.countDown()
                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        latch.countDown()
                    }
                })
            latch.await()
            return success
        } catch (_: Exception) {
            return false
        } finally {
            isRefreshing.set(false)
        }
    }

    private fun handleRefreshResponse(response: Response<ApiResponse>): Boolean {
        if (!response.isSuccessful) return false
        val apiResp = response.body() ?: return false
        if (apiResp.code != 0 || apiResp.data == null) return false
        return try {
            val json = Gson().toJson(apiResp.data)
            val data = Gson().fromJson(json,
                object : TypeToken<com.example.clientforwebstorage.network.models.RefreshTokenResponse>() {}.type)
                as? com.example.clientforwebstorage.network.models.RefreshTokenResponse ?: return false
            saveTokens(data.accessToken, data.refreshToken)
            true
        } catch (_: Exception) { false }
    }

    fun startPeriodicRefresh() {
        stopPeriodicRefresh()
        refreshHandler?.post(object : Runnable {
            override fun run() {
                if (isLoggedIn()) refreshToken()
                refreshHandler?.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        })
    }

    fun stopPeriodicRefresh() {
        refreshHandler?.removeCallbacksAndMessages(null)
    }
}
