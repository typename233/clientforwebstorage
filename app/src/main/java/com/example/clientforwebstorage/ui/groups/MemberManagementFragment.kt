package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.network.models.AddMembersRequest
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.MemberInfo
import com.example.clientforwebstorage.network.models.MemberListResponse
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MemberManagementFragment : Fragment() {

    private lateinit var conversationId: String
    private lateinit var conversationName: String
    private var currentUserRole: String = "member"
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var tvMemberCount: TextView
    private lateinit var recyclerMembers: androidx.recyclerview.widget.RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var fabAddMember: FloatingActionButton
    
    private lateinit var adapter: MemberListAdapter
    private var allMembers: List<MemberInfo> = emptyList()
    private var currentFilter: String = "all"
    private var searchKeyword: String = ""
    
    companion object {
        private const val ARG_CONVERSATION_ID = "conversation_id"
        private const val ARG_CONVERSATION_NAME = "conversation_name"
        private const val ARG_USER_ROLE = "user_role"

        fun newInstance(conversationId: String, conversationName: String, userRole: String): MemberManagementFragment {
            return MemberManagementFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONVERSATION_ID, conversationId)
                    putString(ARG_CONVERSATION_NAME, conversationName)
                    putString(ARG_USER_ROLE, userRole)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationId = it.getString(ARG_CONVERSATION_ID) ?: ""
            conversationName = it.getString(ARG_CONVERSATION_NAME) ?: ""
            currentUserRole = it.getString(ARG_USER_ROLE, "member")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_member_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        setupSearch()
        setupFilters()
        setupRecyclerView()
        setupFabButton()
        
        loadMembers()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_member_management)
        etSearch = view.findViewById(R.id.et_search_member)
        btnClearSearch = view.findViewById(R.id.btn_clear_search)
        tvMemberCount = view.findViewById(R.id.tv_member_count)
        recyclerMembers = view.findViewById(R.id.recycler_members)
        progressLoading = view.findViewById(R.id.progress_loading)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        fabAddMember = view.findViewById(R.id.fab_add_member)
    }

    private fun setupToolbar() {
        toolbar.title = "$conversationName - 成员管理"
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        if (currentUserRole != "owner" && currentUserRole != "admin") {
            fabAddMember.visibility = View.GONE
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchKeyword = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (searchKeyword.isNotEmpty()) View.VISIBLE else View.GONE
                applyFiltersAndSearch()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
        }
    }

    private fun setupFilters() {
        val chipAll = view?.findViewById<Chip>(R.id.chip_all)
        val chipOwner = view?.findViewById<Chip>(R.id.chip_owner)
        val chipAdmin = view?.findViewById<Chip>(R.id.chip_admin)
        val chipMember = view?.findViewById<Chip>(R.id.chip_member)

        val chips = listOf(chipAll, chipOwner, chipAdmin, chipMember)
        
        chips.forEach { chip ->
            chip?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    currentFilter = when (chip.id) {
                        R.id.chip_all -> "all"
                        R.id.chip_owner -> "owner"
                        R.id.chip_admin -> "admin"
                        R.id.chip_member -> "member"
                        else -> "all"
                    }
                    
                    chips.filter { it != chip }.forEach { it?.isChecked = false }
                    applyFiltersAndSearch()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerMembers.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = MemberListAdapter(
            members = emptyList(),
            currentUserId = TokenManager.getUserId(),
            currentUserRole = currentUserRole,
            onRemoveMember = { member -> handleRemoveMember(member) },
            onSetAdmin = { member -> handleSetAdmin(member) },
            onRemoveAdmin = { member -> handleRemoveAdmin(member) },
            onTransferOwnership = { member -> handleTransferOwnership(member) }
        )
        
        recyclerMembers.adapter = adapter
    }

    private fun setupFabButton() {
        fabAddMember.setOnClickListener {
            showAddMemberDialog()
        }
    }

    private fun loadMembers() {
        showLoading(true)
        
        android.util.Log.d("MemberManagement", "=== 开始加载成员列表 ===")
        android.util.Log.d("MemberManagement", "conversationId: $conversationId")

        RetrofitClient.api.listMembers(conversationId, 1, 100)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse

                    showLoading(false)

                    android.util.Log.d("MemberManagement", "=== 成员列表响应 ===")
                    android.util.Log.d("MemberManagement", "isSuccessful: ${response.isSuccessful}")
                    android.util.Log.d("MemberManagement", "code: ${response.body()?.code}")
                    android.util.Log.d("MemberManagement", "message: ${response.body()?.message}")

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val memberListData = parseMemberListResponse(response.body()?.data)

                        if (memberListData != null) {
                            android.util.Log.d("MemberManagement", "解析成功 - total: ${memberListData.total}, items: ${memberListData.items.size}")
                            memberListData.items.forEachIndexed { index, member ->
                                android.util.Log.d("MemberManagement", "成员[$index]: ${member.nickname} (${member.role})")
                            }

                            allMembers = memberListData.items
                            android.util.Log.d("MemberManagement", "allMembers已更新，大小: ${allMembers.size}")

                            updateUIWithMembers()
                            
                            Toast.makeText(requireContext(),
                                "成功加载 ${memberListData.total} 位成员",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            android.util.Log.e("MemberManagement", "解析成员数据失败！data: ${Gson().toJson(response.body()?.data)}")
                            showError("解析成员数据失败")
                            allMembers = emptyList()
                            updateUIWithMembers()
                        }
                    } else {
                        android.util.Log.e("MemberManagement", "加载成员列表API返回错误")
                        showError("加载成员失败: ${response.body()?.message}")
                        allMembers = emptyList()
                        updateUIWithMembers()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    android.util.Log.e("MemberManagement", "加载成员列表网络错误", t)
                    showError("网络错误: ${t.message}")
                    allMembers = emptyList()
                    updateUIWithMembers()
                }
            })
    }

    private fun parseMemberListResponse(data: Any?): MemberListResponse? {
        if (data == null) {
            android.util.Log.e("MemberManagement", "parseMemberListResponse: data为null")
            return null
        }

        return try {
            val json = Gson().toJson(data)
            android.util.Log.d("MemberManagement", "原始JSON数据: $json")

            val result = Gson().fromJson(json, object : TypeToken<MemberListResponse>() {}.type) as MemberListResponse?
            android.util.Log.d("MemberManagement", "解析后的MemberListResponse: total=${result?.total}, items=${result?.items?.size}")
            result
        } catch (e: Exception) {
            android.util.Log.e("MemberManagement", "解析成员列表异常", e)
            null
        }
    }

    private fun applyFiltersAndSearch() {
        android.util.Log.d("MemberManagement", "=== applyFiltersAndSearch ===")
        android.util.Log.d("MemberManagement", "allMembers.size: ${allMembers.size}")
        android.util.Log.d("MemberManagement", "searchKeyword: '$searchKeyword', currentFilter: '$currentFilter'")

        var filteredMembers = allMembers.toList()

        if (searchKeyword.isNotBlank()) {
            filteredMembers = filteredMembers.filter {
                it.nickname.contains(searchKeyword, ignoreCase = true) ||
                it.userId.contains(searchKeyword, ignoreCase = true)
            }
        }

        if (currentFilter.isNotBlank() && currentFilter != "all") {
            filteredMembers = when (currentFilter.lowercase()) {
                "owner" -> filteredMembers.filter { it.role.lowercase() == "owner" }
                "admin" -> filteredMembers.filter { it.role.lowercase() in listOf("admin", "editor") }
                "member" -> filteredMembers.filter { it.role.lowercase() !in listOf("owner", "admin", "editor") }
                else -> filteredMembers
            }
        }

        android.util.Log.d("MemberManagement", "filteredMembers.size: ${filteredMembers.size}")

        adapter.updateData(filteredMembers)
        tvMemberCount.text = "共 ${filteredMembers.size} 位成员"

        layoutEmpty.visibility = if (filteredMembers.isEmpty() && allMembers.isNotEmpty())
            View.VISIBLE else View.GONE
    }

    private fun updateUIWithMembers() {
        applyFiltersAndSearch()
    }

    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        recyclerMembers.visibility = if (show) View.GONE else View.VISIBLE
        
        if (show) {
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showAddMemberDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_invite_member, null)

        val etEmail = dialogView.findViewById<EditText>(R.id.et_email)
        val rgRole = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_role)
        val tvAdminHint = dialogView.findViewById<TextView>(R.id.tv_admin_hint)
        val rbMember = dialogView.findViewById<android.widget.RadioButton>(R.id.rb_member)
        val rbAdmin = dialogView.findViewById<android.widget.RadioButton>(R.id.rb_admin)

        rgRole.setOnCheckedChangeListener { _, checkedId ->
            tvAdminHint.visibility = if (checkedId == R.id.rb_admin) View.VISIBLE else View.GONE
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val emails = etEmail.text?.toString()?.trim()
                    ?.split(",|;|\\s".toRegex())
                    ?.filter { it.isNotBlank() } ?: emptyList()

                if (emails.isEmpty()) {
                    Toast.makeText(requireContext(), "请输入被邀请人邮箱", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val setAsAdmin = rbAdmin.isChecked

                android.util.Log.d("MemberManagement", "=== 添加成员请求 ===")
                android.util.Log.d("MemberManagement", "emails: $emails")
                android.util.Log.d("MemberManagement", "setAsAdmin: $setAsAdmin")

                addMembersWithEmail(emails, setAsAdmin)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addMembersWithEmail(
        emails: List<String>,
        setAsAdmin: Boolean
    ) {
        showLoading(true)

        val request = AddMembersRequest(
            emails = emails
        )

        RetrofitClient.api.addMembers(conversationId, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse

                    if (response.isSuccessful && response.body()?.code == 0) {
                        android.util.Log.d("MemberManagement", "✅ 成员添加成功")

                        if (setAsAdmin && emails.isNotEmpty()) {
                            android.util.Log.d("MemberManagement", "需要设置管理员，开始查询新成员...")
                            queryNewMembersAndSetAdmin(emails, emptyList(), emptyList())
                        } else {
                            showLoading(false)
                            Toast.makeText(requireContext(), "成员添加成功", Toast.LENGTH_SHORT).show()
                            loadMembers()
                        }
                    } else {
                        showLoading(false)
                        showError("添加成员失败: ${response.body()?.message}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    showError("网络错误: ${t.message}")
                }
            })
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryNewMembersAndSetAdmin(
        emails: List<String>,
        phones: List<String>,
        userIds: List<String>
    ) {
        RetrofitClient.api.listMembers(conversationId, 1, 100)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val memberListData = parseMemberListResponse(response.body()?.data)

                        if (memberListData != null) {
                            val currentMemberIds = allMembers.map { it.userId }.toSet()
                            val newMembers: List<MemberInfo> = memberListData.items.filter { it.userId !in currentMemberIds }

                            if (newMembers.isNotEmpty()) {
                                newMembers.forEach { newMember ->
                                    RetrofitClient.api.setAdmin(conversationId,
                                        com.example.clientforwebstorage.network.models.SetAdminRequest(userId = newMember.userId))
                                        .enqueue(object : Callback<ApiResponse> {
                                            override fun onResponse(
                                                call: Call<ApiResponse>,
                                                response: Response<ApiResponse>
                                            ) {
                                                if (response.isSuccessful && response.body()?.code == 0) {
                                                    android.util.Log.d("MemberManagement", "✅ 管理员设置成功: ${newMember.userId}")
                                                } else {
                                                    android.util.Log.e("MemberManagement",
                                                        "❌ 设置管理员失败: ${response.body()?.message}")
                                                }
                                            }

                                            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                                android.util.Log.e("MemberManagement", "设置管理员网络错误", t)
                                            }
                                        })
                                }
                                Toast.makeText(requireContext(),
                                    "已添加 ${newMembers.size} 位成员并设为管理员", Toast.LENGTH_SHORT).show()
                                loadMembers()
                            } else {
                                android.util.Log.w("MemberManagement",
                                    "未找到新成员，可能通过邮箱添加的用户需要手动匹配")
                                Toast.makeText(requireContext(), "成员已添加，但未找到新成员ID来设置管理员", Toast.LENGTH_SHORT).show()
                                loadMembers()
                            }
                        } else {
                            showError("解析成员数据失败")
                            loadMembers()
                        }
                    } else {
                        showError("查询成员列表失败")
                        loadMembers()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    showError("网络错误: ${t.message}")
                }
            })
    }

    private fun handleRemoveMember(member: MemberInfo) {
        val isCurrentUser = member.userId == TokenManager.getUserId()

        if (isCurrentUser) {
            leaveConversation()
        } else {
            removeMember(member)
        }
    }

    private fun removeMember(member: MemberInfo) {
        showLoading(true)

        RetrofitClient.api.removeConversationMember(conversationId, member.userId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), 
                            "已将「${member.nickname}」移出群组", Toast.LENGTH_SHORT).show()
                        loadMembers()
                    } else {
                        val errorMsg = response.body()?.message ?: "移除失败"
                        if (errorMsg.contains("权限", ignoreCase = true)) {
                            showError("权限不足：您没有权限移除该成员")
                        } else {
                            showError("移除成员失败: $errorMsg")
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    showError("网络错误: ${t.message}")
                }
            })
    }

    private fun leaveConversation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("退出群组")
            .setMessage("确定要退出「$conversationName」吗？\n\n如果是群主，退出将解散该群组。")
            .setPositiveButton("确认退出") { _, _ ->
                performLeaveOrDissolve()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performLeaveOrDissolve() {
        showLoading(true)

        if (currentUserRole == "owner") {
            dissolveGroup()
        } else {
            leaveGroup()
        }
    }

    private fun leaveGroup() {
        RetrofitClient.api.leaveConversation(conversationId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已退出群组", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        showError("退出群组失败: ${response.body()?.message}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    showError("网络错误: ${t.message}")
                }
            })
    }

    private fun dissolveGroup() {
        RetrofitClient.api.dissolveConversation(conversationId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "群组已解散", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        showError("解散群组失败: ${response.body()?.message}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    showError("网络错误: ${t.message}")
                }
            })
    }

    private fun handleSetAdmin(member: MemberInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("设置管理员")
            .setMessage("确定要将「${member.nickname}」设为管理员吗？\n\n管理员可以管理群组成员、移除成员等。")
            .setPositiveButton("确认设置") { _, _ ->
                setAdmin(member.userId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setAdmin(userId: String) {
        showLoading(true)

        RetrofitClient.api.setAdmin(conversationId, 
            com.example.clientforwebstorage.network.models.SetAdminRequest(userId = userId))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "管理员设置成功", Toast.LENGTH_SHORT).show()
                        loadMembers()
                    } else {
                        val errorMsg = response.body()?.message ?: ""
                        if (errorMsg.contains("权限", ignoreCase = true)) {
                            showError("权限不足：只有群主可以设置管理员")
                        } else {
                            showError("设置管理员失败: $errorMsg")
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    showError("网络错误: ${t.message}")
                }
            })
    }

    private fun handleRemoveAdmin(member: MemberInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("取消管理员")
            .setMessage("确定要取消「${member.nickname}」的管理员身份吗？")
            .setPositiveButton("确认取消") { _, _ ->
                removeAdmin(member.userId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun removeAdmin(userId: String) {
        showLoading(true)

        RetrofitClient.api.removeAdmin(conversationId, userId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已取消管理员身份", Toast.LENGTH_SHORT).show()
                        loadMembers()
                    } else {
                        val errorMsg = response.body()?.message ?: ""
                        if (errorMsg.contains("权限", ignoreCase = true)) {
                            showError("权限不足：只有群主可以取消管理员")
                        } else {
                            showError("取消管理员失败: $errorMsg")
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    showError("网络错误: ${t.message}")
                }
            })
    }

    private fun handleTransferOwnership(member: MemberInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("转让群主")
            .setMessage("确定要将「$conversationName」的群主身份转让给「${member.nickname}」吗？\n\n转让后您将变为普通成员。此操作不可撤销！")
            .setPositiveButton("确认转让") { _, _ ->
                transferOwnership(member.userId, member.nickname)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun transferOwnership(newOwnerId: String, newOwnerName: String) {
        showLoading(true)

        RetrofitClient.api.transferOwnership(conversationId,
            com.example.clientforwebstorage.network.models.TransferOwnershipRequest(userId = newOwnerId))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), 
                            "已成功将群主身份转让给「$newOwnerName」", Toast.LENGTH_SHORT).show()
                        currentUserRole = "member"
                        loadMembers()
                    } else {
                        val errorMsg = response.body()?.message ?: ""
                        if (errorMsg.contains("权限", ignoreCase = true)) {
                            showError("权限不足：只有当前群主可以转让群主身份")
                        } else {
                            showError("转让群主失败: $errorMsg")
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    showLoading(false)
                    showError("网络错误: ${t.message}")
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (::recyclerMembers.isInitialized) {
            loadMembers()
        }
    }
}
