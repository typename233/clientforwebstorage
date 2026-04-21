package com.example.netdisk.ui.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.netdisk.R

class FileListAdapter(
    private val items: List<FileItem>
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

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.iv_file_icon)
        private val name: TextView = itemView.findViewById(R.id.tv_file_name)
        private val meta: TextView = itemView.findViewById(R.id.tv_file_meta)

        fun bind(item: FileItem) {
            name.text = item.name
            val sizePrefix = item.size?.let { "$it · " } ?: ""
            meta.text = sizePrefix + item.updatedAt

            val (iconRes, tintRes) = when (item.type) {
                FileType.FOLDER -> android.R.drawable.ic_menu_agenda to android.R.color.holo_orange_light
                FileType.FILE -> when (item.extension?.lowercase()) {
                    "png", "jpg", "jpeg" -> android.R.drawable.ic_menu_gallery to android.R.color.holo_green_light
                    "mp4", "avi" -> android.R.drawable.ic_menu_slideshow to android.R.color.holo_red_light
                    "pdf", "doc", "docx" -> android.R.drawable.ic_menu_edit to android.R.color.holo_blue_light
                    else -> android.R.drawable.ic_menu_save to android.R.color.darker_gray
                }
            }
            icon.setImageResource(iconRes)
            icon.setColorFilter(ContextCompat.getColor(itemView.context, tintRes))
        }
    }
}
