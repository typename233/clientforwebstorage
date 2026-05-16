package com.example.clientforwebstorage.ui.agent

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.network.models.AiAgentConfigDTO
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

class AgentFragment : Fragment() {

    private var onNavigateToFiles: (() -> Unit)? = null
    private var onNavigateToUpload: (() -> Unit)? = null
    private lateinit var viewModel: AiAgentViewModel
    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var etInput: TextInputEditText
    private lateinit var sendButton: ImageButton
    private lateinit var layoutInputContainer: LinearLayout
    private lateinit var layoutThinkingIndicator: LinearLayout
    private var isKeyboardVisible = false

    private var isSidebarOpen = false
    private lateinit var sidebarContainer: FrameLayout
    private lateinit var sidebarOverlay: View
    private lateinit var rvConversations: RecyclerView
    private lateinit var conversationAdapter: ConversationAdapter
    private var conversationListLoaded = false
    private lateinit var backPressedCallback: OnBackPressedCallback
    private var lastRenderedConversationId: String? = null
    private lateinit var toolbar: MaterialToolbar
    private var isViewInitialized = false

    fun setNavigationCallbacks(onFiles: () -> Unit, onUpload: () -> Unit) {
        onNavigateToFiles = onFiles
        onNavigateToUpload = onUpload
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_agent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, AiAgentViewModelFactory(context = requireContext().applicationContext))[AiAgentViewModel::class.java]

        val toolbarView = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar = toolbarView
        etInput = view.findViewById(R.id.et_input)
        sendButton = view.findViewById(R.id.btn_send)
        val btnMore = view.findViewById<ImageButton>(R.id.btn_agent_more)

        chatContainer = view.findViewById(R.id.chat_container)
        chatScrollView = view.findViewById(R.id.chat_scroll_view)
        layoutInputContainer = view.findViewById(R.id.layout_input_container)
        layoutThinkingIndicator = view.findViewById(R.id.layout_thinking_indicator)

        sidebarContainer = view.findViewById(R.id.sidebar_container)
        sidebarOverlay = view.findViewById(R.id.view_sidebar_overlay)

        setupSidebar(view)

        toolbar.setNavigationOnClickListener {
            toggleSidebar()
        }

        sidebarOverlay.setOnClickListener {
            closeSidebar()
        }

