package com.example.clientforwebstorage

import com.example.clientforwebstorage.network.models.AiAgentConfigDTO
import com.example.clientforwebstorage.network.models.AiChatRequest
import com.example.clientforwebstorage.network.models.AddMembersRequest
import com.example.clientforwebstorage.network.models.ApiResponse
import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AiAgentIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var gson: Gson
    private lateinit var testApi: TestApiService

    interface TestApiService {
        @retrofit2.http.POST("api/v2/ai/chat")
        fun aiChat(@retrofit2.http.Body request: AiChatRequest): retrofit2.Call<ApiResponse>

        @retrofit2.http.GET("api/v2/ai/conversations")
        fun getAiConversations(
            @retrofit2.http.Query("page") page: Int?,
            @retrofit2.http.Query("pageSize") pageSize: Int?
        ): retrofit2.Call<ApiResponse>

        @retrofit2.http.GET("api/v2/ai/conversations/{conversationId}")
        fun getAiConversation(@retrofit2.http.Path("conversationId") conversationId: String): retrofit2.Call<ApiResponse>

        @retrofit2.http.DELETE("api/v2/ai/conversations/{conversationId}")
        fun deleteAiConversation(@retrofit2.http.Path("conversationId") conversationId: String): retrofit2.Call<ApiResponse>

        @retrofit2.http.GET("api/v2/ai/config")
        fun getAiConfig(): retrofit2.Call<ApiResponse>

        @retrofit2.http.PUT("api/v2/ai/config")
        fun updateAiConfig(@retrofit2.http.Body request: AiAgentConfigDTO): retrofit2.Call<ApiResponse>

        @retrofit2.http.POST("api/v2/chat/conversations/{conversationId}/members")
        fun addMembers(
            @retrofit2.http.Path("conversationId") conversationId: String,
            @retrofit2.http.Body request: AddMembersRequest
        ): retrofit2.Call<ApiResponse>
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        gson = Gson()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        testApi = retrofit.create(TestApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // ==================== AI Chat 接口集成测试 ====================

    @Test
    fun aiChat_successResponse() {
        val responseBody = mapOf(
            "code" to 0,
            "message" to "success",
            "data" to mapOf(
                "conversationId" to "conv_001",
                "messageId" to "msg_001",
                "message" to "你好！有什么可以帮您的？",
                "createdAt" to "2026-05-15T10:30:00Z"
            )
        )
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(responseBody))
            .setHeader("Content-Type", "application/json"))

        val request = AiChatRequest(message = "你好", conversationId = null, useHistory = true)
        val response = testApi.aiChat(request).execute()

        assertTrue(response.isSuccessful)
        val apiResponse = response.body()!!
        assertEquals(0, apiResponse.code)
        assertEquals("success", apiResponse.message)
        assertNotNull(apiResponse.data)
    }

    @Test
    fun aiChat_requestBodyFormat() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        val request = AiChatRequest(message = "帮我搜索文件", conversationId = "conv_123", useHistory = true)
        testApi.aiChat(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("api/v2/ai/chat", recordedRequest.path)

        val requestBody = recordedRequest.body.readUtf8()
        val parsedRequest = gson.fromJson(requestBody, AiChatRequest::class.java)
        assertEquals("帮我搜索文件", parsedRequest.message)
        assertEquals("conv_123", parsedRequest.conversationId)
        assertTrue(parsedRequest.useHistory)
    }

    @Test
    fun aiChat_newConversationWithoutId() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", mapOf("conversationId" to "new_conv_001"))))
            .setHeader("Content-Type", "application/json"))

        val request = AiChatRequest(message = "新对话")
        val response = testApi.aiChat(request).execute()

        assertTrue(response.isSuccessful)
        val recordedRequest = mockWebServer.takeRequest()
        val requestBody = recordedRequest.body.readUtf8()
        assertFalse(requestBody.contains("conversationId"))
    }

    @Test
    fun aiChat_errorResponse() {
        val errorBody = mapOf(
            "code" to 40001,
            "message" to "消息内容不能为空",
            "data" to null
        )
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(errorBody))
            .setHeader("Content-Type", "application/json"))

        val response = testApi.aiChat(AiChatRequest(message = "test")).execute()

        val apiResponse = response.body()!!
        assertEquals(40001, apiResponse.code)
        assertEquals("消息内容不能为空", apiResponse.message)
    }

    // ==================== AI Conversations 接口集成测试 ====================

    @Test
    fun getAiConversations_successResponse() {
        val responseBody = mapOf(
            "code" to 0,
            "message" to "success",
            "data" to mapOf(
                "total" to 2,
                "items" to listOf(
                    mapOf("id" to "conv_1", "title" to "会话1", "lastMessage" to "消息1", "messageCount" to 5),
                    mapOf("id" to "conv_2", "title" to "会话2", "lastMessage" to "消息2", "messageCount" to 3)
                )
            )
        )
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(responseBody))
            .setHeader("Content-Type", "application/json"))

        val response = testApi.getAiConversations(page = 1, pageSize = 20).execute()

        assertTrue(response.isSuccessful)
        val apiResponse = response.body()!!
        assertEquals(0, apiResponse.code)
        assertNotNull(apiResponse.data)
    }

    @Test
    fun getAiConversations_requestPath() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        testApi.getAiConversations(page = 1, pageSize = 10).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path!!.startsWith("api/v2/ai/conversations"))
        assertTrue(recordedRequest.path!!.contains("page=1"))
        assertTrue(recordedRequest.path!!.contains("pageSize=10"))
    }

    // ==================== AI Conversation Detail 接口集成测试 ====================

    @Test
    fun getAiConversation_successResponse() {
        val responseBody = mapOf(
            "code" to 0,
            "message" to "success",
            "data" to mapOf(
                "id" to "conv_001",
                "title" to "测试会话",
                "messages" to listOf(
                    mapOf("id" to "msg_1", "role" to "user", "content" to "你好", "createdAt" to "2026-05-15T10:00:00Z"),
                    mapOf("id" to "msg_2", "role" to "assistant", "content" to "你好！", "createdAt" to "2026-05-15T10:00:01Z")
                )
            )
        )
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(responseBody))
            .setHeader("Content-Type", "application/json"))

        val response = testApi.getAiConversation("conv_001").execute()

        assertTrue(response.isSuccessful)
        val apiResponse = response.body()!!
        assertEquals(0, apiResponse.code)
    }

    @Test
    fun getAiConversation_requestPath() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        testApi.getAiConversation("conv_123").execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("api/v2/ai/conversations/conv_123", recordedRequest.path)
    }

    // ==================== Delete AI Conversation 接口集成测试 ====================

    @Test
    fun deleteAiConversation_successResponse() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        val response = testApi.deleteAiConversation("conv_001").execute()

        assertTrue(response.isSuccessful)
        assertEquals(0, response.body()!!.code)
    }

    @Test
    fun deleteAiConversation_requestPath() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        testApi.deleteAiConversation("conv_del").execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertEquals("api/v2/ai/conversations/conv_del", recordedRequest.path)
    }

    // ==================== AI Config 接口集成测试 ====================

    @Test
    fun getAiConfig_successResponse() {
        val responseBody = mapOf(
            "code" to 0,
            "message" to "success",
            "data" to mapOf(
                "modelName" to "gpt-4o-mini",
                "temperature" to 0.7,
                "maxTokens" to 2048,
                "systemPrompt" to "你是一个智能助手",
                "enabled" to true
            )
        )
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(responseBody))
            .setHeader("Content-Type", "application/json"))

        val response = testApi.getAiConfig().execute()

        assertTrue(response.isSuccessful)
        val apiResponse = response.body()!!
        assertEquals(0, apiResponse.code)
    }

    @Test
    fun getAiConfig_requestPath() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        testApi.getAiConfig().execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("api/v2/ai/config", recordedRequest.path)
    }

    @Test
    fun updateAiConfig_successResponse() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        val configDTO = AiAgentConfigDTO(
            modelName = "gpt-4o-mini",
            temperature = 0.7,
            maxTokens = 2048,
            systemPrompt = "你是一个智能助手",
            enabled = true
        )
        val response = testApi.updateAiConfig(configDTO).execute()

        assertTrue(response.isSuccessful)
        assertEquals(0, response.body()!!.code)
    }

    @Test
    fun updateAiConfig_requestBodyFormat() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        val configDTO = AiAgentConfigDTO(
            modelName = "gpt-4o",
            temperature = 1.0,
            maxTokens = 4096,
            enabled = false
        )
        testApi.updateAiConfig(configDTO).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("PUT", recordedRequest.method)
        assertEquals("api/v2/ai/config", recordedRequest.path)

        val requestBody = recordedRequest.body.readUtf8()
        val parsedConfig = gson.fromJson(requestBody, AiAgentConfigDTO::class.java)
        assertEquals("gpt-4o", parsedConfig.modelName)
        assertEquals(1.0, parsedConfig.temperature!!, 0.001)
        assertEquals(4096, parsedConfig.maxTokens!!)
        assertFalse(parsedConfig.enabled!!)
    }

    // ==================== AddMembers 升级接口集成测试 ====================

    @Test
    fun addMembers_withEmailsAndPhones() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        val request = AddMembersRequest(
            emails = listOf("user1@example.com", "user2@example.com"),
            phones = listOf("13800138000", "13900139000")
        )
        val response = testApi.addMembers("conv_001", request).execute()

        assertTrue(response.isSuccessful)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("api/v2/chat/conversations/conv_001/members", recordedRequest.path)

        val requestBody = recordedRequest.body.readUtf8()
        val parsedRequest = gson.fromJson(requestBody, AddMembersRequest::class.java)
        assertEquals(listOf("user1@example.com", "user2@example.com"), parsedRequest.emails)
        assertEquals(listOf("13800138000", "13900139000"), parsedRequest.phones)
    }

    @Test
    fun addMembers_withUserIdsOnly() {
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        val request = AddMembersRequest(userIds = listOf("uuid-1", "uuid-2"))
        testApi.addMembers("conv_002", request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        val requestBody = recordedRequest.body.readUtf8()
        val parsedRequest = gson.fromJson(requestBody, AddMembersRequest::class.java)
        assertEquals(listOf("uuid-1", "uuid-2"), parsedRequest.userIds)
    }

    // ==================== HTTP 错误处理集成测试 ====================

    @Test
    fun aiChat_serverError() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        val response = testApi.aiChat(AiChatRequest(message = "test")).execute()

        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun aiChat_unauthorized() {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val response = testApi.aiChat(AiChatRequest(message = "test")).execute()

        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test
    fun getAiConfig_notFound() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val response = testApi.getAiConfig().execute()

        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }

    @Test
    fun deleteAiConversation_forbidden() {
        mockWebServer.enqueue(MockResponse().setResponseCode(403))

        val response = testApi.deleteAiConversation("conv_001").execute()

        assertFalse(response.isSuccessful)
        assertEquals(403, response.code())
    }

    // ==================== 完整 API 流程集成测试 ====================

    @Test
    fun fullAiChatWorkflow() {
        val configResponse = mapOf(
            "code" to 0,
            "message" to "success",
            "data" to mapOf(
                "modelName" to "gpt-4o-mini",
                "temperature" to 0.7,
                "maxTokens" to 2048,
                "enabled" to true
            )
        )
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(configResponse))
            .setHeader("Content-Type", "application/json"))

        val configResp = testApi.getAiConfig().execute()
        assertTrue(configResp.isSuccessful)
        assertEquals(0, configResp.body()!!.code)

        val chatResponse = mapOf(
            "code" to 0,
            "message" to "success",
            "data" to mapOf(
                "conversationId" to "conv_workflow_001",
                "messageId" to "msg_workflow_001",
                "message" to "已找到3个最近修改的文件",
                "createdAt" to "2026-05-15T10:30:00Z"
            )
        )
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(chatResponse))
            .setHeader("Content-Type", "application/json"))

        val chatResp = testApi.aiChat(AiChatRequest(message = "搜索最近的文件")).execute()
        assertTrue(chatResp.isSuccessful)
        assertEquals(0, chatResp.body()!!.code)

        val conversationsResponse = mapOf(
            "code" to 0,
            "message" to "success",
            "data" to mapOf("total" to 1, "items" to listOf(
                mapOf("id" to "conv_workflow_001", "title" to "搜索最近的文件")
            ))
        )
        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(conversationsResponse))
            .setHeader("Content-Type", "application/json"))

        val convResp = testApi.getAiConversations(page = 1, pageSize = 20).execute()
        assertTrue(convResp.isSuccessful)

        mockWebServer.enqueue(MockResponse()
            .setBody(gson.toJson(ApiResponse(0, "success", null)))
            .setHeader("Content-Type", "application/json"))

        val deleteResp = testApi.deleteAiConversation("conv_workflow_001").execute()
        assertTrue(deleteResp.isSuccessful)

        assertEquals(4, mockWebServer.requestCount)
    }
}
