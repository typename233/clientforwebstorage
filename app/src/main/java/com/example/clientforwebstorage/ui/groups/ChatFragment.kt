package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.EditMessageRequest
import com.example.clientforwebstorage.network.models.SendMessageRequest
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

enum class MessageSendStatus {
    SUCCESS,
    FAILED,
    SENDING
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    var content: String,
    val timestamp: String,
    val isOwn: Boolean = false,
    var sendStatus: MessageSendStatus = MessageSendStatus.SUCCESS,
    val messageType: String = "text"
)

class ChatFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var messageAdapter: MessageAdapter

    private var conversationId: String? = null
    private var conversationName: String = ""
    private var currentUserId: String = ""
    
    private var nextCursor: String? = null
    private var isLoadingMore = false

    private val messages = mutableListOf<ChatMessage>()

    companion object {
        private const val ARG_CONVERSATION_ID = "conversation_id"
        private const val ARG_CONVERSATION_NAME = "conversation_name"

        fun newInstance(conversationId: String, conversationName: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONVERSATION_ID, conversationId)
                    putString(ARG_CONVERSATION_NAME, conversationName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationId = it.getString(ARG_CONVERSATION_ID)
            conversationName = it.getString(ARG_CONVERSATION_NAME, "群组聊天")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = TokenManager.getUserId() ?: ""

        initViews(view)
        setupToolbar()
        setupRecyclerView()
        setupInputArea()
        loadMessages()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_chat)
        recyclerMessages = view.findViewById(R.id.recycler_messages)
        etMessageInput = view.findViewById(R.id.et_message_input)
        btnSend = view.findViewById(R.id.btn_send)
    }

    private fun setupToolbar() {
        toolbar.title = conversationName
        toolbar.navigationIcon = resources.getDrawable(android.R.drawable.ic_menu_revert, null)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val btnMore = view?.findViewById<ImageButton>(R.id.btn_chat_more)
        btnMore?.setOnClickListener {
            navigateToGroupDetail()
        }
    }

    private fun navigateToGroupDetail() {
        val detailFragment = GroupDetailFragment.newInstance(
            conversationId ?: "",
            conversationName
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack("group_detail")
            .commit()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages) { message, position ->
            handleMessageLongClick(message, position)
        }
        recyclerMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        recyclerMessages.adapter = messageAdapter
        
        recyclerMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(-1) && !isLoadingMore && nextCursor != null) {
                    loadMoreMessages()
                }
            }
        })
    }

    private fun setupInputArea() {
        btnSend.setOnClickListener {
            val messageText = etMessageInput.text?.toString()?.trim()
            if (!messageText.isNullOrBlank()) {
                sendMessage(messageText)
                etMessageInput.text?.clear()
            } else {
                Toast.makeText(requireContext(), "请输入消息内容", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMessages() {
        val cid = conversationId ?: return

        RetrofitClient.api.listMessages(cid, null, 30)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = response.body()?.data
                        val result = parseMessageListResponse(data)
                        messages.clear()
                        messages.addAll(result.first)
                        nextCursor = result.second
                        messageAdapter.notifyDataSetChanged()

                        if (messages.isNotEmpty()) {
                            recyclerMessages.scrollToPosition(messages.size - 1)
                        }
                        
                        markMessagesAsRead()
                    } else {
                        val errorMsg = if (response.isSuccessful) {
                            "加载失败: ${response.body()?.message}"
                        } else {
                            "加载失败: HTTP ${response.code()}"
                        }
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadMoreMessages() {
        val cid = conversationId ?: return
        val cursor = nextCursor ?: return
        isLoadingMore = true

        RetrofitClient.api.listMessages(cid, cursor, 30)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    isLoadingMore = false
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = response.body()?.data
                        val result = parseMessageListResponse(data)
                        val oldSize = messages.size
                        messages.addAll(0, result.first)
                        nextCursor = result.second
                        messageAdapter.notifyItemRangeInserted(0, result.first.size)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    isLoadingMore = false
                }
            })
    }

    private fun sendMessage(content: String) {
        val cid = conversationId ?: return

        val tempId = "temp_${System.currentTimeMillis()}"
        val clientMsgId = "client_${System.currentTimeMillis()}"
        val tempMessage = ChatMessage(
            id = tempId,
            senderId = currentUserId,
            senderName = "我",
            content = content,
            timestamp = getCurrentTime(),
            isOwn = true,
            sendStatus = MessageSendStatus.SENDING
        )

        messages.add(tempMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerMessages.scrollToPosition(messages.size - 1)

        val request = SendMessageRequest(
            messageType = "text",
            contentText = content,
            clientMessageId = clientMsgId
        )
        val messageIndex = messages.size - 1

        RetrofitClient.api.sendMessage(cid, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        messages[messageIndex].sendStatus = MessageSendStatus.SUCCESS
                        messageAdapter.notifyItemChanged(messageIndex)
                        
                        val msgData = parseSingleMessage(response.body()?.data)
                        if (msgData != null) {
                            messages[messageIndex] = msgData.copy(isOwn = true, sendStatus = MessageSendStatus.SUCCESS)
                            messageAdapter.notifyItemChanged(messageIndex)
                        }
                        
                        markMessagesAsRead()
                    } else {
                        messages[messageIndex].sendStatus = MessageSendStatus.FAILED
                        messageAdapter.notifyItemChanged(messageIndex)
                        Toast.makeText(requireContext(), "消息发送失败，点击红色按钮重试", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    messages[messageIndex].sendStatus = MessageSendStatus.FAILED
                    messageAdapter.notifyItemChanged(messageIndex)
                    Toast.makeText(requireContext(), "网络错误：${t.message}，点击红色按钮重试", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun retrySendMessage(position: Int) {
        if (position < 0 || position >= messages.size) return

        val message = messages[position]
        if (!message.isOwn) return

        message.sendStatus = MessageSendStatus.SENDING
        messageAdapter.notifyItemChanged(position)

        val cid = conversationId ?: return
        val request = SendMessageRequest(
            messageType = "text",
            contentText = message.content,
            clientMessageId = "client_retry_${System.currentTimeMillis()}"
        )

        RetrofitClient.api.sendMessage(cid, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        messages[position].sendStatus = MessageSendStatus.SUCCESS
                        messageAdapter.notifyItemChanged(position)
                        Toast.makeText(requireContext(), "消息重新发送成功", Toast.LENGTH_SHORT).show()
                        markMessagesAsRead()
                    } else {
                        messages[position].sendStatus = MessageSendStatus.FAILED
                        messageAdapter.notifyItemChanged(position)
                        Toast.makeText(requireContext(), "重新发送失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    messages[position].sendStatus = MessageSendStatus.FAILED
                    messageAdapter.notifyItemChanged(position)
                    Toast.makeText(requireContext(), "网络错误：${t.message}，请重试", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun editMessage(position: Int) {
        if (position < 0 || position >= messages.size) return
        
        val message = messages[position]
        if (!message.isOwn || message.id.startsWith("temp_")) return

        val editText = EditText(requireContext()).apply {
            setText(message.content)
            isSingleLine = false
            setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14))
            setSelection(message.content.length)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑消息")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newContent = editText.text?.toString()?.trim().orEmpty()
                if (newContent.isNotEmpty() && newContent != message.content) {
                    performEditMessage(message.id, newContent, position)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performEditMessage(messageId: String, newContent: String, position: Int) {
        RetrofitClient.api.editMessage(messageId, EditMessageRequest(contentText = newContent))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        messages[position].content = newContent
                        messageAdapter.notifyItemChanged(position)
                        Toast.makeText(requireContext(), "消息已编辑", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "编辑失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun recallMessage(position: Int) {
        if (position < 0 || position >= messages.size) return
        
        val message = messages[position]
        if (!message.isOwn || message.id.startsWith("temp_")) return

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("撤回消息")
            .setMessage("确定要撤回这条消息吗？（仅支持2分钟内的消息）")
            .setPositiveButton("撤回") { _, _ ->
                performRecallMessage(message.id, position)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performRecallMessage(messageId: String, position: Int) {
        RetrofitClient.api.recallMessage(messageId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        messages.removeAt(position)
                        messageAdapter.notifyItemRemoved(position)
                        Toast.makeText(requireContext(), "消息已撤回", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "撤回失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleMessageLongClick(message: ChatMessage, position: Int) {
        if (!message.isOwn) return
        
        val options = mutableListOf<String>()
        
        if (!message.id.startsWith("temp_")) {
            options.add("编辑消息")
            options.add("撤回消息")
        }
        
        if (options.isEmpty()) return
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("操作")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> editMessage(position)
                    1 -> recallMessage(position)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun markMessagesAsRead() {
        val cid = conversationId ?: return
        val unreadMessageIds = messages.filter { !it.isOwn }.map { it.id }
        
        if (unreadMessageIds.isNotEmpty()) {
            RetrofitClient.api.markReadBatch(
                com.example.clientforwebstorage.network.models.ReadBatchRequest(
                    conversationId = cid,
                    messageIds = unreadMessageIds
                )
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {}
                
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
            })
        }
    }

    private fun parseMessageListResponse(data: Any?): Pair<List<ChatMessage>, String?> {
        if (data == null) return Pair(emptyList(), null)
        return try {
            val map = data as? Map<String, Any?> ?: return Pair(emptyList(), null)
            val items = map["items"] as? List<*> ?: return Pair(emptyList(), null)
            val cursor = map["nextCursor"] as? String

            val messageList = items.mapNotNull { item ->
                val itemMap = item as? Map<String, Any?> ?: return@mapNotNull null
                parseMessageFromMap(itemMap)
            }.sortedWith(compareBy<ChatMessage> { parseTimestampToMillis(it.timestamp) })

            Pair(messageList, cursor)
        } catch (_: Exception) {
            Pair(emptyList(), null)
        }
    }

    private fun parseSingleMessage(data: Any?): ChatMessage? {
        if (data == null) return null
        return try {
            val itemMap = data as? Map<String, Any?>
            parseMessageFromMap(itemMap!!)
        } catch (_: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMessageFromMap(itemMap: Map<String, Any?>): ChatMessage? {
        return try {
            val id = itemMap["id"] as? String ?: ""
            val senderData = itemMap["sender"] as? Map<String, Any?>
            val senderId = senderData?.get("userId") as? String 
                ?: senderData?.get("id") as? String 
                ?: ""
            val senderName = senderData?.get("nickname") as? String 
                ?: senderData?.get("name") as? String 
                ?: "未知用户"
            val contentText = itemMap["contentText"] as? String ?: ""
            val messageType = itemMap["messageType"] as? String ?: "text"
            val createdAt = itemMap["createdAt"] as? String ?: ""
            
            ChatMessage(
                id = id,
                senderId = senderId,
                senderName = senderName,
                content = contentText,
                timestamp = createdAt,
                isOwn = senderId == currentUserId,
                messageType = messageType
            )
        } catch (_: Exception) { null }
    }

    private fun getCurrentTime(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    private fun shouldShowTimestamp(position: Int): Boolean {
        if (position < 0 || position >= messages.size) return true

        val currentMessage = messages[position]

        if (position == messages.size - 1) return true

        val nextMessage = messages[position + 1]

        return currentMessage.senderId != nextMessage.senderId
    }

    private fun isLastInConsecutiveSequence(position: Int): Boolean {
        if (position < 0 || position >= messages.size) return true
        if (position == messages.size - 1) return true

        val currentMessage = messages[position]
        val nextMessage = messages[position + 1]

        return currentMessage.senderId != nextMessage.senderId
    }

    private fun formatTimestamp(timestamp: String): String {
        if (timestamp.isBlank()) return ""

        return try {
            val inputFormats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "MM-dd HH:mm",
                "HH:mm"
            )

            var parsedDate: java.util.Date? = null

            for (format in inputFormats) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                    parsedDate = sdf.parse(timestamp)
                    if (parsedDate != null) break
                } catch (_: Exception) {
                    continue
                }
            }

            if (parsedDate == null) return timestamp

            val now = java.util.Date()
            val calendarNow = java.util.Calendar.getInstance().apply { time = now }
            val calendarMsg = java.util.Calendar.getInstance().apply { time = parsedDate }

            val isSameDay = calendarNow.get(java.util.Calendar.YEAR) == calendarMsg.get(java.util.Calendar.YEAR) &&
                    calendarNow.get(java.util.Calendar.DAY_OF_YEAR) == calendarMsg.get(java.util.Calendar.DAY_OF_YEAR)

            return if (isSameDay) {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(parsedDate)
            } else {
                java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(parsedDate)
            }
        } catch (_: Exception) {
            timestamp
        }
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

    inner class MessageAdapter(
        private val items: List<ChatMessage>,
        private val onItemLongClick: (ChatMessage, Int) -> Unit
    ) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

        inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val layoutContainer: LinearLayout = itemView.findViewById(R.id.layout_message_container)
            val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
            val layoutNameBubble: LinearLayout = itemView.findViewById(R.id.layout_name_bubble)
            val tvSenderName: TextView = itemView.findViewById(R.id.tv_sender_name)
            val layoutBubble: View = itemView.findViewById(R.id.layout_bubble)
            val tvMessageText: TextView = itemView.findViewById(R.id.tv_message_text)
            val tvMessageTime: TextView = itemView.findViewById(R.id.tv_message_time)
            val btnRetrySend: ImageButton = itemView.findViewById(R.id.btn_retry_send)
            val progressRetry: ProgressBar = itemView.findViewById(R.id.progress_retry)

            fun bind(message: ChatMessage, position: Int) {
                tvMessageText.text = message.content

                val formattedTime = formatTimestamp(message.timestamp)
                tvMessageTime.text = formattedTime

                val showTimestamp = isLastInConsecutiveSequence(position)
                tvMessageTime.visibility = if (showTimestamp) View.VISIBLE else View.GONE

                when (message.sendStatus) {
                    MessageSendStatus.SENDING -> {
                        btnRetrySend.visibility = View.GONE
                        progressRetry.visibility = if (message.isOwn) View.VISIBLE else View.GONE
                    }
                    MessageSendStatus.FAILED -> {
                        btnRetrySend.visibility = if (message.isOwn) View.VISIBLE else View.GONE
                        progressRetry.visibility = View.GONE
                    }
                    MessageSendStatus.SUCCESS -> {
                        btnRetrySend.visibility = View.GONE
                        progressRetry.visibility = View.GONE
                    }
                }

                btnRetrySend.setOnClickListener {
                    retrySendMessage(position)
                }

                itemView.setOnLongClickListener {
                    onItemLongClick(message, position)
                    true
                }

                if (message.isOwn) {
                    layoutContainer.gravity = android.view.Gravity.END
                    ivAvatar.visibility = View.GONE
                    tvSenderName.visibility = View.GONE
                    layoutNameBubble.setPadding(0, 0, 0, 0)

                    if (message.sendStatus == MessageSendStatus.FAILED) {
                        layoutBubble.setBackgroundResource(R.drawable.bg_chat_bubble_failed)
                        tvMessageText.alpha = 0.7f
                    } else {
                        layoutBubble.setBackgroundResource(R.drawable.bg_chat_bubble_sent)
                        tvMessageText.alpha = 1.0f
                    }
                } else {
                    layoutContainer.gravity = android.view.Gravity.START
                    ivAvatar.visibility = View.VISIBLE
                    ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
                    tvSenderName.visibility = View.VISIBLE
                    tvSenderName.text = message.senderName
                    tvSenderName.setTextColor(0xFF666666.toInt())
                    layoutBubble.setBackgroundResource(android.R.color.white)
                    tvMessageText.alpha = 1.0f
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            holder.bind(items[position], position)
        }

        override fun getItemCount(): Int = items.size
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