        btnMore.setOnClickListener { showMoreMenu(it) }
        etInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && isSidebarOpen) {
                closeSidebar()
            }
        }

        etInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val text = etInput.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    viewModel.sendMessage(text)
                    etInput.text?.clear()
                }
                true
            } else {
                false
            }
        }

        sendButton.setOnClickListener {
            val text = etInput.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                etInput.text?.clear()
            }
        }

        setupQuickActions(view)

        if (!isViewInitialized) {
            isViewInitialized = true
            setupKeyboardListener()
            setupBackPressedHandler()
        }

        setupObservers()
        viewModel.loadConversations()
        restoreViewState()
    }

    private fun restoreViewState() {
        val restored = viewModel.restoreCurrentConversation()
        if (restored) {
            lastRenderedConversationId = null
            renderChatMessages(viewModel.chatMessages.value ?: mutableListOf())
            updateConversationTitleDisplay(viewModel.currentConversationId.value)
        } else {
            updateConversationTitleDisplay(viewModel.currentConversationId.value)
            val messages = viewModel.chatMessages.value
            if (!messages.isNullOrEmpty()) {
                lastRenderedConversationId = null
                renderChatMessages(messages)
            }
        }
    }

    private fun setupQuickActions(rootView: View) {
        rootView.findViewById<View>(R.id.quick_search).setOnClickListener {
            startNewConversationWithPrompt("搜索最近修改的文档")
        }
        rootView.findViewById<View>(R.id.quick_organize).setOnClickListener {
            startNewConversationWithPrompt("整理上个月的照片")
        }
        rootView.findViewById<View>(R.id.quick_share).setOnClickListener {
            startNewConversationWithPrompt("创建分享链接")
        }
        rootView.findViewById<View>(R.id.quick_summary).setOnClickListener {
            startNewConversationWithPrompt("生成文件夹摘要")
        }
    }

    private fun startNewConversationWithPrompt(prompt: String) {
        viewModel.startNewConversation()
        lastRenderedConversationId = null
        viewModel.sendMessage(prompt)
    }

    private fun updateConversationTitleDisplay(conversationId: String?) {
        if (conversationId.isNullOrEmpty()) {
            toolbar.title = "智能助手"
            return
        }

        val title = viewModel.getConversationTitleById(conversationId)
        if (title.isNullOrEmpty()) {
            toolbar.title = "智能助手"
            return
        }

        val displayTitle = if (title.length > 5) title.substring(0, 5) + "..." else title
        toolbar.title = displayTitle
    }

    private fun setupBackPressedHandler() {
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (isSidebarOpen) {
                    closeSidebar()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun setupSidebar(rootView: View) {
        val sidebarView = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_sidebar_conversations, sidebarContainer, false)
        sidebarContainer.addView(sidebarView)

        rvConversations = sidebarView.findViewById(R.id.rv_conversations)
        val pbLoading = sidebarView.findViewById<ProgressBar>(R.id.pb_loading)
        val tvEmptyState = sidebarView.findViewById<TextView>(R.id.tv_empty_state)
        val tvErrorState = sidebarView.findViewById<TextView>(R.id.tv_error_state)
        val tvUserName = sidebarView.findViewById<TextView>(R.id.tv_user_name)
        val btnNewConversation = sidebarView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_new_conversation)
        val searchView = sidebarView.findViewById<androidx.appcompat.widget.SearchView>(R.id.search_view_conversations)

        tvUserName.text = TokenManager.getNickname() ?: "用户"

        rvConversations.layoutManager = LinearLayoutManager(requireContext())

        val searchSrcText = searchView.findViewById<androidx.appcompat.widget.SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        searchSrcText?.setOnClickListener {
            searchSrcText.requestFocus()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(searchSrcText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = if (newText.isNullOrBlank()) {
                    viewModel.conversations.value ?: emptyList()
                } else {
                    viewModel.searchConversations(newText)
                }
                conversationAdapter?.updateData(filteredList)
                updateEmptyState(filteredList.isEmpty(), tvEmptyState, rvConversations, pbLoading, tvErrorState)
                return true
            }
        })

        btnNewConversation.setOnClickListener {
            viewModel.startNewConversation()
            lastRenderedConversationId = null
            Toast.makeText(requireContext(), "已开始新对话", Toast.LENGTH_SHORT).show()
            closeSidebar()
        }

        viewModel.conversations.observe(viewLifecycleOwner) { conversations ->
            conversationListLoaded = true
            pbLoading.visibility = View.GONE

            if (conversations.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                rvConversations.visibility = View.GONE
                tvErrorState.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                rvConversations.visibility = View.VISIBLE
                tvErrorState.visibility = View.GONE

                conversationAdapter = ConversationAdapter(
                    items = conversations,
                    onItemClick = { item ->
                        viewModel.loadConversationDetail(item.id)
                        closeSidebar()
                    },
                    onDeleteClick = { item ->
                        showDeleteConversationConfirmationDialog(item)
                    },
                    onEditClick = { item ->
                        showRenameDialog(item)
                    }
                )
                rvConversations.adapter = conversationAdapter
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading && !conversationListLoaded) {
                pbLoading.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                if (!conversationListLoaded) {
                    tvErrorState.visibility = View.VISIBLE
                    tvEmptyState.visibility = View.GONE
                    rvConversations.visibility = View.GONE
                    pbLoading.visibility = View.GONE
                }
                tvErrorState.setOnClickListener {
                    viewModel.clearError()
                    viewModel.loadConversations()
                    tvErrorState.visibility = View.GONE
                    pbLoading.visibility = View.VISIBLE
                }
            }
        }

        viewModel.needRefreshConversations.observe(viewLifecycleOwner) { needRefresh ->
            if (needRefresh) {
                viewModel.clearNeedRefreshFlag()
                conversationListLoaded = false
                viewModel.loadConversations(forceRefresh = true)
            }
        }
    }

    private fun toggleSidebar() {
        if (isSidebarOpen) {
            closeSidebar()
        } else {
            openSidebar()
        }
    }

    private fun openSidebar() {
        isSidebarOpen = true
        sidebarContainer.visibility = View.VISIBLE
        sidebarOverlay.visibility = View.VISIBLE

        if (::backPressedCallback.isInitialized) {
            backPressedCallback.isEnabled = true
        }

        sidebarContainer.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        sidebarOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        updateToolbarIcon(true)

        if (!conversationListLoaded) {
            viewModel.loadConversations()
        }
    }

    private fun closeSidebar() {
        if (!isSidebarOpen) return

        conversationAdapter?.hideExpandedButtons()

        if (::backPressedCallback.isInitialized) {
            backPressedCallback.isEnabled = false
        }

        val sidebarWidth = dpToPx(280)

        sidebarContainer.animate()
            .translationX((-sidebarWidth).toFloat())
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                sidebarContainer.visibility = View.GONE
                sidebarOverlay.visibility = View.GONE
            }
            .start()

        sidebarOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        updateToolbarIcon(false)
        isSidebarOpen = false
    }

    private fun updateToolbarIcon(isOpen: Boolean) {
        val toolbar = view?.findViewById<MaterialToolbar>(R.id.toolbar) ?: return
        if (isOpen) {
            toolbar.navigationIcon = requireContext().getDrawable(android.R.drawable.ic_menu_close_clear_cancel)
            toolbar.navigationIcon?.setTint(resources.getColor(android.R.color.white, null))
        } else {
            toolbar.navigationIcon = requireContext().getDrawable(android.R.drawable.ic_menu_manage)
            toolbar.navigationIcon?.setTint(resources.getColor(android.R.color.white, null))
        }
    }

    private fun setupObservers() {
        viewModel.chatMessages.observe(viewLifecycleOwner) { messages ->
            renderChatMessages(messages)
        }

        viewModel.currentConversationId.observe(viewLifecycleOwner) { conversationId ->
            updateConversationTitleDisplay(conversationId)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            sendButton.isEnabled = !loading
            sendButton.alpha = if (loading) 0.5f else 1.0f
            layoutThinkingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) {
                scrollToBottom()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.chatSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                scrollToBottom()
                viewModel.clearChatSuccess()
            }
        }

        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                Toast.makeText(requireContext(), "会话已删除", Toast.LENGTH_SHORT).show()
                viewModel.clearDeleteSuccess()
            }
        }

        viewModel.configUpdateSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                Toast.makeText(requireContext(), "配置已更新", Toast.LENGTH_SHORT).show()
                viewModel.clearConfigUpdateSuccess()
            }
        }
    }

    companion object {
        private const val WELCOME_VIEW_COUNT = 6
    }

    private fun renderChatMessages(messages: List<AiChatMessage>) {
        val currentConversationId = viewModel.currentConversationId.value

        if (currentConversationId != lastRenderedConversationId) {
            android.util.Log.d("AgentFragment", "Conversation changed: $lastRenderedConversationId -> $currentConversationId, forcing full re-render")
            lastRenderedConversationId = currentConversationId
            removeMessageViewsOnly()
            inflateAllMessages(messages)
            return
        }

        android.util.Log.d("AgentFragment", "renderChatMessages called: messages.size=${messages.size}, childCount=${chatContainer.childCount}")

        if (messages.isEmpty()) {
            removeMessageViewsOnly()
            return
        }

        val currentMessageCount = chatContainer.childCount - WELCOME_VIEW_COUNT
        val messageCount = messages.size

        android.util.Log.d("AgentFragment", "currentMessageCount=$currentMessageCount, messageCount=$messageCount")

        if (currentMessageCount != messageCount) {
            android.util.Log.d("AgentFragment", "Performing full re-render")
            removeMessageViewsOnly()
            inflateAllMessages(messages)
        } else {
            android.util.Log.d("AgentFragment", "Performing incremental render from index ${WELCOME_VIEW_COUNT + currentMessageCount}")
            inflateNewMessages(messages, WELCOME_VIEW_COUNT + currentMessageCount)
        }
    }

    private fun removeMessageViewsOnly() {
        while (chatContainer.childCount > WELCOME_VIEW_COUNT) {
            chatContainer.removeViewAt(chatContainer.childCount - 1)
        }
    }

    private fun inflateAllMessages(messages: List<AiChatMessage>) {
        val inflater = LayoutInflater.from(requireContext())

        android.util.Log.d("AgentFragment", "inflateAllMessages: rendering ${messages.size} messages in order")

        for ((index, msg) in messages.withIndex()) {
            val isUser = msg.role == "user"
            val bubbleView = inflater.inflate(
                if (isUser) R.layout.item_message_sent else R.layout.item_message_received,
                chatContainer,
                false
            )
            val tvContent = bubbleView.findViewById<TextView>(R.id.tv_message_content)
            val tvTime = bubbleView.findViewById<TextView>(R.id.tv_message_time)

            tvContent.text = msg.content
            tvTime.text = formatTimestamp(msg.timestamp)

            chatContainer.addView(bubbleView, -1)

            android.util.Log.d("AgentFragment", "Added message[$index]: role=${msg.role}, content=${msg.content.take(20)}, timestamp=${msg.timestamp}, position=${chatContainer.childCount - 1}")
        }

        scrollToBottom()
    }

    private fun inflateNewMessages(messages: List<AiChatMessage>, startIndex: Int) {
        val inflater = LayoutInflater.from(requireContext())

        android.util.Log.d("AgentFragment", "inflateNewMessages: rendering from $startIndex to ${messages.size - 1}")

        for (i in startIndex until messages.size) {
            val msg = messages[i]
            val isUser = msg.role == "user"
            val bubbleView = inflater.inflate(
                if (isUser) R.layout.item_message_sent else R.layout.item_message_received,
                chatContainer,
                false
            )
            val tvContent = bubbleView.findViewById<TextView>(R.id.tv_message_content)
            val tvTime = bubbleView.findViewById<TextView>(R.id.tv_message_time)

            tvContent.text = msg.content
            tvTime.text = formatTimestamp(msg.timestamp)

            chatContainer.addView(bubbleView, -1)

            android.util.Log.d("AgentFragment", "Added new message[$i]: role=${msg.role}, content=${msg.content.take(20)}, timestamp=${msg.timestamp}, position=${chatContainer.childCount - 1}")
        }

        scrollToBottom()
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        if (timestamp.isEmpty()) return ""
        return try {
            val millis = timestamp.toLongOrNull()
            if (millis != null && millis > 1000000000000) {
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(millis))
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }

    private fun showRenameDialog(item: AiConversationItem) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_rename_conversation, null)

        val etNewTitle = dialogView.findViewById<EditText>(R.id.et_new_title)
        etNewTitle.setText(item.title)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("重命名对话")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val newTitle = etNewTitle.text?.toString()?.trim().orEmpty()
                if (newTitle.isNotEmpty()) {
                    viewModel.renameConversationTitle(item.id, newTitle)
                    Toast.makeText(requireContext(), "重命名成功", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    private fun updateEmptyState(
        isEmpty: Boolean,
        tvEmpty: TextView,
        rvList: RecyclerView,
        pbLoading: ProgressBar,
        tvError: TextView
    ) {
        if (isEmpty) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "无匹配的对话"
            rvList.visibility = View.GONE
            pbLoading.visibility = View.GONE
            tvError.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvList.visibility = View.VISIBLE
        }
    }

    private fun showMoreMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_agent_menu, null)

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

        popupView.findViewById<View>(R.id.menu_clear_history)?.setOnClickListener {
            showDeleteConversationConfirmationDialog()
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_settings)?.setOnClickListener {
            showSettingsDialog()
            popupWindow.dismiss()
        }

        val offsetX = anchor.width - popupWidth
        popupWindow.showAsDropDown(anchor, offsetX, 0)
    }

    private fun showDeleteConversationConfirmationDialog() {
        val currentConversationId = viewModel.currentConversationId.value

        if (currentConversationId == null) {
            Toast.makeText(requireContext(), "当前没有会话可删除", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("删除当前会话")
            .setMessage("确定要删除当前会话吗？删除后聊天记录将被永久清除，无法恢复。")
            .setPositiveButton("确定删除") { _, _ ->
                viewModel.deleteConversation(currentConversationId)
                Toast.makeText(requireContext(), "正在删除会话...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            resources.getColor(android.R.color.holo_red_light, null)
        )
    }

    private fun showDeleteConversationConfirmationDialog(item: AiConversationItem) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("删除对话")
            .setMessage("确定要删除「${item.title}」这个对话吗？删除后聊天记录将被永久清除，无法恢复。")
            .setPositiveButton("确定删除") { _, _ ->
                viewModel.deleteConversation(item.id)
                Toast.makeText(requireContext(), "正在删除「${item.title}」...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            resources.getColor(android.R.color.holo_red_light, null)
        )
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_ai_settings, null)

        val etModelName = dialogView.findViewById<EditText>(R.id.et_model_name)
        val etTemperature = dialogView.findViewById<EditText>(R.id.et_temperature)
        val etMaxTokens = dialogView.findViewById<EditText>(R.id.et_max_tokens)
        val etSystemPrompt = dialogView.findViewById<EditText>(R.id.et_system_prompt)
        val switchEnabled = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_enabled)
        val pbLoading = dialogView.findViewById<ProgressBar>(R.id.pb_config_loading)
        val btnSaveConfig = dialogView.findViewById<View>(R.id.btn_save_config)

        pbLoading.visibility = View.VISIBLE
        btnSaveConfig.isEnabled = false

        fillConfigFromViewModel(etModelName, etTemperature, etMaxTokens, etSystemPrompt, switchEnabled)

        viewModel.loadConfig()

        val configObserver = androidx.lifecycle.Observer<AiConfigData> { config ->
            if (config != null) {
                fillConfigToForm(config, etModelName, etTemperature, etMaxTokens, etSystemPrompt, switchEnabled)
                pbLoading.visibility = View.GONE
                btnSaveConfig.isEnabled = true
            }
        }

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.show()

        viewModel.config.observe(viewLifecycleOwner, configObserver)

        dialog.setOnDismissListener {
            viewModel.config.removeObserver(configObserver)
        }

        btnSaveConfig?.setOnClickListener {
            val modelName = etModelName.text?.toString()?.trim().orEmpty()
            val temperatureStr = etTemperature.text?.toString()?.trim().orEmpty()
            val maxTokensStr = etMaxTokens.text?.toString()?.trim().orEmpty()
            val systemPrompt = etSystemPrompt.text?.toString()?.trim().orEmpty()
            val enabled = switchEnabled.isChecked

            val configDTO = AiAgentConfigDTO(
                modelName = modelName.ifEmpty { null },
                temperature = temperatureStr.toDoubleOrNull(),
                maxTokens = maxTokensStr.toIntOrNull(),
                systemPrompt = systemPrompt.ifEmpty { null },
                enabled = enabled
            )

            val validationError = viewModel.validateConfig(configDTO)
            if (validationError != null) {
                Toast.makeText(requireContext(), validationError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pbLoading.visibility = View.VISIBLE
            btnSaveConfig.isEnabled = false

            viewModel.updateConfig(configDTO)

            Toast.makeText(requireContext(), "配置已保存", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_cancel_config)?.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun fillConfigFromViewModel(
        etModelName: EditText,
        etTemperature: EditText,
        etMaxTokens: EditText,
        etSystemPrompt: EditText,
        switchEnabled: com.google.android.material.switchmaterial.SwitchMaterial
    ) {
        viewModel.config.value?.let { config ->
            fillConfigToForm(config, etModelName, etTemperature, etMaxTokens, etSystemPrompt, switchEnabled)
        }
    }

    private fun fillConfigToForm(
        config: AiConfigData,
        etModelName: EditText,
        etTemperature: EditText,
        etMaxTokens: EditText,
        etSystemPrompt: EditText,
        switchEnabled: com.google.android.material.switchmaterial.SwitchMaterial
    ) {
        etModelName.setText(if (config.modelName.isNullOrEmpty()) "glm-4-flash" else config.modelName)

        if (config.temperature != null) {
            etTemperature.setText(config.temperature.toString())
        } else {
            etTemperature.setText("")
        }

        if (config.maxTokens != null) {
            etMaxTokens.setText(config.maxTokens.toString())
        } else {
            etMaxTokens.setText("")
        }

        etSystemPrompt.setText(config.systemPrompt.orEmpty())

        if (config.enabled != null) {
            switchEnabled.isChecked = config.enabled
        } else {
            switchEnabled.isChecked = true
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupKeyboardListener() {
        view?.viewTreeObserver?.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            view?.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view?.rootView?.height ?: 0
            val keyboardHeight = screenHeight - rect.bottom

            val wasKeyboardVisible = isKeyboardVisible
            isKeyboardVisible = keyboardHeight > screenHeight * 0.15

            if (isKeyboardVisible && !wasKeyboardVisible) {
                (activity as? com.example.clientforwebstorage.MainActivity)?.hideBottomNav()
                layoutInputContainer.animate()
                    .translationY((-keyboardHeight).toFloat())
                    .setDuration(0)
                    .start()
                scrollToBottom()
            } else if (!isKeyboardVisible && wasKeyboardVisible) {
                layoutInputContainer.animate()
                    .translationY(0f)
                    .setDuration(0)
                    .start()
                (activity as? com.example.clientforwebstorage.MainActivity)?.updateBottomNavigationVisibility()
            }
        }
    }

}
