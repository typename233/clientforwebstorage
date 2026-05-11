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
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.CreateGroupRequest
import com.example.clientforwebstorage.network.models.GroupData
import com.example.clientforwebstorage.network.models.GroupListData
import com.example.clientforwebstorage.network.models.UpdateGroupRequest
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

        btnSearch.setOnClickListener {
            Toast.makeText(requireContext(), "搜索群组功能开发中", Toast.LENGTH_SHORT).show()
        }

        btnMore.setOnClickListener { showMoreMenu(it) }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = GroupListAdapter(
            groups,
            onItemClick = { item -> navigateToChat(item.id, item.name) },
            onItemLongClick = { item, anchor -> showGroupItemMenu(item, anchor) },
            onMoreButtonClick = { item, anchor -> showGroupItemMenu(item, anchor) }
        )
        recycler.adapter = adapter

        fab.setOnClickListener { showCreateGroupBottomSheet() }

        loadGroups(groupCount)
    }

    override fun onResume() {
        super.onResume()
        if (::pinnedManager.isInitialized && ::recycler.isInitialized) {
            val groupCount = view?.findViewById<TextView>(R.id.tv_group_count)
            if (groupCount != null) {
                loadGroups(groupCount)
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
            if (name.isNotEmpty()) createGroup(name)
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
            Toast.makeText(requireContext(), "搜索群组功能开发中", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_view_invites)?.setOnClickListener {
            navigateToInvites()
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
                Toast.makeText(requireContext(), "成员管理功能开发中", Toast.LENGTH_SHORT).show()
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
        
        val request = UpdateGroupRequest(
            name = item.name,
            isPinned = newPinnedState
        )

        RetrofitClient.api.updateGroup(item.id, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
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
                        
                        loadGroups(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                    } else {
                        pinnedManager.togglePinGroup(item.id)
                        Toast.makeText(requireContext(), "操作失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    pinnedManager.togglePinGroup(item.id)
                    Toast.makeText(requireContext(), "网络错误，已本地保存置顶状态", Toast.LENGTH_LONG).show()
                    loadGroups(view?.findViewById(R.id.tv_group_count) ?: return@onFailure)
                }
            })
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
                    fillAfter = true
                }
                
                val alphaAnim = android.view.animation.AlphaAnimation(1.0f, 0.5f).apply {
                    duration = 200
                    fillAfter = true
                }
                
                tv.startAnimation(scaleAnim)
                
                scaleAnim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        val resetAnim = android.view.animation.ScaleAnimation(
                            1.2f, 1.0f, 1.2f, 1.0f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
                        ).apply {
                            duration = 200
                            fillAfter = true
                        }
                        
                        val fadeInAnim = android.view.animation.AlphaAnimation(0.5f, 1.0f).apply {
                            duration = 200
                            fillAfter = true
                        }
                        
                        tv.startAnimation(resetAnim)
                        tv.startAnimation(fadeInAnim)
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
                    updateGroupName(item.id, newName)
                } else if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "群组名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateGroupName(groupId: String, newName: String) {
        RetrofitClient.api.updateGroup(groupId, UpdateGroupRequest(name = newName))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "群组名称已更新", Toast.LENGTH_SHORT).show()
                        loadGroups(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
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
            hint = "请输入新群主的用户ID或邮箱"
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
                    Toast.makeText(requireContext(), "请输入新群主信息", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun transferOwnership(groupId: String, newOwner: String, groupName: String) {
        RetrofitClient.api.updateMemberRole(
            groupId,
            newOwner,
            com.example.clientforwebstorage.network.models.UpdateMemberRoleRequest(role = "owner")
        ).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (!isAdded) return@onResponse
                if (response.isSuccessful && response.body()?.code == 0) {
                    Toast.makeText(requireContext(), "已成功转让「${groupName}」的群主身份", Toast.LENGTH_SHORT).show()
                    loadGroups(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
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
        val newMutedState = !item.isMuted
        val request = UpdateGroupRequest(
            name = item.name,
            isMuted = newMutedState
        )

        RetrofitClient.api.updateGroup(item.id, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val message = if (newMutedState) "已开启「${item.name}」的消息免打扰" else "已关闭「${item.name}」的消息免打扰"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        loadGroups(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                    } else {
                        Toast.makeText(requireContext(), "操作失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showDissolveGroupConfirmation(item: GroupItem) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ 解散群组")
            .setMessage("确定要解散「${item.name}」吗？\n\n⚠️ 此操作不可撤销，群组的所有数据将被永久删除！")
            .setPositiveButton("确认解散") { _, _ ->
                dissolveGroup(item.id, item.name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun dissolveGroup(groupId: String, groupName: String) {
        RetrofitClient.api.deleteGroup(groupId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已成功解散「${groupName}」", Toast.LENGTH_SHORT).show()
                        loadGroups(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
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

    private fun navigateToChat(groupId: String, groupName: String) {
        val chatFragment = ChatFragment.newInstance(groupId, groupName)
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

    private fun createGroup(name: String) {
        RetrofitClient.api.createGroup(CreateGroupRequest(name))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "群组已创建: $name", Toast.LENGTH_SHORT).show()
                        loadGroups(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                    } else {
                        Toast.makeText(requireContext(), "创建失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadGroups(countView: TextView) {
        RetrofitClient.api.getGroups(1, 50, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseGroupListData(response.body()?.data)
                        if (data != null) {
                            val localPinnedIds = pinnedManager.getPinnedGroupIds()
                            
                            groups = data.items.map { group ->
                                val isLocallyPinned = localPinnedIds.contains(group.id)
                                GroupItem(
                                    id = group.id,
                                    name = group.name,
                                    role = group.role,
                                    memberCount = group.memberCount,
                                    storageUsed = formatSize(group.storageUsed),
                                    visibility = group.visibility,
                                    isPinned = group.isPinned ?: false || isLocallyPinned,
                                    isMuted = group.isMuted ?: false
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
                        } else {
                            groups = emptyList()
                        }
                        
                        val pinnedCount = groups.count { it.isPinned }
                        
                        when {
                            pinnedCount > 0 -> countView.setSafeText("我的群组 ${groups.size} ($pinnedCount↑)")
                            groups.size > 0 -> countView.setSafeText("我的群组 ${groups.size}")
                            else -> countView.setSafeText("我的群组")
                        }
                        
                        adapter = GroupListAdapter(
                            groups,
                            onItemClick = { item -> navigateToChat(item.id, item.name) },
                            onItemLongClick = { item, anchor -> showGroupItemMenu(item, anchor) },
                            onMoreButtonClick = { item, anchor -> showGroupItemMenu(item, anchor) }
                        )
                        recycler.adapter = adapter
                    } else {
                        countView.setSafeText("我的群组 (0)")
                        groups = emptyList()
                        adapter = GroupListAdapter(emptyList(), onItemClick = { item ->
                            navigateToChat(item.id, item.name)
                        })
                        recycler.adapter = adapter
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun parseGroupListData(data: Any?): GroupListData? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<GroupListData>() {}.type)
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
