package com.example.clientforwebstorage.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.PurgeRecycleRequest
import com.example.clientforwebstorage.network.models.Resource
import com.example.clientforwebstorage.network.models.ResourceListData
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecycleBinFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvStorageUsed: TextView
    private lateinit var tvTotalItems: TextView
    private lateinit var progressStorage: LinearProgressIndicator
    private lateinit var tvEmptyState: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var containerList: LinearLayout
    private lateinit var btnPurgeAll: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_recycle_bin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        loadRecycleBinData()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_recycle_bin)
        tvStorageUsed = view.findViewById(R.id.tv_storage_used)
        tvTotalItems = view.findViewById(R.id.tv_total_items)
        progressStorage = view.findViewById(R.id.progress_storage)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        progressLoading = view.findViewById(R.id.progress_loading)
        containerList = view.findViewById(R.id.container_recycle_list)
        btnPurgeAll = view.findViewById(R.id.btn_purge_all)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadRecycleBinData() {
        progressLoading.visibility = View.VISIBLE
        containerList.visibility = View.GONE
        tvEmptyState.visibility = View.GONE

        RetrofitClient.api.getRecycleResources(null, null, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    progressLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseData<ResourceListData>(response.body()?.data)
                        val items = data?.items ?: emptyList()
                        
                        updateStorageInfo(items)
                        displayRecycleItems(items)
                    } else {
                        showError("加载失败")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    progressLoading.visibility = View.GONE
                    showError("网络错误：${t.message}")
                }
            })
    }

    private fun updateStorageInfo(items: List<Resource>) {
        val totalSize = items.sumOf { it.size ?: 0L }
        val formattedSize = formatFileSize(totalSize)
        
        tvStorageUsed.text = "已用空间（已删除）：$formattedSize"
        tvTotalItems.text = "已删除文件数：${items.size} 个"
        
        progressStorage.isIndeterminate = false
        progressStorage.progress = 100
        
        if (items.isEmpty()) {
            btnPurgeAll.visibility = View.GONE
        } else {
            btnPurgeAll.visibility = View.VISIBLE
            btnPurgeAll.setOnClickListener { showPurgeAllConfirmDialog() }
        }
    }

    private fun displayRecycleItems(items: List<Resource>) {
        containerList.removeAllViews()

        if (items.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            containerList.visibility = View.GONE
            return
        }

        tvEmptyState.visibility = View.GONE
        containerList.visibility = View.VISIBLE

        items.forEach { resource ->
            addRecycleItemView(resource)
        }
    }

    private fun addRecycleItemView(resource: Resource) {
        val itemView = layoutInflater.inflate(R.layout.item_recycle_bin, containerList, false)

        val ivIcon = itemView.findViewById<ImageView>(R.id.iv_file_icon)
        val tvName = itemView.findViewById<TextView>(R.id.tv_file_name)
        val tvInfo = itemView.findViewById<TextView>(R.id.tv_file_info)
        val tvPath = itemView.findViewById<TextView>(R.id.tv_original_path)
        val btnRestore = itemView.findViewById<TextView>(R.id.btn_restore)
        val btnDelete = itemView.findViewById<TextView>(R.id.btn_delete_permanently)

        tvName.text = resource.name ?: "未知文件"

        val sizeStr = formatFileSize(resource.size ?: 0L)
        val typeStr = getFileTypeString(resource.type, resource.extension)
        val deleteTime = formatDate(resource.updatedAt)

        tvInfo.text = "$sizeStr · $typeStr · 删除于 $deleteTime"

        val extensionStr = if (!resource.extension.isNullOrBlank()) ".${resource.extension}" else ""
        tvPath.text = "文件类型：${resource.type}$extensionStr"

        setFileIcon(ivIcon, resource.type, resource.extension)

        btnRestore.setOnClickListener {
            showRestoreConfirmDialog(resource.id, resource.name ?: "该文件")
        }

        btnDelete.setOnClickListener {
            showPermanentDeleteConfirmDialog(resource.id, resource.name ?: "该文件", itemView)
        }

        containerList.addView(itemView)
    }

    private fun setFileIcon(imageView: ImageView, type: String, extension: String?) {
        when {
            type == "image" || extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                imageView.setBackgroundColor(0xFFE8F5E9.toInt())
            }
            type == "video" || extension in listOf("mp4", "avi", "mkv", "mov", "wmv") -> {
                imageView.setImageResource(android.R.drawable.ic_media_play)
                imageView.setBackgroundColor(0xFFFFEBEE.toInt())
            }
            type == "document" || extension in listOf("pdf", "doc", "docx", "txt", "xls", "xlsx") -> {
                imageView.setImageResource(android.R.drawable.ic_menu_agenda)
                imageView.setBackgroundColor(0xFFE3F2FD.toInt())
            }
            type == "folder" -> {
                imageView.setImageResource(android.R.drawable.ic_menu_save)
                imageView.setBackgroundColor(0xFFF3E5F5.toInt())
            }
            else -> {
                imageView.setImageResource(android.R.drawable.ic_menu_save)
                imageView.setBackgroundColor(0xFFE0E0E0.toInt())
            }
        }
    }

    private fun restoreResource(resourceId: String) {
        RetrofitClient.api.restoreResource(resourceId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "文件已恢复", Toast.LENGTH_SHORT).show()
                        loadRecycleBinData()
                    } else {
                        Toast.makeText(requireContext(), "恢复失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun permanentDeleteResource(resourceId: String, itemView: View) {
        RetrofitClient.api.purgeRecycle(PurgeRecycleRequest(purgeAll = false, resourceIds = listOf(resourceId)))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "文件已永久删除", Toast.LENGTH_SHORT).show()
                        containerList.removeView(itemView)
                        loadRecycleBinData()
                    } else {
                        Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun purgeAllResources() {
        RetrofitClient.api.purgeRecycle(PurgeRecycleRequest(purgeAll = true, resourceIds = null))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "回收站已清空", Toast.LENGTH_SHORT).show()
                        loadRecycleBinData()
                    } else {
                        Toast.makeText(requireContext(), "清空失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showRestoreConfirmDialog(resourceId: String, fileName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("恢复文件")
            .setMessage("确定要恢复「$fileName」吗？\n恢复后文件将回到原来的位置。")
            .setPositiveButton("确定恢复") { _, _ ->
                restoreResource(resourceId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPermanentDeleteConfirmDialog(resourceId: String, fileName: String, itemView: View) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("永久删除")
            .setMessage("确定要永久删除「$fileName」吗？\n此操作不可撤销！")
            .setPositiveButton("永久删除") { _, _ ->
                permanentDeleteResource(resourceId, itemView)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPurgeAllConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空回收站")
            .setMessage("确定要清空所有已删除的文件吗？\n此操作将永久删除所有文件，不可撤销！")
            .setPositiveButton("确定清空") { _, _ ->
                purgeAllResources()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showError(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
        containerList.visibility = View.GONE
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.CHINA, "%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format(Locale.CHINA, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    private fun getFileTypeString(type: String, extension: String?): String {
        return when {
            type == "image" || extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> "图片"
            type == "video" || extension in listOf("mp4", "avi", "mkv", "mov", "wmv") -> "视频"
            type == "audio" || extension in listOf("mp3", "wav", "flac", "aac") -> "音频"
            extension == "pdf" -> "PDF文档"
            extension in listOf("doc", "docx") -> "Word文档"
            extension in listOf("xls", "xlsx") -> "Excel表格"
            extension in listOf("txt", "md") -> "文本文件"
            extension in listOf("zip", "rar", "7z", "tar") -> "压缩包"
            type == "folder" -> "文件夹"
            else -> if (!extension.isNullOrBlank()) "${extension.toUpperCase()} 文件" else "其他文件"
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }

    private inline fun <reified T> parseData(data: Any?): T? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<T>() {}.type)
        } catch (_: Exception) { null }
    }
}
