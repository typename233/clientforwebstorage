package com.example.clientforwebstorage

import com.example.clientforwebstorage.network.models.AiAgentConfigDTO
import com.example.clientforwebstorage.network.models.AiChatRequest
import com.example.clientforwebstorage.network.models.AddMembersRequest
import com.example.clientforwebstorage.ui.agent.AiAgentViewModel
import com.example.clientforwebstorage.ui.agent.AiChatMessage
import com.example.clientforwebstorage.ui.agent.AiConfigData
import com.example.clientforwebstorage.ui.agent.AiConversationItem
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AiAgentUnitTest {

    private lateinit var viewModel: AiAgentViewModel
    private val gson = Gson()

    @Before
    fun setup() {
        viewModel = AiAgentViewModel()
    }

    // ==================== AiChatRequest 模型测试 ====================

    @Test
    fun aiChatRequest_defaultValues() {
        val request = AiChatRequest(message = "你好")
        assertEquals("你好", request.message)
        assertNull(request.conversationId)
        assertTrue(request.useHistory)
    }

    @Test
    fun aiChatRequest_withConversationId() {
        val request = AiChatRequest(
            message = "帮我搜索文件",
            conversationId = "conv_123",
            useHistory = false
        )
        assertEquals("帮我搜索文件", request.message)
        assertEquals("conv_123", request.conversationId)
        assertFalse(request.useHistory)
    }

    @Test
    fun aiChatRequest_jsonSerialization() {
        val request = AiChatRequest(
            message = "测试消息",
            conversationId = "conv_456",
            useHistory = true
        )
        val json = gson.toJson(request)
        val deserialized = gson.fromJson(json, AiChatRequest::class.java)
        assertEquals(request.message, deserialized.message)
        assertEquals(request.conversationId, deserialized.conversationId)
        assertEquals(request.useHistory, deserialized.useHistory)
    }

    @Test
    fun aiChatRequest_jsonSerialization_nullConversationId() {
        val request = AiChatRequest(message = "测试")
        val json = gson.toJson(request)
        assertFalse(json.contains("conversationId"))
    }

    // ==================== AiAgentConfigDTO 模型测试 ====================

    @Test
    fun aiAgentConfigDTO_defaultValues() {
        val config = AiAgentConfigDTO()
        assertNull(config.modelName)
        assertNull(config.temperature)
        assertNull(config.maxTokens)
        assertNull(config.systemPrompt)
        assertNull(config.enabled)
    }

    @Test
    fun aiAgentConfigDTO_fullConfig() {
        val config = AiAgentConfigDTO(
            modelName = "gpt-4o-mini",
            temperature = 0.7,
            maxTokens = 2048,
            systemPrompt = "你是一个助手",
            enabled = true
        )
        assertEquals("gpt-4o-mini", config.modelName)
        assertEquals(0.7, config.temperature!!, 0.001)
        assertEquals(2048, config.maxTokens!!)
        assertEquals("你是一个助手", config.systemPrompt)
        assertTrue(config.enabled!!)
    }

    @Test
    fun aiAgentConfigDTO_jsonSerialization() {
        val config = AiAgentConfigDTO(
            modelName = "gpt-4o",
            temperature = 1.0,
            maxTokens = 4096,
            systemPrompt = "系统提示",
            enabled = false
        )
        val json = gson.toJson(config)
        val deserialized = gson.fromJson(json, AiAgentConfigDTO::class.java)
        assertEquals(config.modelName, deserialized.modelName)
        assertEquals(config.temperature, deserialized.temperature)
        assertEquals(config.maxTokens, deserialized.maxTokens)
        assertEquals(config.systemPrompt, deserialized.systemPrompt)
        assertEquals(config.enabled, deserialized.enabled)
    }

    @Test
    fun aiAgentConfigDTO_partialUpdate() {
        val config = AiAgentConfigDTO(temperature = 0.5)
        val json = gson.toJson(config)
        val deserialized = gson.fromJson(json, AiAgentConfigDTO::class.java)
        assertNull(deserialized.modelName)
        assertEquals(0.5, deserialized.temperature!!, 0.001)
        assertNull(deserialized.maxTokens)
    }

    // ==================== AddMembersRequest 升级测试 ====================

    @Test
    fun addMembersRequest_withUserIds() {
        val request = AddMembersRequest(userIds = listOf("uuid-1", "uuid-2"))
        assertEquals(listOf("uuid-1", "uuid-2"), request.userIds)
        assertNull(request.emails)
        assertNull(request.phones)
    }

    @Test
    fun addMembersRequest_withEmails() {
        val request = AddMembersRequest(emails = listOf("user1@example.com", "user2@example.com"))
        assertNull(request.userIds)
        assertEquals(listOf("user1@example.com", "user2@example.com"), request.emails)
        assertNull(request.phones)
    }

    @Test
    fun addMembersRequest_withPhones() {
        val request = AddMembersRequest(phones = listOf("13800138000", "13900139000"))
        assertNull(request.userIds)
        assertNull(request.emails)
        assertEquals(listOf("13800138000", "13900139000"), request.phones)
    }

    @Test
    fun addMembersRequest_mixedIdentifiers() {
        val request = AddMembersRequest(
            userIds = listOf("uuid-1"),
            emails = listOf("user@example.com"),
            phones = listOf("13800138000")
        )
        assertEquals(listOf("uuid-1"), request.userIds)
        assertEquals(listOf("user@example.com"), request.emails)
        assertEquals(listOf("13800138000"), request.phones)
    }

    @Test
    fun addMembersRequest_jsonSerialization() {
        val request = AddMembersRequest(
            userIds = listOf("uuid-1"),
            emails = listOf("a@b.com"),
            phones = listOf("13800000000"),
            role = "editor"
        )
        val json = gson.toJson(request)
        val deserialized = gson.fromJson(json, AddMembersRequest::class.java)
        assertEquals(request.userIds, deserialized.userIds)
        assertEquals(request.emails, deserialized.emails)
        assertEquals(request.phones, deserialized.phones)
        assertEquals(request.role, deserialized.role)
    }

    // ==================== ViewModel 验证逻辑测试 ====================

    @Test
    fun validateMessage_validMessage() {
        assertNull(viewModel.validateMessage("你好"))
        assertNull(viewModel.validateMessage("帮我搜索最近的文件"))
    }

    @Test
    fun validateMessage_emptyMessage() {
        val error = viewModel.validateMessage("")
        assertNotNull(error)
        assertEquals("消息不能为空", error)
    }

    @Test
    fun validateMessage_blankMessage() {
        val error = viewModel.validateMessage("   ")
        assertNotNull(error)
        assertEquals("消息不能为空", error)
    }

    @Test
    fun validateMessage_tooLongMessage() {
        val longMessage = "a".repeat(4001)
        val error = viewModel.validateMessage(longMessage)
        assertNotNull(error)
        assertTrue(error!!.contains("4000"))
    }

    @Test
    fun validateMessage_maxLengthMessage() {
        val maxMessage = "a".repeat(4000)
        assertNull(viewModel.validateMessage(maxMessage))
    }

    @Test
    fun validateConfig_validConfig() {
        val config = AiAgentConfigDTO(
            modelName = "gpt-4o-mini",
            temperature = 0.7,
            maxTokens = 2048,
            systemPrompt = "你是一个助手",
            enabled = true
        )
        assertNull(viewModel.validateConfig(config))
    }

    @Test
    fun validateConfig_modelNameTooLong() {
        val config = AiAgentConfigDTO(modelName = "a".repeat(65))
        val error = viewModel.validateConfig(config)
        assertNotNull(error)
        assertTrue(error!!.contains("64"))
    }

    @Test
    fun validateConfig_modelNameMaxLength() {
        val config = AiAgentConfigDTO(modelName = "a".repeat(64))
        assertNull(viewModel.validateConfig(config))
    }

    @Test
    fun validateConfig_temperatureTooLow() {
        val config = AiAgentConfigDTO(temperature = -0.1)
        val error = viewModel.validateConfig(config)
        assertNotNull(error)
        assertTrue(error!!.contains("0.0"))
    }

    @Test
    fun validateConfig_temperatureTooHigh() {
        val config = AiAgentConfigDTO(temperature = 20.1)
        val error = viewModel.validateConfig(config)
        assertNotNull(error)
        assertTrue(error!!.contains("20.0"))
    }

    @Test
    fun validateConfig_temperatureBoundary() {
        assertNull(viewModel.validateConfig(AiAgentConfigDTO(temperature = 0.0)))
        assertNull(viewModel.validateConfig(AiAgentConfigDTO(temperature = 20.0)))
    }

    @Test
    fun validateConfig_maxTokensTooLow() {
        val config = AiAgentConfigDTO(maxTokens = 255)
        val error = viewModel.validateConfig(config)
        assertNotNull(error)
        assertTrue(error!!.contains("256"))
    }

    @Test
    fun validateConfig_maxTokensTooHigh() {
        val config = AiAgentConfigDTO(maxTokens = 8193)
        val error = viewModel.validateConfig(config)
        assertNotNull(error)
        assertTrue(error!!.contains("8192"))
    }

    @Test
    fun validateConfig_maxTokensBoundary() {
        assertNull(viewModel.validateConfig(AiAgentConfigDTO(maxTokens = 256)))
        assertNull(viewModel.validateConfig(AiAgentConfigDTO(maxTokens = 8192)))
    }

    @Test
    fun validateConfig_systemPromptTooLong() {
        val config = AiAgentConfigDTO(systemPrompt = "a".repeat(4001))
        val error = viewModel.validateConfig(config)
        assertNotNull(error)
        assertTrue(error!!.contains("4000"))
    }

    @Test
    fun validateConfig_systemPromptMaxLength() {
        val config = AiAgentConfigDTO(systemPrompt = "a".repeat(4000))
        assertNull(viewModel.validateConfig(config))
    }

    @Test
    fun validateConfig_nullFieldsAllowed() {
        val config = AiAgentConfigDTO()
        assertNull(viewModel.validateConfig(config))
    }

    // ==================== AiChatMessage 数据类测试 ====================

    @Test
    fun aiChatMessage_creation() {
        val msg = AiChatMessage(
            id = "msg_1",
            role = "user",
            content = "你好",
            timestamp = "1715800000000"
        )
        assertEquals("msg_1", msg.id)
        assertEquals("user", msg.role)
        assertEquals("你好", msg.content)
        assertEquals("1715800000000", msg.timestamp)
    }

    @Test
    fun aiChatMessage_defaultTimestamp() {
        val msg = AiChatMessage(id = "msg_2", role = "assistant", content = "回复")
        assertEquals("", msg.timestamp)
    }

    @Test
    fun aiChatMessage_copy() {
        val original = AiChatMessage(id = "1", role = "user", content = "原始")
        val copied = original.copy(content = "修改")
        assertEquals("修改", copied.content)
        assertEquals("原始", original.content)
        assertEquals("1", copied.id)
    }

    // ==================== AiConversationItem 数据类测试 ====================

    @Test
    fun aiConversationItem_creation() {
        val item = AiConversationItem(
            id = "conv_1",
            title = "测试会话",
            lastMessage = "最后消息",
            lastMessageAt = "2026-05-15 10:00:00",
            messageCount = 5
        )
        assertEquals("conv_1", item.id)
        assertEquals("测试会话", item.title)
        assertEquals("最后消息", item.lastMessage)
        assertEquals(5, item.messageCount)
    }

    @Test
    fun aiConversationItem_defaults() {
        val item = AiConversationItem(id = "conv_2", title = "空会话")
        assertNull(item.lastMessage)
        assertNull(item.lastMessageAt)
        assertEquals(0, item.messageCount)
    }

    // ==================== AiConfigData 数据类测试 ====================

    @Test
    fun aiConfigData_defaults() {
        val config = AiConfigData()
        assertEquals("gpt-4o-mini", config.modelName)
        assertEquals(0.7, config.temperature, 0.001)
        assertEquals(2048, config.maxTokens)
        assertEquals("", config.systemPrompt)
        assertTrue(config.enabled)
    }

    @Test
    fun aiConfigData_customValues() {
        val config = AiConfigData(
            modelName = "gpt-4o",
            temperature = 1.5,
            maxTokens = 4096,
            systemPrompt = "自定义提示",
            enabled = false
        )
        assertEquals("gpt-4o", config.modelName)
        assertEquals(1.5, config.temperature, 0.001)
        assertEquals(4096, config.maxTokens)
        assertEquals("自定义提示", config.systemPrompt)
        assertFalse(config.enabled)
    }

    @Test
    fun aiConfigData_jsonRoundTrip() {
        val config = AiConfigData(
            modelName = "gpt-4o-mini",
            temperature = 0.7,
            maxTokens = 2048,
            systemPrompt = "测试",
            enabled = true
        )
        val json = gson.toJson(config)
        val deserialized = gson.fromJson(json, AiConfigData::class.java)
        assertEquals(config.modelName, deserialized.modelName)
        assertEquals(config.temperature, deserialized.temperature, 0.001)
        assertEquals(config.maxTokens, deserialized.maxTokens)
        assertEquals(config.systemPrompt, deserialized.systemPrompt)
        assertEquals(config.enabled, deserialized.enabled)
    }

    // ==================== ViewModel 状态管理测试 ====================

    @Test
    fun viewModel_initialState() {
        assertNotNull(viewModel.chatMessages)
        assertNotNull(viewModel.conversations)
        assertNotNull(viewModel.currentConversationId)
        assertNotNull(viewModel.config)
        assertNotNull(viewModel.isLoading)
        assertNotNull(viewModel.error)
        assertNull(viewModel.currentConversationId.value)
        assertFalse(viewModel.isLoading.value!!)
        assertNull(viewModel.error.value)
    }

    @Test
    fun viewModel_startNewConversation() {
        viewModel.startNewConversation()
        assertNull(viewModel.currentConversationId.value)
        assertTrue(viewModel.chatMessages.value!!.isEmpty())
    }

    @Test
    fun viewModel_clearError() {
        viewModel.clearError()
        assertNull(viewModel.error.value)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun validateMessage_unicodeCharacters() {
        val message = "你好世界🎉🎊"
        assertNull(viewModel.validateMessage(message))
    }

    @Test
    fun validateConfig_temperatureAtBoundaries() {
        assertNull(viewModel.validateConfig(AiAgentConfigDTO(temperature = 0.0)))
        assertNull(viewModel.validateConfig(AiAgentConfigDTO(temperature = 20.0)))
        assertNotNull(viewModel.validateConfig(AiAgentConfigDTO(temperature = -0.001)))
        assertNotNull(viewModel.validateConfig(AiAgentConfigDTO(temperature = 20.001)))
    }

    @Test
    fun addMembersRequest_emptyLists() {
        val request = AddMembersRequest(
            userIds = emptyList(),
            emails = emptyList(),
            phones = emptyList()
        )
        assertTrue(request.userIds!!.isEmpty())
        assertTrue(request.emails!!.isEmpty())
        assertTrue(request.phones!!.isEmpty())
    }

    @Test
    fun aiChatRequest_messageMaxLength() {
        val request = AiChatRequest(message = "a".repeat(4000))
        assertEquals(4000, request.message.length)
    }

    @Test
    fun validateConfig_allFieldsInvalid() {
        val config = AiAgentConfigDTO(
            modelName = "a".repeat(65),
            temperature = 21.0,
            maxTokens = 100,
            systemPrompt = "a".repeat(4001)
        )
        val error = viewModel.validateConfig(config)
        assertNotNull(error)
    }
}
