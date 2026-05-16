package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.ApiService
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.network.models.AddMembersRequest
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.MemberInfo
import com.example.clientforwebstorage.network.models.MemberListResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupDetailFragment : Fragment() {

    private var conversationId: String? = null
    private var conversationName: String = ""
    private var currentUserRole: String = "member"
    private var allMembers: List<MemberInfo> = emptyList()

    private lateinit var apiService: ApiService

    companion object {
        private const val ARG_CONVERSATION_ID = "conversation_id"
        private const val ARG_CONVERSATION_NAME = "conversation_name"

        fun newInstance(conversationId: String, conversationName: String): GroupDetailFragment {
            return GroupDetailFragment().apply {
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
            conversationName = it.getString(ARG_CONVERSATION_NAME, "群组详情")
        }
        apiService = RetrofitClient.api
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_group_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        loadConversationDetail()
        setupActionButtons(view)
    }

    private fun initViews(view: View) {

    }

    private fun setupToolbar() {
        view?.findViewById<MaterialToolbar>(R.id.toolbar_detail)?.apply {
            title = conversationName
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadConversationDetail() {
        conversationId?.let { id ->
            apiService.getConversation(id).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.code == 0) {
                            updateUIWithConversationData(apiResponse.data)
                        } else {
                            showError("加载群组详情失败: ${apiResponse.message}")
                        }
                    } else {
                        showError("网络请求失败")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showError("网络错误: ${t.message}")
                }
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateUIWithConversationData(data: Any?) {
        view?.let { v ->
            try {
                val convData = data as? Map<String, Any?>

                v.findViewById<TextView>(R.id.tv_group_name)?.text =
                    convData?.get("name") as? String ?: conversationName
                v.findViewById<TextView>(R.id.tv_group_id)?.text =
                    "ID: ${convData?.get("id") ?: conversationId}"
                v.findViewById<TextView>(R.id.tv_created_at)?.text =
                    "创建于 ${convData?.get("lastMessageAt") ?: "未知"}"

                val members = convData?.get("members") as? List<*>
                val memberCount = members?.size ?: 0
                v.findViewById<TextView>(R.id.tv_member_count)?.text =
                    memberCount.toString()

                v.findViewById<TextView>(R.id.tv_storage_used)?.text =
                    "N/A"

                v.findViewById<TextView>(R.id.tv_visibility)?.text =
                    when (convData?.get("status") as? String) {
                        "active" -> "公开"
                        else -> "私有"
                    }

                val ownerUserId = convData?.get("ownerUserId") as? String
                val currentUserId = TokenManager.getUserId()
                currentUserRole = when {
                    ownerUserId != null && ownerUserId == currentUserId -> "owner"
                    convData?.get("currentUserRole") != null -> convData?.get("currentUserRole") as? String ?: "member"
                    else -> "member"
                }
                v.findViewById<TextView>(R.id.tv_role)?.text =
                    when {
                        currentUserRole == "owner" -> "群主"
                        currentUserRole == "admin" -> "管理员"
                        else -> "普通成员"
                    }

                v.findViewById<TextView>(R.id.tv_role)?.setBackgroundResource(
                    when {
                        currentUserRole == "owner" -> R.drawable.bg_role_owner
                        currentUserRole == "admin" -> R.drawable.bg_role_admin
                        else -> R.drawable.bg_role_member
                    }
                )

                v.findViewById<TextView>(R.id.tv_role)?.setTextColor(
                    when {
                        currentUserRole == "owner" -> android.graphics.Color.parseColor("#FF9800")
                        currentUserRole == "admin" -> android.graphics.Color.parseColor("#2196F3")
                        else -> android.graphics.Color.parseColor("#4CAF50")
                    }
                )

                allMembers = if (members != null) {
                    members.mapNotNull { member ->
                        val memberMap = member as? Map<String, Any?>
                        val userId = memberMap?.get("userId") as? String
                        val nickname = memberMap?.get("nickname") as? String
                        val role = memberMap?.get("role") as? String
                        if (userId != null && nickname != null && role != null) {
                            MemberInfo(userId, nickname, null, role)
                        } else {
                            null
                        }
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {

            }
        }
    }

    private fun setupActionButtons(view: View) {
        view.findViewById<View>(R.id.btn_invite)?.setOnClickListener {
            showAddMemberDialog()
        }

        view.findViewById<View>(R.id.btn_leave)?.setOnClickListener {
            showLeaveGroupDialog()
        }

        view.findViewById<View>(R.id.card_member_count)?.setOnClickListener {
            navigateToMemberManagement()
        }
    }

    private fun navigateToMemberManagement() {
        val id = conversationId ?: return
        val fragment = MemberManagementFragment.newInstance(id, conversationName, currentUserRole)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("member_management")
            .commit()
    }

    private fun showAddMemberDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_invite_member, null)

        val etEmail = dialogView.findViewById<EditText>(R.id.et_email)
        val rgRole = dialogView.findViewById<RadioGroup>(R.id.rg_role)
        val tvAdminHint = dialogView.findViewById<TextView>(R.id.tv_admin_hint)
        val rbMember = dialogView.findViewById<RadioButton>(R.id.rb_member)
        val rbAdmin = dialogView.findViewById<RadioButton>(R.id.rb_admin)

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

                addMembersWithEmail(emails, setAsAdmin)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addMember(email: String, role: String?) {
        conversationId?.let { id ->
            val request = AddMembersRequest(
                emails = listOf(email),
                role = role
            )

            apiService.addMembers(id, request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.code == 0) {
                            handleAddMemberResponse(apiResponse.data)
                        } else {
                            showError("添加失败: ${apiResponse.message}")
                        }
                    } else {
                        showError("网络请求失败")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showError("网络错误: ${t.message}")
                }
            })
        }
    }

    private fun addMembersWithEmail(
        emails: List<String>,
        setAsAdmin: Boolean
    ) {
        conversationId?.let { id ->
            val request = AddMembersRequest(
                emails = emails
            )

            apiService.addMembers(id, request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.code == 0) {
                            if (setAsAdmin) {
                                queryNewMembersAndSetAdmin(emptyList(), emptyList(), emptyList())
                            } else {
                                handleAddMemberResponse(apiResponse.data)
                            }
                        } else {
                            showError("添加失败: ${apiResponse.message}")
                        }
                    } else {
                        showError("网络请求失败")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showError("网络错误: ${t.message}")
                }
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleAddMemberResponse(data: Any?) {
        try {
            val responseData = data as? Map<String, Any?>
            val addedCount = responseData?.get("addedCount") as? Int ?: 0
            val failedUsers = responseData?.get("failedUsers") as? List<*>

            when {
                addedCount > 0 -> {
                    val message = if (failedUsers != null && failedUsers.isNotEmpty()) {
                        "成功添加 $addedCount 位成员，${failedUsers.size} 位添加失败"
                    } else {
                        "成员已添加"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    loadConversationDetail()
                }
                failedUsers != null && failedUsers.isNotEmpty() -> {
                    val failDetails = failedUsers.joinToString(", ") { it.toString() }
                    showError("添加失败: $failDetails")
                }
                else -> {
                    showError("添加失败：服务器返回空结果")
                }
            }
        } catch (e: Exception) {
            showError("解析响应数据异常")
        }
    }

    private fun queryNewMembersAndSetAdmin(
        emails: List<String>,
        phones: List<String>,
        userIds: List<String>
    ) {
        conversationId?.let { id ->
            apiService.listMembers(id, 1, 100).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val memberListData = parseMemberListResponse(response.body()?.data)

                        if (memberListData != null) {
                            val currentMemberIds = allMembers.map { it.userId }.toSet()
                            val newMembers: List<MemberInfo> = memberListData.items.filter { it.userId !in currentMemberIds }

                            if (newMembers.isNotEmpty()) {
                                newMembers.forEach { newMember ->
                                    apiService.setAdmin(id,
                                        com.example.clientforwebstorage.network.models.SetAdminRequest(userId = newMember.userId))
                                        .enqueue(object : Callback<ApiResponse> {
                                            override fun onResponse(
                                                call: Call<ApiResponse>,
                                                response: Response<ApiResponse>
                                            ) {
                                                if (response.isSuccessful && response.body()?.code == 0) {
                                                    android.util.Log.d("GroupDetail", "✅ 管理员设置成功: ${newMember.userId}")
                                                } else {
                                                    android.util.Log.e("GroupDetail",
                                                        "❌ 设置管理员失败: ${response.body()?.message}")
                                                }
                                            }

                                            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                                android.util.Log.e("GroupDetail", "设置管理员网络错误", t)
                                            }
                                        })
                                }
                            } else {
                                android.util.Log.w("GroupDetail",
                                    "未找到新成员，可能通过邮箱/手机号添加的用户需要手动匹配")

                                handleAddMemberResponse(response.body()?.data)
                            }
                        } else {
                            handleAddMemberResponse(response.body()?.data)
                        }
                    } else {
                        showError("查询成员列表失败")
                        handleAddMemberResponse(response.body()?.data)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showError("网络错误: ${t.message}")
                }
            })
        }
    }

    private fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return false
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showLeaveGroupDialog() {
        val currentUserRole = getConversationData()?.get("currentUserRole") as? String ?: "member"

        val title = when {
            currentUserRole == "owner" -> "解散群组"
            else -> "退出群组"
        }

        val message = when {
            currentUserRole == "owner" ->
                "确定要解散该群组吗？\n\n解散后群组将永久删除，所有成员将被移出，此操作不可撤销！"
            else ->
                "确定要退出「$conversationName」吗？"
        }

        val positiveButton = when {
            currentUserRole == "owner" -> "确认解散"
            else -> "确认退出"
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton) { _, _ ->
                if (currentUserRole == "owner") {
                    dissolveConversation()
                } else {
                    leaveConversation()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun getConversationData(): Map<String, Any?>? {
        return mapOf(
            "currentUserRole" to currentUserRole,
            "ownerUserId" to when {
                currentUserRole == "owner" -> TokenManager.getUserId()
                else -> null
            }
        )
    }

    private fun leaveConversation() {
        conversationId?.let { id ->
            apiService.leaveConversation(id).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.code == 0) {
                            Toast.makeText(requireContext(), "已退出群组", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            showError("退出失败: ${apiResponse.message}")
                        }
                    } else {
                        showError("网络请求失败")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showError("网络错误: ${t.message}")
                }
            })
        }
    }

    private fun dissolveConversation() {
        conversationId?.let { id ->
            apiService.dissolveConversation(id).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.code == 0) {
                            Toast.makeText(requireContext(), "群组已解散", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            showError("解散失败: ${apiResponse.message}")
                        }
                    } else {
                        showError("网络请求失败")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showError("网络错误: ${t.message}")
                }
            })
        }
    }

    private fun parseMemberListResponse(data: Any?): MemberListResponse? {
        if (data == null) return null

        return try {
            val json = Gson().toJson(data)
            val result = Gson().fromJson(json, object : TypeToken<MemberListResponse>() {}.type) as MemberListResponse?
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}