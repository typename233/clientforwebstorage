package com.example.clientforwebstorage.ui.agent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.clientforwebstorage.network.ApiService
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.AiAgentConfigDTO
import com.example.clientforwebstorage.network.models.AiChatRequest
import com.example.clientforwebstorage.network.models.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Context
import android.content.SharedPreferences

data class AiChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: String = ""
)

data class AiConversationItem(
    val id: String,
    val title: String,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val messageCount: Int = 0
)

data class AiConfigData(
    val modelName: String = "gpt-4o-mini",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "",
    val enabled: Boolean = true
)

class AiAgentViewModel(
    private val api: ApiService = RetrofitClient.api,
    private val context: Context? = null
) : ViewModel() {

    companion object {
        private const val MAX_MESSAGE_LENGTH = 4000
        private const val MAX_MODEL_NAME_LENGTH = 64
        private const val MAX_SYSTEM_PROMPT_LENGTH = 4000
        private const val MIN_TEMPERATURE = 0.0
        private const val MAX_TEMPERATURE = 20.0
        private const val MIN_MAX_TOKENS = 256
        private const val MAX_MAX_TOKENS = 8192
        private const val CACHE_DURATION_MS = 5 * 60 * 1000L
        private const val PREFS_NAME = "conversation_titles"
        private const val PREFS_CHAT = "conversation_messages"
        private const val KEY_CURRENT_CONVERSATION_ID = "current_conversation_id"
        private const val KEY_TITLES_CACHED = "titles_cached"
    }

    private val gson = Gson()

    private var conversationsCache: List<AiConversationItem>? = null
    private var conversationsCacheTimestamp: Long = 0
    private var isConversationsLoading = false
    private var isTitlesCached: Boolean = false

    private val prefs: SharedPreferences? by lazy {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val chatPrefs: SharedPreferences? by lazy {
        context?.getSharedPreferences(PREFS_CHAT, Context.MODE_PRIVATE)
    }

    private val _chatMessages = MutableLiveData<MutableList<AiChatMessage>>(mutableListOf())
    val chatMessages: LiveData<MutableList<AiChatMessage>> = _chatMessages

    private val _conversations = MutableLiveData<List<AiConversationItem>>()
    val conversations: LiveData<List<AiConversationItem>> = _conversations

    private val _currentConversationId = MutableLiveData<String?>(null)
    val currentConversationId: LiveData<String?> = _currentConversationId

    private val _config = MutableLiveData<AiConfigData>()
    val config: LiveData<AiConfigData> = _config

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _chatSuccess = MutableLiveData<Boolean>()
    val chatSuccess: LiveData<Boolean> = _chatSuccess

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    private val _configUpdateSuccess = MutableLiveData<Boolean>()
    val configUpdateSuccess: LiveData<Boolean> = _configUpdateSuccess

    private val _needRefreshConversations = MutableLiveData(false)
    val needRefreshConversations: LiveData<Boolean> = _needRefreshConversations

    fun clearNeedRefreshFlag() {
        _needRefreshConversations.value = false
    }

    private fun handleChatResponse(apiResponse: ApiResponse) {
        try {
            val json = gson.toJson(apiResponse.data)
            val data = gson.fromJson(json, object : TypeToken<Map<String, Any?>>() {}.type)
                    as? Map<String, Any?> ?: run {
                        _error.value = "解析响应数据失败"
                        return
                    }

            val conversationId = data["conversationId"] as? String
        if (conversationId != null) {
            if (_currentConversationId.value == null) {
                _currentConversationId.value = conversationId
            }
            if (pendingFirstMessage != null) {
                saveConversationTitle(conversationId, pendingFirstMessage!!)
                pendingFirstMessage = null
            }
            conversationsCache = null
            _needRefreshConversations.value = true
        }

            val assistantContent = data["message"] as? String
                ?: data["content"] as? String
                ?: data["reply"] as? String
                ?: ""

            val assistantMessage = AiChatMessage(
                id = (data["messageId"] as? String) ?: generateMessageId(),
                role = "assistant",
                content = assistantContent,
                timestamp = (data["createdAt"] as? String) ?: System.currentTimeMillis().toString()
            )
            val currentMessages = _chatMessages.value ?: mutableListOf()
            currentMessages.add(assistantMessage)
            _chatMessages.value = currentMessages
            _chatSuccess.value = true

            saveCurrentConversationMessages()
        } catch (e: Exception) {
            _error.value = "解析响应失败: ${e.message}"
        }
    }

    fun loadConversations(forceRefresh: Boolean = false) {
        if (isConversationsLoading) return

        val now = System.currentTimeMillis()
        if (!forceRefresh && conversationsCache != null && (now - conversationsCacheTimestamp) < CACHE_DURATION_MS) {
            _conversations.value = conversationsCache!!
            return
        }

        checkAndLoadTitlesCachedFlag()

        isConversationsLoading = true

        api.getAiConversations(page = 1, pageSize = 50).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                isConversationsLoading = false
                _isLoading.value = false

                if (!response.isSuccessful) {
                    _error.value = "加载会话列表失败: ${response.code()}"
                    return
                }
                val apiResponse = response.body() ?: return
                if (apiResponse.code != 0) {
                    _error.value = apiResponse.message
                    return
                }
                try {
                    val json = gson.toJson(apiResponse.data)
                    val dataArray = gson.fromJson<List<Map<String, Any?>>>(
                        json,
                        object : TypeToken<List<Map<String, Any?>>>() {}.type
                    ) ?: emptyList()

                    if (dataArray.isEmpty()) {
                        conversationsCache = emptyList()
                        conversationsCacheTimestamp = System.currentTimeMillis()
                        _conversations.value = emptyList()
                        return
                    }

                    val conversationIds = dataArray.mapNotNull { item ->
                        item["conversationId"] as? String
                    }

                    val conversationItems = conversationIds.map { convId ->
                        AiConversationItem(
                            id = convId,
                            title = getConversationTitleById(convId) ?: "新对话",
                            lastMessageAt = dataArray.find { it["conversationId"] == convId }?.get("lastMessageAt") as? String,
                            messageCount = ((dataArray.find { it["conversationId"] == convId }?.get("messageCount") as? Double)?.toInt() ?: 0)
                        )
                    }
                    conversationsCache = conversationItems
                    conversationsCacheTimestamp = System.currentTimeMillis()
                    _conversations.value = conversationItems

                    if (!isTitlesCached && conversationIds.isNotEmpty()) {
                        fetchAndCacheConversationTitles(conversationIds)
                    }
                } catch (e: Exception) {
                    _error.value = "解析会话列表失败: ${e.message}"
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                isConversationsLoading = false
                _isLoading.value = false
                _error.value = "网络错误: ${t.message}"
            }
        })
    }

    fun loadConversationDetail(conversationId: String) {
        _chatMessages.value = mutableListOf()

        api.getAiConversation(conversationId).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (!response.isSuccessful) {
                    _error.value = "加载会话详情失败: ${response.code()}"
                    return
                }
                val apiResponse = response.body() ?: return
                if (apiResponse.code != 0) {
                    _error.value = apiResponse.message
                    return
                }
                try {
                    val json = gson.toJson(apiResponse.data)
                    val data = gson.fromJson(json, object : TypeToken<Map<String, Any?>>() {}.type)
                            as? Map<String, Any?> ?: return

                    _currentConversationId.value = conversationId

                    val messages = data["messages"] as? List<Map<String, Any?>> ?: return
                    val chatMsgs = messages.map { msg ->
                        AiChatMessage(
                            id = msg["id"] as? String ?: generateMessageId(),
                            role = msg["role"] as? String ?: "user",
                            content = msg["content"] as? String ?: "",
                            timestamp = msg["createdAt"] as? String ?: ""
                        )
                    }.sortedWith(compareBy<AiChatMessage> { parseTimestampToMillis(it.timestamp) })
                        .toMutableList()
                    _chatMessages.value = chatMsgs
                } catch (e: Exception) {
                    _error.value = "解析会话详情失败: ${e.message}"
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                _error.value = "网络错误: ${t.message}"
            }
        })
    }

    fun deleteConversation(conversationId: String) {
        api.deleteAiConversation(conversationId).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (!response.isSuccessful) {
                    _error.value = "删除会话失败: ${response.code()}"
                    return
                }
                val apiResponse = response.body() ?: return
                if (apiResponse.code != 0) {
                    _error.value = apiResponse.message
                    return
                }
                if (_currentConversationId.value == conversationId) {
                    _currentConversationId.value = null
                    _chatMessages.value = mutableListOf()
                }
                _deleteSuccess.value = true
                deleteConversationTitle(conversationId)
                conversationsCache = null
                loadConversations(forceRefresh = true)
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                _error.value = "网络错误: ${t.message}"
            }
        })
    }

    fun loadConfig() {
        api.getAiConfig().enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (!response.isSuccessful) {
                    _error.value = "加载配置失败: ${response.code()}"
                    return
                }
                val apiResponse = response.body() ?: return
                if (apiResponse.code != 0) {
                    _error.value = apiResponse.message
                    return
                }
                try {
                    val json = gson.toJson(apiResponse.data)
                    val configData = gson.fromJson(json, AiConfigData::class.java)
                    _config.value = configData
                } catch (e: Exception) {
                    _error.value = "解析配置失败: ${e.message}"
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                _error.value = "网络错误: ${t.message}"
            }
        })
    }

    fun updateConfig(configDTO: AiAgentConfigDTO) {
        val validationError = validateConfig(configDTO)
        if (validationError != null) {
            _error.value = validationError
            return
        }

        api.updateAiConfig(configDTO).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (!response.isSuccessful) {
                    _error.value = "更新配置失败: ${response.code()}"
                    return
                }
                val apiResponse = response.body() ?: return
                if (apiResponse.code != 0) {
                    _error.value = apiResponse.message
                    return
                }
                _configUpdateSuccess.value = true
                loadConfig()
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                _error.value = "网络错误: ${t.message}"
            }
        })
    }

    fun startNewConversation() {
        clearCurrentConversationFromLocal()
        _currentConversationId.value = null
        _chatMessages.value = mutableListOf()
    }

    fun resetLoadingState() {
        _isLoading.value = false
    }

    private var pendingFirstMessage: String? = null

    fun sendMessage(message: String) {
        if (_currentConversationId.value == null) {
            pendingFirstMessage = message
        }

        val validationError = validateMessage(message)
        if (validationError != null) {
            _error.value = validationError
            return
        }

        _isLoading.value = true

        val userMessage = AiChatMessage(
            id = generateMessageId(),
            role = "user",
            content = message,
            timestamp = System.currentTimeMillis().toString()
        )
        val currentMessages = _chatMessages.value ?: mutableListOf()
        currentMessages.add(userMessage)
        _chatMessages.value = currentMessages

        val request = AiChatRequest(
            message = message,
            conversationId = _currentConversationId.value,
            useHistory = true
        )

        api.aiChat(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                _isLoading.value = false
                if (!response.isSuccessful) {
                    _error.value = "请求失败: ${response.code()}"
                    return
                }
                val apiResponse = response.body()
                if (apiResponse == null) {
                    _error.value = "响应为空"
                    return
                }
                if (apiResponse.code != 0) {
                    _error.value = apiResponse.message
                    return
                }
                handleChatResponse(apiResponse)
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                _isLoading.value = false
                _error.value = "网络错误: ${t.message}"
            }
        })
    }

    fun clearError() {
        _error.value = null
    }

    fun clearChatSuccess() {
        _chatSuccess.value = null
    }

    fun clearDeleteSuccess() {
        _deleteSuccess.value = null
    }

    fun clearConfigUpdateSuccess() {
        _configUpdateSuccess.value = null
    }

    fun validateMessage(message: String): String? {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return "消息不能为空"
        }
        if (trimmed.length > MAX_MESSAGE_LENGTH) {
            return "消息长度不能超过 $MAX_MESSAGE_LENGTH 个字符"
        }
        return null
    }

    fun validateConfig(config: AiAgentConfigDTO): String? {
        config.modelName?.let { name ->
            if (name.length > MAX_MODEL_NAME_LENGTH) {
                return "模型名称长度不能超过 $MAX_MODEL_NAME_LENGTH 个字符"
            }
        }
        config.temperature?.let { temp ->
            if (temp < MIN_TEMPERATURE || temp > MAX_TEMPERATURE) {
                return "温度参数范围为 $MIN_TEMPERATURE ~ $MAX_TEMPERATURE"
            }
        }
        config.maxTokens?.let { tokens ->
            if (tokens < MIN_MAX_TOKENS || tokens > MAX_MAX_TOKENS) {
                return "最大Token数范围为 $MIN_MAX_TOKENS ~ $MAX_MAX_TOKENS"
            }
        }
        config.systemPrompt?.let { prompt ->
            if (prompt.length > MAX_SYSTEM_PROMPT_LENGTH) {
                return "系统提示词长度不能超过 $MAX_SYSTEM_PROMPT_LENGTH 个字符"
            }
        }
        return null
    }

    private var messageIdCounter = 0L
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${messageIdCounter++}"
    }

    private fun parseTimestampToMillis(timestamp: String): Long {
        if (timestamp.isEmpty()) return 0L

        val numericValue = timestamp.toLongOrNull()
        if (numericValue != null) {
            return if (numericValue > 1000000000000) numericValue else numericValue * 1000
        }

        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm:ss"
        )

        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                val date = sdf.parse(timestamp) ?: continue
                return date.time
            } catch (_: Exception) {
                continue
            }
        }

        return 0L
    }

    fun getConversationTitleById(conversationId: String): String? {
        if (conversationId.isEmpty()) return null
        return prefs?.getString("title_$conversationId", null)
    }

    private fun saveConversationTitle(conversationId: String, firstMessage: String) {
        if (conversationId.isEmpty()) return
        val title = if (firstMessage.length > 20) firstMessage.substring(0, 20) + "..." else firstMessage
        prefs?.edit()?.putString("title_$conversationId", title)?.apply()
    }

    fun deleteConversationTitle(conversationId: String) {
        if (conversationId.isEmpty()) return
        prefs?.edit()?.remove("title_$conversationId")?.apply()
    }

    fun renameConversationTitle(conversationId: String, newTitle: String) {
        if (conversationId.isEmpty() || newTitle.isBlank()) return
        val title = if (newTitle.length > 20) newTitle.substring(0, 20) + "..." else newTitle
        prefs?.edit()?.putString("title_$conversationId", title)?.apply()

        val currentList = _conversations.value ?: return
        val updatedList = currentList.map { item ->
            if (item.id == conversationId) item.copy(title = title) else item
        }
        conversationsCache = updatedList
        _conversations.value = updatedList
    }

    fun searchConversations(query: String): List<AiConversationItem> {
        if (query.isBlank()) return conversationsCache ?: emptyList()
        val allConversations = conversationsCache ?: _conversations.value ?: return emptyList()
        return allConversations.filter { item ->
            item.title.contains(query, ignoreCase = true)
        }
    }

    private fun checkAndLoadTitlesCachedFlag() {
        isTitlesCached = prefs?.getBoolean(KEY_TITLES_CACHED, false) ?: false
    }

    fun markTitlesAsCached() {
        isTitlesCached = true
        prefs?.edit()?.putBoolean(KEY_TITLES_CACHED, true)?.apply()
    }

    fun clearTitlesCachedFlag() {
        isTitlesCached = false
        prefs?.edit()?.remove(KEY_TITLES_CACHED)?.apply()
    }

    private fun fetchAndCacheConversationTitles(conversationIds: List<String>) {
        if (conversationIds.isEmpty()) return

        var completedCount = 0
        val totalCount = conversationIds.size

        conversationIds.forEach { conversationId ->
            api.getAiConversation(conversationId).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    completedCount++

                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.code == 0) {
                            try {
                                val json = gson.toJson(apiResponse.data)
                                val data = gson.fromJson(json, object : TypeToken<Map<String, Any?>>() {}.type)
                                        as? Map<String, Any?>

                                data?.let { convData ->
                                    val messages = convData["messages"] as? List<Map<String, Any?>>
                                    val firstUserMessage = messages?.find { it["role"] == "user" }
                                    val firstMessageContent = firstUserMessage?.get("content") as? String

                                    if (!firstMessageContent.isNullOrEmpty()) {
                                        saveConversationTitle(conversationId, firstMessageContent)

                                        conversationsCache?.let { cache ->
                                            val updatedCache = cache.map { item ->
                                                if (item.id == conversationId) {
                                                    val title = if (firstMessageContent.length > 20) firstMessageContent.substring(0, 20) + "..." else firstMessageContent
                                                    item.copy(title = title)
                                                } else item
                                            }
                                            conversationsCache = updatedCache
                                            _conversations.value = updatedCache
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (completedCount >= totalCount) {
                        markTitlesAsCached()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    completedCount++
                    if (completedCount >= totalCount) {
                        markTitlesAsCached()
                    }
                }
            })
        }
    }

    private fun saveCurrentConversationMessages() {
        val conversationId = _currentConversationId.value ?: return
        val messages = _chatMessages.value ?: return

        try {
            val messagesJson = gson.toJson(messages)
            chatPrefs?.edit()
                ?.putString("messages_$conversationId", messagesJson)
                ?.putString(KEY_CURRENT_CONVERSATION_ID, conversationId)
                ?.apply()
        } catch (e: Exception) {
            // 保存失败不影响主流程
        }
    }

    fun restoreCurrentConversation(): Boolean {
        val savedConversationId = chatPrefs?.getString(KEY_CURRENT_CONVERSATION_ID, null)
        if (savedConversationId.isNullOrEmpty()) return false

        val messagesJson = chatPrefs?.getString("messages_$savedConversationId", null)
        if (messagesJson.isNullOrEmpty()) return false

        try {
            val messagesType = object : TypeToken<MutableList<AiChatMessage>>() {}.type
            val messages = gson.fromJson<MutableList<AiChatMessage>>(messagesJson, messagesType)

            _currentConversationId.value = savedConversationId
            _chatMessages.value = messages ?: mutableListOf()

            return !_chatMessages.value.isNullOrEmpty()
        } catch (e: Exception) {
            return false
        }
    }

    fun clearCurrentConversationFromLocal() {
        val conversationId = _currentConversationId.value ?: return
        chatPrefs?.edit()
            ?.remove("messages_$conversationId")
            ?.remove(KEY_CURRENT_CONVERSATION_ID)
            ?.apply()
    }

    override fun onCleared() {
        saveCurrentConversationMessages()
        super.onCleared()
    }
}

class AiAgentViewModelFactory(
    private val api: ApiService = RetrofitClient.api,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AiAgentViewModel(api, context) as T
    }
}
