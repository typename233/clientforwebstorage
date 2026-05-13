package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
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
    val content: String,
    val timestamp: String,
    val isOwn: Boolean = false,
    var sendStatus: MessageSendStatus = MessageSendStatus.SUCCESS
)

class ChatFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var messageAdapter: MessageAdapter

    private var groupId: String? = null
    private var groupName: String = ""
    private var currentUserId: String = ""

    private val messages = mutableListOf<ChatMessage>()

    companion object {
        private const val ARG_GROUP_ID = "group_id"
        private const val ARG_GROUP_NAME = "group_name"

        fun newInstance(groupId: String, groupName: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                    putString(ARG_GROUP_NAME, groupName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID)
            groupName = it.getString(ARG_GROUP_NAME, "群组聊天")
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
        toolbar.title = groupName
        toolbar.navigationIcon = resources.getDrawable(android.R.drawable.ic_menu_revert, null)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val btnMore = view?.findViewById<ImageButton>(R.id.btn_chat_more)
        btnMore?.setOnClickListener {
            showChatMenu(it)
        }
    }

    private fun showChatMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_chat_menu, null)

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            setBackgroundDrawable(resources.getDrawable(android.R.color.white, null))
            isOutsideTouchable = true
            isFocusable = true
        }

        popupView.findViewById<View>(R.id.menu_group_info)?.setOnClickListener {
            navigateToGroupDetail()
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_search_messages)?.setOnClickListener {
            Toast.makeText(requireContext(), "搜索消息功能开发中", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_clear_messages)?.setOnClickListener {
            Toast.makeText(requireContext(), "清空消息记录功能开发中", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        val offsetX = anchor.width - popupWidth
        popupWindow.showAsDropDown(anchor, offsetX, 0)
    }

    private fun navigateToGroupDetail() {
        val detailFragment = GroupDetailFragment.newInstance(
            groupId ?: "",
            groupName
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack("group_detail")
            .commit()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        recyclerMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        recyclerMessages.adapter = messageAdapter
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
        val gid = groupId ?: return

        RetrofitClient.api.getGroupMessages(gid, 1, 50)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = response.body()?.data
                        val messageList = parseMessages(data)
                        messages.clear()
                        messages.addAll(messageList)
                        messageAdapter.notifyDataSetChanged()

                        if (messages.isNotEmpty()) {
                            recyclerMessages.scrollToPosition(messages.size - 1)
                        }
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

    private fun sendMessage(content: String) {
        val gid = groupId ?: return

        val tempId = "temp_${System.currentTimeMillis()}"
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

        val request = SendMessageRequest(content = content)
        val messageIndex = messages.size - 1

        RetrofitClient.api.sendGroupMessage(gid, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        messages[messageIndex].sendStatus = MessageSendStatus.SUCCESS
                        messageAdapter.notifyItemChanged(messageIndex)
                        loadMessages()
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

        val gid = groupId ?: return
        val request = SendMessageRequest(content = message.content)

        RetrofitClient.api.sendGroupMessage(gid, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        messages[position].sendStatus = MessageSendStatus.SUCCESS
                        messageAdapter.notifyItemChanged(position)
                        Toast.makeText(requireContext(), "消息重新发送成功", Toast.LENGTH_SHORT).show()
                        loadMessages()
                    } else {
                        messages[position].sendStatus = MessageSendStatus.FAILED
                        messageAdapter.notifyItemChanged(position)
                        Toast.makeText(requireContext(), "重新发送失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    messages[position].sendStatus = MessageSendStatus.FAILED
                    messageAdapter.notifyItemChanged(position)
                    Toast.makeText(requireContext(), "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun parseMessages(data: Any?): List<ChatMessage> {
        if (data == null) return emptyList()
        return try {
            val json = Gson().toJson(data)
            val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
            val rawList: List<Map<String, Any?>> = Gson().fromJson(json, type)

            rawList.map { item ->
                val senderName = item["senderName"]?.toString() ?: item["sender"]?.toString() ?: "未知用户"
                val senderId = item["senderId"]?.toString() ?: item["userId"]?.toString() ?: ""
                val content = item["content"]?.toString() ?: ""
                val timestamp = item["createdAt"]?.toString() ?: item["timestamp"]?.toString() ?: ""
                val id = item["id"]?.toString() ?: ""
                val isOwn = senderId == currentUserId || item["isOwn"] as? Boolean == true

                ChatMessage(
                    id = id,
                    senderId = senderId,
                    senderName = senderName,
                    content = content,
                    timestamp = timestamp,
                    isOwn = isOwn
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
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

    inner class MessageAdapter(private val items: List<ChatMessage>) :
        RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

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

                val showTimestamp = shouldShowTimestamp(position)
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
