package com.example.clientforwebstorage.ui.groups

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.models.MemberInfo
import de.hdodenhof.circleimageview.CircleImageView

class MemberListAdapter(
    private var members: List<MemberInfo>,
    private val currentUserId: String?,
    private val currentUserRole: String,
    private val onRemoveMember: (MemberInfo) -> Unit,
    private val onSetAdmin: (MemberInfo) -> Unit,
    private val onRemoveAdmin: (MemberInfo) -> Unit,
    private val onTransferOwnership: (MemberInfo) -> Unit
) : RecyclerView.Adapter<MemberListAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: CircleImageView = itemView.findViewById(R.id.iv_avatar)
        val tvNickname: TextView = itemView.findViewById(R.id.tv_nickname)
        val tvRoleBadge: TextView = itemView.findViewById(R.id.tv_role_badge)
        val tvUserId: TextView = itemView.findViewById(R.id.tv_user_id)
        val tvJoinedAt: TextView = itemView.findViewById(R.id.tv_joined_at)
        val btnMoreActions: ImageButton = itemView.findViewById(R.id.btn_more_actions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        val context = holder.itemView.context

        holder.tvNickname.text = member.nickname
        holder.tvUserId.text = "ID: ${member.userId.take(8)}..."

        if (!member.avatarUrl.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(member.avatarUrl)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.ic_menu_myplaces)
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
        }

        when (member.role.lowercase()) {
            "owner" -> {
                holder.tvRoleBadge.visibility = android.view.View.VISIBLE
                holder.tvRoleBadge.text = "群主"
                holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_role_owner)
                holder.tvRoleBadge.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.role_owner_text
                    )
                )
            }
            "admin", "editor" -> {
                holder.tvRoleBadge.visibility = android.view.View.VISIBLE
                holder.tvRoleBadge.text = "管理员"
                holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_role_editor)
                holder.tvRoleBadge.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.role_editor_text
                    )
                )
            }
            else -> {
                holder.tvRoleBadge.visibility = android.view.View.VISIBLE
                holder.tvRoleBadge.text = "成员"
                holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_role_viewer)
                holder.tvRoleBadge.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        holder.itemView.context,
                        R.color.role_viewer_text
                    )
                )
            }
        }

        member.joinedAt?.let { joinedTime ->
            try {
                val parsedDate = parseJoinedAtTime(joinedTime)
                holder.tvJoinedAt.text = "加入时间: $parsedDate"
            } catch (e: Exception) {
                android.util.Log.w("MemberListAdapter", "解析加入时间失败: $joinedTime", e)
                holder.tvJoinedAt.text = "加入时间: $joinedTime"
            }
        } ?: run {
            holder.tvJoinedAt.visibility = android.view.View.GONE
        }

        val isCurrentUser = member.userId == currentUserId
        val isOwner = currentUserRole == "owner"

        holder.btnMoreActions.setOnClickListener { anchor ->
            showMemberActionMenu(member, isCurrentUser, isOwner, anchor, context)
        }
    }

    private fun showMemberActionMenu(
        member: MemberInfo,
        isCurrentUser: Boolean,
        isOwner: Boolean,
        anchor: android.view.View,
        context: android.content.Context
    ) {
        val popupView = LayoutInflater.from(context)
            .inflate(R.layout.popup_member_actions, null)

        popupView.measure(android.view.View.MeasureSpec.UNSPECIFIED, android.view.View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth

        val menuSetAdmin = popupView.findViewById<TextView>(R.id.menu_set_admin)
        val menuRemoveAdmin = popupView.findViewById<TextView>(R.id.menu_remove_admin)
        val menuTransferOwner = popupView.findViewById<TextView>(R.id.menu_transfer_owner)
        val menuRemoveMember = popupView.findViewById<TextView>(R.id.menu_remove_member)
        val dividerAdmin = popupView.findViewById<android.view.View>(R.id.divider_admin)
        val dividerTransfer = popupView.findViewById<android.view.View>(R.id.divider_transfer)

        when {
            isCurrentUser -> {
                menuSetAdmin.visibility = android.view.View.GONE
                menuRemoveAdmin.visibility = android.view.View.GONE
                menuTransferOwner.visibility = android.view.View.GONE
                dividerAdmin.visibility = android.view.View.GONE
                dividerTransfer.visibility = android.view.View.GONE
                menuRemoveMember.text = "退出群组"
            }
            isOwner -> {
                when (member.role.lowercase()) {
                    "owner" -> {
                        menuSetAdmin.visibility = android.view.View.GONE
                        menuRemoveAdmin.visibility = android.view.View.GONE
                        menuTransferOwner.visibility = android.view.View.VISIBLE
                    }
                    "admin", "editor" -> {
                        menuSetAdmin.visibility = android.view.View.GONE
                        menuRemoveAdmin.visibility = android.view.View.VISIBLE
                        menuTransferOwner.visibility = android.view.View.VISIBLE
                    }
                    else -> {
                        menuSetAdmin.visibility = android.view.View.VISIBLE
                        menuRemoveAdmin.visibility = android.view.View.GONE
                        menuTransferOwner.visibility = android.view.View.VISIBLE
                    }
                }
                dividerAdmin.visibility = android.view.View.VISIBLE
                dividerTransfer.visibility = android.view.View.VISIBLE
                menuRemoveMember.visibility = android.view.View.VISIBLE
            }
            else -> {
                menuSetAdmin.visibility = android.view.View.GONE
                menuRemoveAdmin.visibility = android.view.View.GONE
                menuTransferOwner.visibility = android.view.View.GONE
                dividerAdmin.visibility = android.view.View.GONE
                dividerTransfer.visibility = android.view.View.GONE
                menuRemoveMember.visibility = android.view.View.GONE
                Toast.makeText(context, "您没有权限管理该成员", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            setBackgroundDrawable(context.resources.getDrawable(android.R.color.white, null))
            isOutsideTouchable = true
            isFocusable = true
        }

        menuSetAdmin?.setOnClickListener {
            onSetAdmin(member)
            popupWindow.dismiss()
        }

        menuRemoveAdmin?.setOnClickListener {
            onRemoveAdmin(member)
            popupWindow.dismiss()
        }

        menuTransferOwner?.setOnClickListener {
            onTransferOwnership(member)
            popupWindow.dismiss()
        }

        menuRemoveMember?.setOnClickListener {
            if (isCurrentUser) {
                showLeaveConfirmation(context, member)
            } else {
                showRemoveConfirmation(context, member)
            }
            popupWindow.dismiss()
        }

        val offsetX = anchor.width - popupWidth
        popupWindow.showAsDropDown(anchor, offsetX, 0)
    }

    private fun showLeaveConfirmation(context: android.content.Context, member: MemberInfo) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("退出群组")
            .setMessage("确定要退出该群组吗？")
            .setPositiveButton("确认退出") { _, _ ->
                onRemoveMember(member)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRemoveConfirmation(context: android.content.Context, member: MemberInfo) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("移除成员")
            .setMessage("确定要将「${member.nickname}」移出群组吗？")
            .setPositiveButton("确认移除") { _, _ ->
                onRemoveMember(member)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun getItemCount(): Int = members.size

    fun updateData(newMembers: List<MemberInfo>) {
        members = newMembers
        notifyDataSetChanged()
    }

    private fun parseJoinedAtTime(timeString: String): String {
        val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd"
        )

        for (format in formats) {
            try {
                val inputFormat = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                val date = inputFormat.parse(timeString)
                if (date != null) {
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                continue
            }
        }

        return timeString
    }

    fun filterMembers(keyword: String?, roleFilter: String?): List<MemberInfo> {
        var filtered = members

        if (!keyword.isNullOrBlank()) {
            filtered = filtered.filter { 
                it.nickname.contains(keyword, ignoreCase = true) || 
                it.userId.contains(keyword, ignoreCase = true) 
            }
        }

        if (!roleFilter.isNullOrBlank() && roleFilter != "all") {
            filtered = when (roleFilter.lowercase()) {
                "owner" -> filtered.filter { it.role.lowercase() == "owner" }
                "admin" -> filtered.filter { it.role.lowercase() in listOf("admin", "editor") }
                "member" -> filtered.filter { it.role.lowercase() !in listOf("owner", "admin", "editor") }
                else -> filtered
            }
        }

        return filtered
    }
}
