package com.example.clientforwebstorage.ui.agent

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R

class ConversationAdapter(
    private var items: List<AiConversationItem>,
    private val onItemClick: (AiConversationItem) -> Unit,
    private val onDeleteClick: (AiConversationItem) -> Unit,
    private val onEditClick: (AiConversationItem) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    private var expandedPosition = RecyclerView.NO_POSITION

    inner class ConversationViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_conversation_title)
        val tvTime: TextView = itemView.findViewById(R.id.tv_conversation_time)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit_conversation)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_conversation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title.ifEmpty { "未命名对话" }
        holder.tvTime.text = formatTimestamp(item.lastMessageAt)

        val isExpanded = position == expandedPosition
        holder.btnEdit.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.INVISIBLE
        holder.btnDelete.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.INVISIBLE

        holder.itemView.setOnClickListener {
            if (expandedPosition == holder.adapterPosition) {
                hideExpandedButtons()
            } else {
                onItemClick(item)
            }
        }

        holder.itemView.setOnLongClickListener {
            val oldPos = expandedPosition
            expandedPosition = holder.adapterPosition
            if (oldPos != RecyclerView.NO_POSITION && oldPos != expandedPosition) {
                notifyItemChanged(oldPos)
            }
            holder.btnEdit.visibility = android.view.View.VISIBLE
            holder.btnDelete.visibility = android.view.View.VISIBLE
            true
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AiConversationItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun hideExpandedButtons() {
        val oldPos = expandedPosition
        expandedPosition = RecyclerView.NO_POSITION
        if (oldPos != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPos)
        }
    }

    private fun formatTimestamp(timestamp: String?): String {
        if (timestamp.isNullOrEmpty()) return ""
        return try {
            val millis = timestamp.toLongOrNull()
            if (millis != null && millis > 1000000000000) {
                val now = System.currentTimeMillis()
                val diff = now - millis
                when {
                    diff < 60 * 1000 -> "刚刚"
                    diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                    diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                    diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
                    else -> {
                        val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(millis))
                    }
                }
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}
