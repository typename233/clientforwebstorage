package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: String,
    val isOwn: Boolean = false
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
            navigateToGroupDetail()
        }
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

        val request = SendMessageRequest(content = content)

        RetrofitClient.api.sendGroupMessage(gid, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        loadMessages()
                    } else {
                        Toast.makeText(requireContext(), "发送消息失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
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
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
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

            fun bind(message: ChatMessage) {
                tvMessageText.text = message.content
                tvMessageTime.text = message.timestamp

                if (message.isOwn) {
                    layoutContainer.gravity = android.view.Gravity.END
                    ivAvatar.visibility = View.GONE
                    tvSenderName.visibility = View.GONE
                    layoutNameBubble.setPadding(0, 0, 0, 0)
                    layoutBubble.setBackgroundResource(R.drawable.bg_chat_bubble_sent)
                } else {
                    layoutContainer.gravity = android.view.Gravity.START
                    ivAvatar.visibility = View.VISIBLE
                    ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
                    tvSenderName.visibility = View.VISIBLE
                    tvSenderName.text = message.senderName
                    tvSenderName.setTextColor(0xFF666666.toInt())
                    layoutBubble.setBackgroundResource(android.R.color.white)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
