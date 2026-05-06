package com.example.clientforwebstorage.ui.groups

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
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class InviteItem(
    val inviteId: String,
    val groupId: String,
    val groupName: String,
    val inviteeEmail: String?,
    val inviteeUserId: String?,
    val role: String,
    val status: String,
    val createdAt: String?
)

class InvitesFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerInvites: RecyclerView
    private lateinit var adapter: InviteAdapter
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tabPending: TextView
    private lateinit var tabAll: TextView

    private val allInvites = mutableListOf<InviteItem>()
    private var currentFilter: String? = null

    companion object {
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
        loadAllInvites()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_invites)
        recyclerInvites = view.findViewById(R.id.recycler_invites)
        progressLoading = view.findViewById(R.id.progress_loading)
        tvEmpty = view.findViewById(R.id.tv_empty)
        tabPending = view.findViewById(R.id.tab_pending)
        tabAll = view.findViewById(R.id.tab_all)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupTabs() {
        tabPending.setOnClickListener {
            selectTab(tabPending, tabAll)
            currentFilter = "pending"
            filterAndDisplay()
        }
        tabAll.setOnClickListener {
            selectTab(tabAll, tabPending)
            currentFilter = null
            filterAndDisplay()
        }
    }

    private fun selectTab(selected: TextView, unselected: TextView) {
        selected.setTextColor(resources.getColor(android.R.color.white, null))
        selected.textSize = 14f
        selected.setBackgroundResource(R.drawable.bg_chip_selected)
        unselected.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        val outValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        unselected.setBackgroundResource(outValue.resourceId)
    }

    private fun setupRecyclerView() {
        adapter = InviteAdapter(allInvites) { invite, position ->
            cancelInvite(invite, position)
        }
        recyclerInvites.layoutManager = LinearLayoutManager(requireContext())
        recyclerInvites.adapter = adapter
    }

    private fun loadAllInvites() {
        progressLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        recyclerInvites.visibility = View.GONE

        RetrofitClient.api.getGroups(1, 100, null)
            .enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                override fun onResponse(
                    call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                    response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                ) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val groupListData = parseGroupListData(response.body()?.data)
                        if (groupListData != null && groupListData.items.isNotEmpty()) {
                            loadInvitesForGroups(groupListData.items)
                        } else {
                            showEmpty()
                        }
                    } else {
                        showError("加载群组列表失败")
                    }
                }

                override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                    showError("网络错误: ${t.message}")
                }
            })
    }

    private fun loadInvitesForGroups(groups: List<com.example.clientforwebstorage.network.models.GroupData>) {
        var completedCount = 0
        val totalGroups = groups.size

        groups.forEach { group ->
            RetrofitClient.api.getInvites(group.id, 1, 50, null)
                .enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                    override fun onResponse(
                        call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                        response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                    ) {
                        completedCount++
                        if (response.isSuccessful && response.body()?.code == 0) {
                            val invites = parseInviteList(response.body()?.data, group.id, group.name)
                            allInvites.addAll(invites)
                        }

                        if (completedCount >= totalGroups) {
                            onAllInvitesLoaded()
                        }
                    }

                    override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                        completedCount++
                        if (completedCount >= totalGroups) {
                            onAllInvitesLoaded()
                        }
                    }
                })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseInviteList(data: Any?, groupId: String, groupName: String): List<InviteItem> {
        if (data == null) return emptyList()
        return try {
            val map = data as? Map<String, Any?> ?: return emptyList()
            val items = map["items"] as? List<*> ?: return emptyList()
            items.mapNotNull { item ->
                val itemMap = item as? Map<String, Any?> ?: return@mapNotNull null
                InviteItem(
                    inviteId = itemMap["id"] as? String ?: "",
                    groupId = groupId,
                    groupName = groupName,
                    inviteeEmail = itemMap["inviteeEmail"] as? String,
                    inviteeUserId = itemMap["inviteeUserId"] as? String,
                    role = when (itemMap["role"] as? String) {
                        "editor" -> "编辑者"
                        "viewer" -> "查看者"
                        else -> "成员"
                    },
                    status = when (itemMap["status"] as? String) {
                        "pending" -> "待处理"
                        "accepted" -> "已接受"
                        "rejected" -> "已拒绝"
                        "expired" -> "已过期"
                        "cancelled" -> "已撤销"
                        else -> "未知"
                    },
                    createdAt = itemMap["createdAt"] as? String
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseGroupListData(data: Any?): com.example.clientforwebstorage.network.models.GroupListData? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<com.example.clientforwebstorage.network.models.GroupListData>() {}.type)
        } catch (_: Exception) { null }
    }

    private fun onAllInvitesLoaded() {
        progressLoading.visibility = View.GONE
        filterAndDisplay()
    }

    private fun filterAndDisplay() {
        val filtered = if (currentFilter == "pending") {
            allInvites.filter { it.status == "待处理" }
        } else {
            allInvites
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerInvites.visibility = View.GONE
            tvEmpty.text = if (currentFilter == "pending") "暂无待处理的邀请" else "暂无邀请记录"
        } else {
            tvEmpty.visibility = View.GONE
            recyclerInvites.visibility = View.VISIBLE
            adapter.updateData(filtered)
        }
    }

    private fun showEmpty() {
        progressLoading.visibility = View.GONE
        tvEmpty.visibility = View.VISIBLE
        recyclerInvites.visibility = View.GONE
    }

    private fun cancelInvite(invite: InviteItem, position: Int) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("撤销邀请")
            .setMessage("确定要撤销对 ${invite.inviteeEmail ?: invite.inviteeUserId} 的邀请吗？")
            .setPositiveButton("撤销") { _, _ ->
                doCancelInvite(invite, position)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doCancelInvite(invite: InviteItem, position: Int) {
        RetrofitClient.api.cancelInvite(invite.groupId, invite.inviteId)
            .enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                override fun onResponse(
                    call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                    response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                ) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "邀请已撤销", Toast.LENGTH_SHORT).show()
                        val idx = allInvites.indexOfFirst { it.inviteId == invite.inviteId }
                        if (idx >= 0) {
                            allInvites[idx] = allInvites[idx].copy(status = "已撤销")
                            filterAndDisplay()
                        }
                    } else {
                        Toast.makeText(requireContext(), "撤销失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showError(message: String) {
        progressLoading.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    inner class InviteAdapter(
        private var items: List<InviteItem>,
        private val onCancelClick: (InviteItem, Int) -> Unit
    ) : RecyclerView.Adapter<InviteAdapter.InviteViewHolder>() {

        inner class InviteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvInvitee: TextView = itemView.findViewById(R.id.tv_invitee)
            val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
            val tvGroupName: TextView = itemView.findViewById(R.id.tv_group_name)
            val tvRole: TextView = itemView.findViewById(R.id.tv_role)
            val btnCancel: TextView = itemView.findViewById(R.id.btn_cancel)

            fun bind(invite: InviteItem, position: Int) {
                tvInvitee.text = invite.inviteeEmail ?: invite.inviteeUserId ?: "未知用户"
                tvStatus.text = invite.status
                tvGroupName.text = invite.groupName
                tvRole.text = invite.role

                when (invite.status) {
                    "待处理" -> {
                        tvStatus.setTextColor(0xFF1976D2.toInt())
                        btnCancel.visibility = View.VISIBLE
                    }
                    "已接受" -> {
                        tvStatus.setTextColor(0xFF4CAF50.toInt())
                        btnCancel.visibility = View.GONE
                    }
                    "已拒绝", "已过期", "已撤销" -> {
                        tvStatus.setTextColor(0xFF9E9E9E.toInt())
                        btnCancel.visibility = View.GONE
                    }
                    else -> {
                        tvStatus.setTextColor(0xFF666666.toInt())
                        btnCancel.visibility = View.GONE
                    }
                }

                btnCancel.setOnClickListener { onCancelClick(invite, position) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_invite, parent, false)
            return InviteViewHolder(view)
        }

        override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
            holder.bind(items[position], position)
        }

        override fun getItemCount(): Int = items.size

        fun updateData(newItems: List<InviteItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}
