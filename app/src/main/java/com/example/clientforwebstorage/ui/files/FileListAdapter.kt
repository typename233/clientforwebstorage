package com.example.clientforwebstorage.ui.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R

class FileListAdapter(
    private val items: List<FileItem>,
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Boolean = { false }
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.iv_file_icon)
        private val name: TextView = itemView.findViewById(R.id.tv_file_name)
        private val meta: TextView = itemView.findViewById(R.id.tv_file_meta)
        private val btnMore: View = itemView.findViewById(R.id.btn_more)

        fun bind(item: FileItem) {
            name.text = item.name
            val sizeStr = if (item.type == FileType.FOLDER && item.size == 0L) "" else formatFileSize(item.size)
            val sizePrefix = sizeStr?.let { "$it · " } ?: ""
            meta.text = sizePrefix + item.updatedAt

            val (iconRes, tintRes) = when (item.type) {
                FileType.FOLDER -> android.R.drawable.ic_menu_agenda to R.color.primary_blue
                FileType.FILE -> when (item.extension?.lowercase()) {
                    "png", "jpg", "jpeg" -> android.R.drawable.ic_menu_gallery to R.color.primary_blue
                    "mp4", "avi" -> android.R.drawable.ic_menu_slideshow to R.color.primary_blue
                    "pdf", "doc", "docx" -> android.R.drawable.ic_menu_edit to R.color.primary_blue
                    else -> android.R.drawable.ic_menu_save to R.color.text_secondary
                }
            }
            icon.setImageResource(iconRes)
            icon.setColorFilter(ContextCompat.getColor(itemView.context, tintRes))

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { onItemLongClick(item) }
            btnMore.setOnClickListener { onItemLongClick(item) }
        }

        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> "${size} B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
                else -> "${size / (1024 * 1024 * 1024)} GB"
            }
        }
    }
}
