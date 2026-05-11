package com.example.clientforwebstorage.ui.groups

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.GroupData
import com.example.clientforwebstorage.network.models.GroupListData
import com.example.clientforwebstorage.network.models.InviteData
import com.example.clientforwebstorage.network.models.InviteListData
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class InvitesFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerInvites: RecyclerView
    private lateinit var adapter: UnifiedRecordAdapter
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var tabAll: TextView
    private lateinit var tabPending: TextView
    private lateinit var tabNotifications: TextView

    private val tabs: List<TextView> by lazy { listOf(tabAll, tabPending, tabNotifications) }

    private enum class TabMode { ALL, PENDING, NOTIFICATIONS }
    private var currentTab = TabMode.PENDING

    private var isLoading = false
    private var activeLoadTasks = 0

    companion object {
        const val PAGE_SIZE = 50
        fun newInstance(): InvitesFragment = InvitesFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_invites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupToolbar()
        setupTabs()
        setupRecyclerView()
        loadAllData()
    }

    override fun onResume() {
        super.onResume()
        if (::recyclerInvites.isInitialized) {
            refreshCurrentTab()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        InviteDataCache.clearAll()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_invites)
        recyclerInvites = view.findViewById(R.id.recycler_invites)
        progressLoading = view.findViewById(R.id.progress_loading)
        tvEmpty = view.findViewById(R.id.tv_empty)
        tabAll = view.findViewById(R.id.tab_all)
        tabPending = view.findViewById(R.id.tab_pending)
        tabNotifications = view.findViewById(R.id.tab_notifications)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupTabs() {
        tabAll.setOnClickListener { switchTab(TabMode.ALL) }
        tabPending.setOnClickListener { switchTab(TabMode.PENDING) }
        tabNotifications.setOnClickListener { switchTab(TabMode.NOTIFICATIONS) }
        updateTabUI(TabMode.PENDING)
    }

    private fun switchTab(target: TabMode) {
        if (currentTab == target) return
        animateTabSwitch(currentTab, target)
        currentTab = target
        refreshCurrentTab()
    }

    private fun animateTabSwitch(from: TabMode, to: TabMode) {
        updateTabUI(to)
    }

    private fun updateTabUI(selected: TabMode) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = when (selected) {
                TabMode.ALL -> index == 0
                TabMode.PENDING -> index == 1
                TabMode.NOTIFICATIONS -> index == 2
            }
            if (isSelected) {
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_selected_text))
                tab.textSize = 14f
                tab.setTypeface(null, android.graphics.Typeface.BOLD)
                tab.setBackgroundResource(R.drawable.bg_chip_selected)
            } else {
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_unselected_text))
                tab.textSize = 14f
                tab.setTypeface(null, android.graphics.Typeface.NORMAL)
                val tv = android.util.TypedValue()
                requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
                tab.setBackgroundResource(tv.resourceId)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = UnifiedRecordAdapter(emptyList()) { item, action ->
            handleRecordAction(item, action)
        }
        recyclerInvites.layoutManager = LinearLayoutManager(requireContext())
        recyclerInvites.adapter = adapter
    }

    private fun loadAllData(forceRefresh: Boolean = false) {
        showLoading()

        val needReceived = forceRefresh || InviteDataCache.isReceivedExpired() || InviteDataCache.isReceivedEmpty()
        val needSent = forceRefresh || InviteDataCache.isSentExpired() || InviteDataCache.isSentEmpty()
        val needJoinRequests = forceRefresh || InviteDataCache.isJoinRequestsExpired() || InviteDataCache.isJoinRequestsEmpty()

        activeLoadTasks = 0
        if (needReceived) { activeLoadTasks++; loadReceivedInvites() }
        if (needSent) { activeLoadTasks++; loadSentInvites() }
        if (needJoinRequests) { activeLoadTasks++; loadJoinRequests() }

        if (activeLoadTasks == 0) onDataReady()
    }

    private fun refreshCurrentTab() {
        loadAllData(forceRefresh = true)
    }

    private fun loadReceivedInvites() {
        RetrofitClient.api.getMyInvites(1, PAGE_SIZE)
            .enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                override fun onResponse(
                    call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                    response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                ) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val listData = parseInviteListData(response.body()?.data)
                        if (listData != null) {
                            val items = listData.items.map { data ->
                                UnifiedRecordItem(
                                    id = data.inviteId,
                                    type = RecordType.RECEIVED_INVITE,
                                    title = "来自 ${data.inviterUserId} 的邀请",
                                    groupId = data.groupId,
                                    groupName = data.groupName,
                                    role = data.role,
                                    status = RecordStatus.fromApiStatus(data.status),
                                    rawStatus = data.status,
                                    createdAt = data.createdAt,
                                    expiredAt = data.expiredAt,
                                    inviterUserId = data.inviterUserId,
                                    actionLabel = if (data.status.lowercase() == "pending") "接受/拒绝" else null,
                                    extraData = mapOf(
                                        UnifiedRecordItem.EXTRA_GROUP_ID to data.groupId,
                                        UnifiedRecordItem.EXTRA_INVITE_ID to data.inviteId,
                                        UnifiedRecordItem.EXTRA_INVITER_USER_ID to data.inviterUserId
                                    )
                                )
                            }
                            InviteDataCache.setReceivedInvites(items, listData.total)
                        }
                    }
                    onTaskComplete()
                }

                override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                    onTaskComplete()
                }
            })
    }

    private fun loadSentInvites() {
        RetrofitClient.api.getGroups(1, 100, null)
            .enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                override fun onResponse(
                    call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                    response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                ) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val groupListData = parseGroupListData(response.body()?.data)
                        if (groupListData != null && groupListData.items.isNotEmpty()) {
                            loadInvitesForEachGroup(groupListData.items)
                        } else {
                            InviteDataCache.setSentInvites(emptyList(), 0)
                            onTaskComplete()
                        }
                    } else {
                        InviteDataCache.setSentInvites(emptyList(), 0)
                        onTaskComplete()
                    }
                }

                override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                    onTaskComplete()
                }
            })
    }

    private fun loadInvitesForEachGroup(groups: List<GroupData>) {
        var completedCount = 0
        val totalGroups = groups.size
        val allSentItems = mutableListOf<UnifiedRecordItem>()

        groups.forEach { group ->
            RetrofitClient.api.getInvites(group.id, 1, 50, null)
                .enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                    override fun onResponse(
                        call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                        response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                    ) {
                        completedCount++
                        if (response.isSuccessful && response.body()?.code == 0) {
                            val items = parseSentInviteItems(response.body()?.data, group.id, group.name)
                            allSentItems.addAll(items)
                        }
                        if (completedCount >= totalGroups) {
                            InviteDataCache.setSentInvites(allSentItems.sortedByDescending { it.createdAt }, allSentItems.size)
                            onTaskComplete()
                        }
                    }

                    override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                        completedCount++
                        if (completedCount >= totalGroups) {
                            InviteDataCache.setSentInvites(allSentItems.sortedByDescending { it.createdAt }, allSentItems.size)
                            onTaskComplete()
                        }
                    }
                })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseSentInviteItems(data: Any?, groupId: String, groupName: String): List<UnifiedRecordItem> {
        if (data == null) return emptyList()
        return try {
            val map = data as? Map<String, Any?> ?: return emptyList()
            val items = map["items"] as? List<*> ?: return emptyList()
            items.mapNotNull { item ->
                val itemMap = item as? Map<String, Any?> ?: return@mapNotNull null
                val inviteId = itemMap["id"] as? String ?: return@mapNotNull null
                val status = itemMap["status"] as? String ?: "unknown"
                val role = itemMap["role"] as? String ?: ""
                val createdAt = itemMap["createdAt"] as? String ?: ""
                val inviteeEmail = itemMap["inviteeEmail"] as? String
                val inviteeUserId = itemMap["inviteeUserId"] as? String

                UnifiedRecordItem(
                    id = "${groupId}_$inviteId",
                    type = RecordType.SENT_INVITE,
                    title = inviteeEmail ?: inviteeUserId ?: "未知用户",
                    groupId = groupId,
                    groupName = groupName,
                    role = role.ifEmpty { null },
                    status = RecordStatus.fromApiStatus(status),
                    rawStatus = status,
                    createdAt = createdAt,
                    actionLabel = if (status.lowercase() == "pending") "撤销" else null,
                    extraData = mapOf(
                        UnifiedRecordItem.EXTRA_GROUP_ID to groupId,
                        UnifiedRecordItem.EXTRA_INVITE_ID to inviteId
                    )
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun loadJoinRequests() {
        RetrofitClient.api.getMyJoinRequests(1, PAGE_SIZE)
            .enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                override fun onResponse(
                    call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                    response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                ) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val items = parseJoinRequestItems(response.body()?.data)
                        InviteDataCache.setJoinRequests(items, items.size)
                    }
                    onTaskComplete()
                }

                override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                    onTaskComplete()
                }
            })
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJoinRequestItems(data: Any?): List<UnifiedRecordItem> {
        if (data == null) return emptyList()
        return try {
            val map = data as? Map<String, Any?> ?: return emptyList()
            val items = map["items"] as? List<*> ?: return emptyList()
            items.mapNotNull { item ->
                val itemMap = item as? Map<String, Any?> ?: return@mapNotNull null
                val requestId = itemMap["id"] as? String ?: return@mapNotNull null
                val status = itemMap["status"] as? String ?: "unknown"
                val groupId = itemMap["groupId"] as? String ?: ""
                val groupName = itemMap["groupName"] as? String ?: ""
                val createdAt = itemMap["createdAt"] as? String ?: ""

                UnifiedRecordItem(
                    id = "jr_$requestId",
                    type = RecordType.JOIN_REQUEST,
                    title = "申请加入「$groupName」",
                    groupId = groupId,
                    groupName = groupName,
                    role = null,
                    status = RecordStatus.fromApiStatus(status),
                    rawStatus = status,
                    createdAt = createdAt,
                    actionLabel = null,
                    extraData = mapOf(
                        UnifiedRecordItem.EXTRA_GROUP_ID to groupId,
                        UnifiedRecordItem.EXTRA_REQUEST_ID to requestId
                    )
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    @Synchronized
    private fun onTaskComplete() {
        activeLoadTasks--
        if (activeLoadTasks <= 0) {
            activeLoadTasks = 0
            onDataReady()
        }
    }

    private fun onDataReady() {
        hideLoading()
        displayCurrentTab()
    }

    private fun displayCurrentTab() {
        val data = when (currentTab) {
            TabMode.ALL -> InviteDataCache.getAllMerged()
            TabMode.PENDING -> InviteDataCache.getPendingRecords()
            TabMode.NOTIFICATIONS -> InviteDataCache.getReceivedInvites() + InviteDataCache.getJoinRequests()
        }.sortedByDescending { it.createdAt }

        if (data.isEmpty()) {
            showEmptyState()
        } else {
            showData(data)
        }
    }

    private fun showLoading() {
        isLoading = true
        progressLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        recyclerInvites.visibility = View.GONE
    }

    private fun hideLoading() {
        isLoading = false
        progressLoading.visibility = View.GONE
    }

    private fun showEmptyState() {
        val emptyMessage = when (currentTab) {
            TabMode.ALL -> "暂无任何记录"
            TabMode.PENDING -> "暂无待处理的记录"
            TabMode.NOTIFICATIONS -> "暂无新通知"
        }
        tvEmpty.text = emptyMessage
        tvEmpty.visibility = View.VISIBLE
        recyclerInvites.visibility = View.GONE
    }

    private fun showData(items: List<UnifiedRecordItem>) {
        tvEmpty.visibility = View.GONE
        recyclerInvites.visibility = View.VISIBLE
        adapter.updateData(items)
    }

    private fun handleRecordAction(item: UnifiedRecordItem, action: String) {
        when (item.type) {
            RecordType.RECEIVED_INVITE -> handleReceivedInviteAction(item, action)
            RecordType.SENT_INVITE -> handleSentInviteAction(item, action)
            RecordType.JOIN_REQUEST -> { }
        }
    }

    private fun handleReceivedInviteAction(item: UnifiedRecordItem, action: String) {
        val inviteToken = item.extraData[UnifiedRecordItem.EXTRA_INVITE_ID] ?: return
        when (action) {
            "accept" -> {
                RetrofitClient.api.acceptInvite(inviteToken)
                    .enqueue(createSimpleCallback("已接受邀请", item.id, RecordStatus.ACCEPTED))
            }
            "reject" -> {
                RetrofitClient.api.rejectInvite(inviteToken)
                    .enqueue(createSimpleCallback("已拒绝邀请", item.id, RecordStatus.REJECTED))
            }
            "cancel" -> {
                val groupId = item.extraData[UnifiedRecordItem.EXTRA_GROUP_ID] ?: return
                RetrofitClient.api.cancelInvite(groupId, inviteToken)
                    .enqueue(createSimpleCallback("已撤销邀请", item.id, RecordStatus.CANCELLED))
            }
        }
    }

    private fun handleSentInviteAction(item: UnifiedRecordItem, action: String) {
        if (action == "cancel") {
            val groupId = item.extraData[UnifiedRecordItem.EXTRA_GROUP_ID] ?: return
            val inviteId = item.extraData[UnifiedRecordItem.EXTRA_INVITE_ID] ?: return
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("撤销邀请")
                .setMessage("确定要撤销对「${item.title}」的邀请吗？")
                .setPositiveButton("撤销") { _, _ ->
                    RetrofitClient.api.cancelInvite(groupId, inviteId)
                        .enqueue(createSimpleCallback("邀请已撤销", item.id, RecordStatus.CANCELLED))
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun createSimpleCallback(successMsg: String, recordId: String, newStatus: RecordStatus): Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
        return object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
            override fun onResponse(
                call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
            ) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show()
                    InviteDataCache.updateRecordStatus(recordId, newStatus)
                    displayCurrentTab()
                } else {
                    Toast.makeText(requireContext(), "操作失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parseInviteListData(data: Any?): InviteListData? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<InviteListData>() {}.type)
        } catch (_: Exception) { null }
    }

    private fun parseGroupListData(data: Any?): GroupListData? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<GroupListData>() {}.type)
        } catch (_: Exception) { null }
    }

    inner class UnifiedRecordAdapter(
        private var items: List<UnifiedRecordItem>,
        private val onActionClick: (UnifiedRecordItem, String) -> Unit
    ) : RecyclerView.Adapter<UnifiedRecordAdapter.RecordViewHolder>() {

        inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTypeBadge: TextView = itemView.findViewById(R.id.tv_type_badge)
            val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
            val tvGroupName: TextView = itemView.findViewById(R.id.tv_group_name)
            val tvRole: TextView = itemView.findViewById(R.id.tv_role)
            val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
            val tvTime: TextView = itemView.findViewById(R.id.tv_time)
            val btnAction: TextView = itemView.findViewById(R.id.btn_action)

            fun bind(item: UnifiedRecordItem, position: Int) {
                tvTypeBadge.text = item.type.displayName
                tvTypeBadge.setBackgroundColor(item.type.badgeColor)

                tvTitle.text = item.title
                tvGroupName.text = item.groupName ?: ""
                tvRole.text = item.role?.let { role ->
                    when (role.lowercase(Locale.getDefault())) {
                        "owner" -> "群主"
                        "editor" -> "编辑者"
                        "viewer" -> "查看者"
                        else -> role
                    }
                } ?: ""
                tvRole.visibility = if (item.role != null) View.VISIBLE else View.GONE

                tvStatus.text = item.status.displayText
                tvStatus.setTextColor(item.status.color)

                tvTime.text = formatTime(item.createdAt)

                when {
                    item.isPendingActionable && item.type == RecordType.RECEIVED_INVITE -> {
                        btnAction.visibility = View.VISIBLE
                        btnAction.text = "查看详情"
                        btnAction.setOnClickListener { showReceiveInviteActions(item) }
                    }
                    item.isPendingActionable && item.type == RecordType.SENT_INVITE -> {
                        btnAction.visibility = View.VISIBLE
                        btnAction.text = item.actionLabel ?: "撤销"
                        btnAction.setOnClickListener { onActionClick(item, "cancel") }
                    }
                    else -> {
                        btnAction.visibility = View.GONE
                    }
                }

                itemView.setOnClickListener {
                    when (item.type) {
                        RecordType.RECEIVED_INVITE -> showReceiveInviteActions(item)
                        RecordType.SENT_INVITE -> { if (item.isPendingActionable) onActionClick(item, "cancel") }
                        RecordType.JOIN_REQUEST -> { }
                    }
                }
            }
        }

        private fun showReceiveInviteActions(item: UnifiedRecordItem) {
            val options = arrayOf("接受邀请", "拒绝邀请")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("处理邀请")
                .setMessage("${item.title}\n群组：${item.groupName}\n角色：${item.role ?: "-"}")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> onActionClick(item, "accept")
                        1 -> onActionClick(item, "reject")
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        private fun formatTime(timeStr: String): String {
            return try {
                timeStr.substringBeforeLast(".").substring(0, minOf(timeStr.length, 16))
            } catch (_: Exception) { timeStr }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_invite, parent, false)
            return RecordViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
            holder.bind(items[position], position)
        }

        override fun getItemCount(): Int = items.size

        fun updateData(newItems: List<UnifiedRecordItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
