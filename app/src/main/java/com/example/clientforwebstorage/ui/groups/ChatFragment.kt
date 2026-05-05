package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
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
import com.google.android.material.appbar.MaterialToolbar

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
        setHasOptionsMenu(true)
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
        loadMockMessages()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_chat)
        recyclerMessages = view.findViewById(R.id.recycler_messages)
        etMessageInput = view.findViewById(R.id.et_message_input)
        btnSend = view.findViewById(R.id.btn_send)
    }

    private fun setupToolbar() {
        toolbar.title = groupName
        toolbar.inflateMenu(R.menu.menu_chat_toolbar)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_group_detail -> {
                    navigateToGroupDetail()
                    true
                }
                else -> false
            }
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

    private fun sendMessage(content: String) {
        val newMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderId = "current_user",
            senderName = "我",
            content = content,
            timestamp = getCurrentTime(),
            isOwn = true
        )
        
        messages.add(newMessage)
        messageAdapter.notifyItemInserted(messages.size - 1)
        recyclerMessages.scrollToPosition(messages.size - 1)
        
        // TODO: 调用实际的消息发送 API
        Toast.makeText(requireContext(), "消息发送成功（模拟）", Toast.LENGTH_SHORT).show()
    }

    private fun loadMockMessages() {
        // 加载模拟数据（后续替换为真实 API）
        messages.clear()
        
        messages.add(ChatMessage(
            id = "1",
            senderId = "user_1",
            senderName = "张三",
            content = "大家好！",
            timestamp = "14:25",
            isOwn = false
        ))
        
        messages.add(ChatMessage(
            id = "2",
            senderId = "user_2",
            senderName = "李四",
            content = "文件已经上传到共享文件夹了",
            timestamp = "14:28",
            isOwn = false
        ))
        
        messages.add(ChatMessage(
            id = "3",
            senderId = "current_user",
            senderName = "我",
            content = "收到，辛苦了！",
            timestamp = "14:30",
            isOwn = true
        ))
        
        messageAdapter.notifyDataSetChanged()
        
        if (messages.isNotEmpty()) {
            recyclerMessages.scrollToPosition(messages.size - 1)
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
                    // 自己的消息：靠右，无头像，无昵称，绿色气泡
                    layoutContainer.gravity = android.view.Gravity.END
                    ivAvatar.visibility = View.GONE
                    tvSenderName.visibility = View.GONE
                    layoutNameBubble.setPadding(0, 0, 0, 0)
                    layoutBubble.setBackgroundResource(R.drawable.bg_chat_bubble_sent)
                } else {
                    // 对方的消息：靠左，有头像，昵称在气泡上方（QQ风格）
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
}
