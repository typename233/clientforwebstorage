package com.example.netdisk.ui.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.netdisk.R

class GroupListAdapter(
    private val items: List<GroupItem>
) : RecyclerView.Adapter<GroupListAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.tv_group_name)
        private val meta: TextView = itemView.findViewById(R.id.tv_group_meta)
        private val role: TextView = itemView.findViewById(R.id.tv_group_role)
        private val visibilityIcon: ImageView = itemView.findViewById(R.id.iv_visibility)

        fun bind(item: GroupItem) {
            name.text = item.name
            when (item.role) {
                "owner" -> {
                    role.text = "所有者"
                    role.setBackgroundResource(R.drawable.bg_role_owner)
                }
                "editor" -> {
                    role.text = "编辑者"
                    role.setBackgroundResource(R.drawable.bg_role_editor)
                }
                else -> {
                    role.text = "查看者"
                    role.setBackgroundResource(R.drawable.bg_role_viewer)
                }
            }
            meta.text = "${item.memberCount} 成员 · ${item.storageUsed}"
            visibilityIcon.setImageResource(
                if (item.visibility == "private") {
                    android.R.drawable.ic_lock_lock
                } else {
                    android.R.drawable.presence_online
                }
            )
            visibilityIcon.setColorFilter(
                ContextCompat.getColor(itemView.context, R.color.text_secondary)
            )
        }
    }
}
