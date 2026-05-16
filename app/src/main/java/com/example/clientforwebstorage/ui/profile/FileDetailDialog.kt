package com.example.clientforwebstorage.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.clientforwebstorage.R

class FileDetailDialog : DialogFragment() {

    companion object {
        private const val ARG_FILE_DATA = "file_data"

        fun create(fileData: Map<String, Any?>): FileDetailDialog {
            return FileDetailDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILE_DATA, HashMap(fileData))
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_file_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fileData = arguments?.getSerializable(ARG_FILE_DATA) as? Map<String, Any?>
        if (fileData == null) {
            dismiss()
            return
        }

        setupViews(view, fileData)
    }

    private fun setupViews(view: View, fileData: Map<String, Any?>) {
        val ivIcon = view.findViewById<ImageView>(R.id.iv_detail_icon)
        val tvName = view.findViewById<TextView>(R.id.tv_detail_name)
        val tvSize = view.findViewById<TextView>(R.id.tv_detail_size)
        val tvType = view.findViewById<TextView>(R.id.tv_detail_type)
        val tvCreatedAt = view.findViewById<TextView>(R.id.tv_detail_created_at)
        val tvUpdatedAt = view.findViewById<TextView>(R.id.tv_detail_updated_at)
        val btnClose = view.findViewById<View>(R.id.btn_close)

        val name = fileData["name"] as? String ?: fileData["title"] as? String ?: "未知文件"
        val size = fileData["size"] as? Number ?: 0L
        val extension = fileData["extension"] as? String
        val mimeType = fileData["mimeType"] as? String ?: "未知类型"
        val createdAt = fileData["createdAt"] as? String ?: "未知"
        val updatedAt = fileData["updatedAt"] as? String ?: "未知"

        tvName.text = name
        tvSize.text = formatFileSize(size.toLong())
        tvType.text = if (!extension.isNullOrEmpty()) "$extension ($mimeType)" else mimeType
        tvCreatedAt.text = createdAt
        tvUpdatedAt.text = updatedAt

        val iconRes = when (extension?.lowercase()) {
            "png", "jpg", "jpeg" -> android.R.drawable.ic_menu_gallery
            "mp4", "avi" -> android.R.drawable.ic_menu_slideshow
            "pdf", "doc", "docx" -> android.R.drawable.ic_menu_edit
            else -> android.R.drawable.ic_menu_save
        }
        ivIcon.setImageResource(iconRes)

        btnClose.setOnClickListener { dismiss() }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
