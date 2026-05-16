package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.databinding.ActivityMainBinding
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.CreateConversationRequest
import com.example.clientforwebstorage.network.models.MuteRequest
import com.example.clientforwebstorage.network.models.TransferOwnershipRequest
import com.example.clientforwebstorage.network.models.UpdateConversationRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupsFragment : Fragment() {

    private lateinit var recycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: GroupListAdapter
    private var groups: List<GroupItem> = emptyList()
    private lateinit var pinnedManager: PinnedGroupsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        pinnedManager = PinnedGroupsManager.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val groupCount = view.findViewById<TextView>(R.id.tv_group_count)
        recycler = view.findViewById(R.id.recycler_groups)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_group)
        val btnSearch = view.findViewById<ImageButton>(R.id.btn_search)
        val btnMore = view.findViewById<ImageButton>(R.id.btn_groups_more)
        val cardMySpace = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_my_space)

        btnSearch.setOnClickListener {
            navigateToSearch()
        }

        btnMore.setOnClickListener { showMoreMenu(it) }

        cardMySpace.setOnClickListener {
            navigateToFiles()
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = GroupListAdapter(
            groups,
            onItemClick = { item -> navigateToChat(item.id, item.name) },
            onItemLongClick = { item, anchor -> showGroupItemMenu(item, anchor) },
            onMoreButtonClick = { item, anchor -> showGroupItemMenu(item, anchor) }
        )
        recycler.adapter = adapter

        fab.setOnClickListener { showCreateGroupBottomSheet() }

        loadSpaceUsage()
        loadConversations(groupCount)
    }

    override fun onResume() {
        super.onResume()
        if (::pinnedManager.isInitialized && ::recycler.isInitialized) {
            val groupCount = view?.findViewById<TextView>(R.id.tv_group_count)
            if (groupCount != null) {
                loadConversations(groupCount)
            }
        }
    }

    private fun showCreateGroupBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_create_group, null)

        sheetView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            val nameInput = sheetView.findViewById<EditText>(R.id.et_group_name)
            val name = nameInput.text?.toString()?.trim().orEmpty()
            if (name.isNotEmpty()) createConversation(name)
            else Toast.makeText(requireContext(), "请输入群组名称", Toast.LENGTH_SHORT).show()
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun showMoreMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_groups_menu, null)

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

        popupView.findViewById<View>(R.id.menu_search_groups)?.setOnClickListener {
            navigateToSearch()
            popupWindow.dismiss()
        }

        val offsetX = anchor.width - popupWidth
        popupWindow.showAsDropDown(anchor, offsetX, 0)
    }

    private fun showGroupItemMenu(item: GroupItem, anchor: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_group_item_menu, null)

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth

        val menuPin = popupView.findViewById<TextView>(R.id.menu_pin_group)
        menuPin.text = if (item.isPinned) "取消置顶" else "置顶群组"

        val menuMute = popupView.findViewById<TextView>(R.id.menu_mute_notifications)
        menuMute.text = if (item.isMuted) "关闭免打扰" else "消息免打扰"

        val dividerTransfer = popupView.findViewById<View>(R.id.divider_transfer)
        val menuTransfer = popupView.findViewById<TextView>(R.id.menu_transfer_ownership)
        val dividerDissolve = popupView.findViewById<View>(R.id.divider_dissolve)
        val menuDissolve = popupView.findViewById<TextView>(R.id.menu_dissolve_group)

        val isOwner = item.role == "owner"
        val isAdmin = isOwner || item.role == "editor"

        menuTransfer.visibility = if (isOwner) View.VISIBLE else View.GONE
        dividerTransfer.visibility = if (isOwner) View.VISIBLE else View.GONE
        menuDissolve.visibility = if (isOwner) View.VISIBLE else View.GONE
        dividerDissolve.visibility = if (isOwner) View.VISIBLE else View.GONE

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

        popupView.findViewById<View>(R.id.menu_pin_group)?.setOnClickListener {
            togglePinGroup(item)
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_edit_group)?.setOnClickListener {
            if (isAdmin) {
                showEditGroupDialog(item)
            } else {
                Toast.makeText(requireContext(), "您没有权限编辑群组信息", Toast.LENGTH_SHORT).show()
            }
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_manage_members)?.setOnClickListener {
            if (isAdmin) {
                navigateToMemberManagement(item)
            } else {
                Toast.makeText(requireContext(), "您没有权限管理成员", Toast.LENGTH_SHORT).show()
            }
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_transfer_ownership)?.setOnClickListener {
            if (isOwner) {
                showTransferOwnershipDialog(item)
            }
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_mute_notifications)?.setOnClickListener {
            toggleMuteNotifications(item)
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_dissolve_group)?.setOnClickListener {
            if (isOwner) {
                showDissolveGroupConfirmation(item)
            }
            popupWindow.dismiss()
        }

        val offsetX = anchor.width - popupWidth
        popupWindow.showAsDropDown(anchor, offsetX, 0)
    }

    private fun togglePinGroup(item: GroupItem) {
        val newPinnedState = !item.isPinned
        
        pinnedManager.togglePinGroup(item.id)
        
        val message = if (newPinnedState) 
            "已将「${item.name}」置顶" 
        else 
            "已取消「${item.name}」的置顶"
        
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        
        if (newPinnedState) {
            showPinnedAnimation()
        } else {
            showUnpinnedAnimation()
        }
        
        loadConversations(view?.findViewById(R.id.tv_group_count) ?: return)
    }

    private fun showPinnedAnimation() {
        view?.let { v ->
            val countView = v.findViewById<TextView>(R.id.tv_group_count)
            countView?.let { tv ->
                val scaleAnim = android.view.animation.ScaleAnimation(
                    1.0f, 1.2f, 1.0f, 1.2f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 200
                    fillAfter = false
                }

                val alphaAnim = android.view.animation.AlphaAnimation(1.0f, 0.5f).apply {
                    duration = 200
                    fillAfter = false
                }

                tv.startAnimation(scaleAnim)
                tv.startAnimation(alphaAnim)

                scaleAnim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        val resetAnim = android.view.animation.ScaleAnimation(
                            1.2f, 1.0f, 1.2f, 1.0f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
                        ).apply {
                            duration = 200
                            fillAfter = false
                        }

                        val fadeInAnim = android.view.animation.AlphaAnimation(0.5f, 1.0f).apply {
                            duration = 200
                            fillAfter = false
                        }

                        tv.startAnimation(resetAnim)
                        tv.startAnimation(fadeInAnim)

                        resetAnim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                                tv.clearAnimation()
                            }
                            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                        })
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })
            }
        }
    }

    private fun showUnpinnedAnimation() {
        view?.let { v ->
            val anim = android.view.animation.TranslateAnimation(
                0f, -50f, 0f, 0f
            ).apply {
                duration = 300
                fillAfter = false
            }
            
            v.findViewById<TextView>(R.id.tv_group_count)?.startAnimation(anim)
        }
    }

    private fun showEditGroupDialog(item: GroupItem) {
        val editText = EditText(requireContext()).apply {
            setText(item.name)
            isSingleLine = true
            setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14))
            setSelection(item.name.length)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑群组信息")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newName = editText.text?.toString()?.trim().orEmpty()
                if (newName.isNotEmpty() && newName != item.name) {
                    updateConversationName(item.id, newName)
                } else if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "群组名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateConversationName(conversationId: String, newName: String) {
        RetrofitClient.api.updateConversation(conversationId, UpdateConversationRequest(name = newName))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "群组名称已更新", Toast.LENGTH_SHORT).show()
                        loadConversations(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                    } else {
                        Toast.makeText(requireContext(), "更新失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showTransferOwnershipDialog(item: GroupItem) {
        val editText = EditText(requireContext()).apply {
            hint = "请输入新群主的用户ID"
            isSingleLine = true
            setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14))
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("转让群主")
            .setMessage("确定要将「${item.name}」的群主身份转让给其他成员吗？转让后您将变为普通成员。")
            .setView(editText)
            .setPositiveButton("确认转让") { _, _ ->
                val newOwnerId = editText.text?.toString()?.trim().orEmpty()
                if (newOwnerId.isNotEmpty()) {
                    transferOwnership(item.id, newOwnerId, item.name)
                } else {
                    Toast.makeText(requireContext(), "请输入新群主用户ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun transferOwnership(conversationId: String, newOwner: String, groupName: String) {
        RetrofitClient.api.transferOwnership(conversationId, TransferOwnershipRequest(userId = newOwner))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已成功转让「${groupName}」的群主身份", Toast.LENGTH_SHORT).show()
                        loadConversations(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                    } else {
                        Toast.makeText(requireContext(), "转让失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun toggleMuteNotifications(item: GroupItem) {
        if (item.isMuted) {
            RetrofitClient.api.unmuteConversation(item.id)
                .enqueue(createMuteCallback(item, false))
        } else {
            RetrofitClient.api.muteConversation(item.id, MuteRequest())
                .enqueue(createMuteCallback(item, true))
        }
    }

    private fun createMuteCallback(item: GroupItem, newMutedState: Boolean): Callback<ApiResponse> {
        return object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (!isAdded) return@onResponse
                if (response.isSuccessful && response.body()?.code == 0) {
                    val message = if (newMutedState) "已开启「${item.name}」的消息免打扰" else "已关闭「${item.name}」的消息免打扰"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    loadConversations(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                } else {
                    Toast.makeText(requireContext(), "操作失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                if (!isAdded) return@onFailure
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDissolveGroupConfirmation(item: GroupItem) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ 解散群组")
            .setMessage("确定要解散「${item.name}」吗？\n\n⚠️ 此操作不可撤销，群组的所有数据将被永久删除！")
            .setPositiveButton("确认解散") { _, _ ->
                dissolveConversation(item.id, item.name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun dissolveConversation(conversationId: String, groupName: String) {
        RetrofitClient.api.dissolveConversation(conversationId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已成功解散「${groupName}」", Toast.LENGTH_SHORT).show()
                        loadConversations(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                    } else {
                        Toast.makeText(requireContext(), "解散失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun navigateToChat(conversationId: String, conversationName: String) {
        val chatFragment = ChatFragment.newInstance(conversationId, conversationName)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .addToBackStack("chat")
            .commit()
    }

    private fun navigateToInvites() {
        val invitesFragment = InvitesFragment.newInstance()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, invitesFragment)
            .addToBackStack("invites")
            .commit()
    }

    private fun navigateToSearch() {
        val searchFragment = SearchGroupsFragment.newInstance()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, searchFragment)
            .addToBackStack("search_groups")
            .commit()
    }

    private fun navigateToFiles() {
        activity?.let { act ->
            if (act is com.example.clientforwebstorage.MainActivity) {
                val bottomNav = act.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
                bottomNav?.selectedItemId = R.id.nav_files
            }
        }
    }

    private fun navigateToMemberManagement(item: GroupItem) {
        val memberManagementFragment = MemberManagementFragment.newInstance(
            item.id,
            item.name,
            item.role
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, memberManagementFragment)
            .addToBackStack("member_management")
            .commit()
    }

    private fun createConversation(name: String) {
        val currentUserId = TokenManager.getUserId()
        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "用户信息缺失，请重新登录", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CreateConversationRequest(
            conversationType = "group",
            memberUserIds = listOf(currentUserId),
            name = name
        )

        android.util.Log.d("GroupsFragment", "=== 创建会话请求 ===")
        android.util.Log.d("GroupsFragment", "conversationType: ${request.conversationType}")
        android.util.Log.d("GroupsFragment", "memberUserIds: ${request.memberUserIds}")
        android.util.Log.d("GroupsFragment", "name: ${request.name}")
        android.util.Log.d("GroupsFragment", "当前Token: ${TokenManager.getAccessToken()?.take(20)}...")
        
        RetrofitClient.api.createConversation(request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    val body = response.body()
                    android.util.Log.d("GroupsFragment", "=== 创建会话响应 ===")
                    android.util.Log.d("GroupsFragment", "isSuccessful: ${response.isSuccessful}")
                    android.util.Log.d("GroupsFragment", "code: ${response.code()}")
                    android.util.Log.d("GroupsFragment", "body.code: ${body?.code}")
                    android.util.Log.d("GroupsFragment", "body.message: ${body?.message}")
                    android.util.Log.d("GroupsFragment", "body.data: ${Gson().toJson(body?.data)}")
                    
                    if (response.isSuccessful && body?.code == 0) {
                        Toast.makeText(requireContext(), "群组已创建: $name", Toast.LENGTH_SHORT).show()
                        loadConversations(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                    } else {
                        Toast.makeText(requireContext(), "创建失败: ${body?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    android.util.Log.e("GroupsFragment", "创建会话网络错误", t)
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadConversations(countView: TextView) {
        android.util.Log.d("GroupsFragment", "=== 开始加载群组列表 ===")

        RetrofitClient.api.listConversations(null, 1, 50)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse

                    android.util.Log.d("GroupsFragment", "=== 群组列表响应 ===")
                    android.util.Log.d("GroupsFragment", "isSuccessful: ${response.isSuccessful}")
                    android.util.Log.d("GroupsFragment", "responseCode: ${response.code()}")

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseConversationListData(response.body()?.data)
                        if (data != null) {
                            android.util.Log.d("GroupsFragment", "解析成功，总条数: ${data.total}, items数量: ${data.items.size}")

                            val localPinnedIds = pinnedManager.getPinnedGroupIds()

                            groups = data.items.filter { it.conversationType == "group" }.map { conv ->
                                val isLocallyPinned = localPinnedIds.contains(conv.id)

                                GroupItem(
                                    id = conv.id,
                                    name = conv.name,
                                    role = "member",
                                    memberCount = 0,
                                    storageUsed = "",
                                    visibility = if (conv.status == "active") "public" else "private",
                                    isPinned = isLocallyPinned,
                                    isMuted = conv.isMuted
                                )
                            }.sortedWith(compareByDescending<GroupItem> { item ->
                                if (item.isPinned) {
                                    pinnedManager.getPinTime(item.id)
                                } else {
                                    0L
                                }
                            }.thenBy { it.name.lowercase() })

                            pinnedManager.syncWithServer(
                                groups.filter { it.isPinned }.map { it.id }.toSet()
                            )

                            updateUIWithGroups(countView)

                            android.util.Log.d("GroupsFragment", "开始加载 ${groups.size} 个群组的详情信息...")
                            loadGroupDetails()
                        } else {
                            android.util.Log.w("GroupsFragment", "解析会话列表数据失败，data为null")
                            groups = emptyList()
                            updateUIWithGroups(countView)
                        }
                    } else {
                        android.util.Log.e("GroupsFragment",
                            "加载群组列表失败 - code: ${response.body()?.code}, message: ${response.body()?.message}")
                        countView.setSafeText("群组 (0)")
                        groups = emptyList()
                        adapter = GroupListAdapter(emptyList(), onItemClick = { item ->
                            navigateToChat(item.id, item.name)
                        })
                        recycler.adapter = adapter
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    android.util.Log.e("GroupsFragment", "加载群组列表网络错误", t)
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUIWithGroups(countView: TextView? = null) {
        val targetCountView = countView ?: view?.findViewById(R.id.tv_group_count) ?: return

        val pinnedCount = groups.count { it.isPinned }

        when {
            pinnedCount > 0 -> targetCountView.setSafeText("群组 ${groups.size} ($pinnedCount↑)")
            groups.size > 0 -> targetCountView.setSafeText("群组 ${groups.size}")
            else -> targetCountView.setSafeText("群组")
        }

        adapter = GroupListAdapter(
            groups,
            onItemClick = { item -> navigateToChat(item.id, item.name) },
            onItemLongClick = { item, anchor -> showGroupItemMenu(item, anchor) },
            onMoreButtonClick = { item, anchor -> showGroupItemMenu(item, anchor) }
        )
        recycler.adapter = adapter
    }

    private fun loadGroupDetails() {
        if (groups.isEmpty()) {
            android.util.Log.d("GroupsFragment", "无群组需要加载详情")
            return
        }

        val currentUserId = TokenManager.getUserId()
        if (currentUserId.isNullOrEmpty()) {
            android.util.Log.w("GroupsFragment", "用户ID为空，无法确定用户角色")
            return
        }

        android.util.Log.d("GroupsFragment", "当前用户ID: $currentUserId")

        var completedCount = 0
        val totalGroups = groups.size

        for (group in groups) {
            android.util.Log.d("GroupsFragment", "正在加载群组详情: ${group.id} (${group.name})")

            RetrofitClient.api.getConversation(group.id)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (!isAdded) return@onResponse

                        completedCount++

                        if (response.isSuccessful && response.body()?.code == 0) {
                            val detail = parseConversationDetailData(response.body()?.data)
                            if (detail != null) {
                                val memberCount = detail.members.size
                                val userRole = detail.members.find { it.userId == currentUserId }?.role
                                    ?: if (detail.ownerUserId == currentUserId) "owner" else "member"

                                android.util.Log.d("GroupsFragment",
                                    "群组详情加载成功 - id: ${group.id}, name: ${group.name}, memberCount: $memberCount, userRole: $userRole")

                                val updatedGroups = groups.map { item ->
                                    if (item.id == group.id) {
                                        item.copy(role = userRole, memberCount = memberCount)
                                    } else {
                                        item
                                    }
                                }
                                groups = updatedGroups
                            } else {
                                android.util.Log.w("GroupsFragment",
                                    "解析群组详情失败 - id: ${group.id}")
                            }
                        } else {
                            android.util.Log.w("GroupsFragment",
                                "加载群组详情失败 - id: ${group.id}, code: ${response.body()?.code}")
                        }

                        if (completedCount >= totalGroups) {
                            android.util.Log.d("GroupsFragment", "所有群组详情加载完成，刷新UI")
                            updateUIWithGroups()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        if (!isAdded) return@onFailure

                        completedCount++
                        android.util.Log.e("GroupsFragment",
                            "加载群组详情网络错误 - id: ${group.id}", t)

                        if (completedCount >= totalGroups) {
                            android.util.Log.d("GroupsFragment", "所有群组详情请求完成（部分可能失败），刷新UI")
                            updateUIWithGroups()
                        }
                    }
                })
        }
    }

    private fun parseConversationDetailData(data: Any?): com.example.clientforwebstorage.network.models.ConversationDetail? {
        if (data == null) {
            android.util.Log.w("GroupsFragment", "群组详情数据为null")
            return null
        }
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json,
                object : TypeToken<com.example.clientforwebstorage.network.models.ConversationDetail>() {}.type)
        } catch (e: Exception) {
            android.util.Log.e("GroupsFragment", "解析群组详情数据异常", e)
            null
        }
    }

    private fun loadSpaceUsage() {
        val tvSpaceUsage = view?.findViewById<TextView>(R.id.tv_space_usage) ?: return

        RetrofitClient.api.getSpaceUsage()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = response.body()?.data
                        if (data is Map<*, *>) {
                            val usedBytes = (data["usedBytes"] as? Number)?.toLong() ?: 0L
                            val quotaBytes = (data["quotaBytes"] as? Number)?.toLong() ?: 0L
                            val usedText = formatSize(usedBytes)
                            val quotaText = formatSize(quotaBytes)
                            tvSpaceUsage.text = "$usedText 已使用 · $quotaText 总容量"
                        }
                    } else {
                        tvSpaceUsage.text = "获取空间信息失败"
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    tvSpaceUsage.text = "网络错误"
                }
            })
    }

    private fun parseConversationListData(data: Any?): com.example.clientforwebstorage.network.models.ConversationListResponse? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<com.example.clientforwebstorage.network.models.ConversationListResponse>() {}.type)
        } catch (_: Exception) { null }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * requireContext().resources.displayMetrics.density).toInt()

    private fun TextView.setSafeText(text: String, maxLength: Int = 20) {
        val displayText = if (text.length > maxLength) {
            text.substring(0, maxLength - 3) + "..."
        } else {
            text
        }
        this.text = displayText
    }
}
