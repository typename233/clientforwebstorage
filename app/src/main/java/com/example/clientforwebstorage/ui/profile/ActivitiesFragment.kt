package com.example.clientforwebstorage.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.UserActivity
import com.example.clientforwebstorage.network.models.UserActivityListData
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivitiesFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvTotalActivities: TextView
    private lateinit var tvSuccessCount: TextView
    private lateinit var tvFailedCount: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var containerList: LinearLayout

    private lateinit var btnPrevPage: TextView
    private lateinit var btnNextPage: TextView
    private lateinit var tvPageInfo: TextView
    private lateinit var paginationLayout: LinearLayout

    private var currentPage = 1
    private val pageSize = 5
    private var totalPages = 0
    private var totalRecords = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_activities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        setupPaginationControls()
        loadActivitiesData(currentPage, pageSize)
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_activities)
        tvTotalActivities = view.findViewById(R.id.tv_total_activities)
        tvSuccessCount = view.findViewById(R.id.tv_success_count)
        tvFailedCount = view.findViewById(R.id.tv_failed_count)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        progressLoading = view.findViewById(R.id.progress_loading)
        containerList = view.findViewById(R.id.container_activity_list)

        paginationLayout = view.findViewById(R.id.pagination_layout)
        btnPrevPage = view.findViewById(R.id.btn_prev_page)
        btnNextPage = view.findViewById(R.id.btn_next_page)
        tvPageInfo = view.findViewById(R.id.tv_page_info)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupPaginationControls() {
        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                loadActivitiesData(currentPage, pageSize)
            }
        }

        btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                loadActivitiesData(currentPage, pageSize)
            }
        }
    }

    private fun updatePaginationUI() {
        val displayTotalPages = if (totalPages == 0) 1 else totalPages
        tvPageInfo.text = "第 $currentPage / $displayTotalPages 页"

        btnPrevPage.isEnabled = currentPage > 1
        btnNextPage.isEnabled = currentPage < totalPages
        btnPrevPage.alpha = if (currentPage > 1) 1f else 0.4f
        btnNextPage.alpha = if (currentPage < totalPages) 1f else 0.4f

        if (totalRecords > pageSize) {
            paginationLayout.visibility = View.VISIBLE
        } else {
            paginationLayout.visibility = View.GONE
        }
    }

    private fun loadActivitiesData(page: Int, size: Int) {
        progressLoading.visibility = View.VISIBLE
        containerList.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
        paginationLayout.visibility = View.GONE

        RetrofitClient.api.getUserActivities(page, size)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    progressLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseData<UserActivityListData>(response.body()?.data)
                        val activities = data?.items ?: emptyList()
                        totalRecords = data?.total ?: 0

                        if (totalRecords > 0 && size > 0) {
                            totalPages = (totalRecords + size - 1) / size
                        } else {
                            totalPages = 0
                        }

                        updateStatistics(activities)
                        displayActivityItems(activities)
                        updatePaginationUI()
                    } else {
                        showError("加载失败：${response.body()?.message ?: "未知错误"}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    progressLoading.visibility = View.GONE
                    showError("网络错误：${t.message}")
                    updatePaginationUI()
                }
            })
    }

    private fun updateStatistics(activities: List<UserActivity>) {
        val total = activities.size
        val successCount = activities.count { it.result == "success" || it.result == "Success" }
        val failedCount = activities.count { it.result == "failed" || it.result == "Failed" || it.result == "error" }

        tvTotalActivities.text = total.toString()
        tvSuccessCount.text = successCount.toString()
        tvFailedCount.text = failedCount.toString()
    }

    private fun displayActivityItems(activities: List<UserActivity>) {
        containerList.removeAllViews()

        if (activities.isEmpty()) {
            tvEmptyState.text = "暂无操作记录\n您的操作历史将在这里显示"
            tvEmptyState.visibility = View.VISIBLE
            containerList.visibility = View.GONE
            return
        }

        tvEmptyState.visibility = View.GONE
        containerList.visibility = View.VISIBLE

        activities.forEach { activity ->
            addActivityItemView(activity)
        }
    }

    private fun addActivityItemView(activity: UserActivity) {
        val itemView = layoutInflater.inflate(R.layout.item_activity, containerList, false)

        val ivIcon = itemView.findViewById<ImageView>(R.id.iv_activity_icon)
        val tvEventType = itemView.findViewById<TextView>(R.id.tv_event_type)
        val badgeResult = itemView.findViewById<TextView>(R.id.badge_result)
        val tvTargetInfo = itemView.findViewById<TextView>(R.id.tv_target_info)
        val tvMetadata = itemView.findViewById<TextView>(R.id.tv_metadata)
        val tvReason = itemView.findViewById<TextView>(R.id.tv_reason)
        val tvTime = itemView.findViewById<TextView>(R.id.tv_time)

        val (eventText, iconRes, bgColor) = getEventTypeInfo(activity.eventType)
        tvEventType.text = eventText
        ivIcon.setImageResource(iconRes)
        ivIcon.setBackgroundColor(bgColor)

        when {
            activity.result.equals("success", ignoreCase = true) -> {
                badgeResult.text = "成功"
                badgeResult.setTextColor(0xFFFFFFFF.toInt())
                badgeResult.setBackgroundColor(0xFF4CAF50.toInt())
            }
            activity.result.equals("failed", ignoreCase = true) ||
            activity.result.equals("error", ignoreCase = true) -> {
                badgeResult.text = "失败"
                badgeResult.setTextColor(0xFFFFFFFF.toInt())
                badgeResult.setBackgroundColor(0xFFD32F2F.toInt())
            }
            else -> {
                badgeResult.text = activity.result
                badgeResult.setTextColor(0xFFFFFFFF.toInt())
                badgeResult.setBackgroundColor(0xFFFF9800.toInt())
            }
        }

        val targetTypeName = getTargetTypeDisplayName(activity.targetType)
        tvTargetInfo.text = "目标：$targetTypeName (${activity.targetId.takeLast(8)})"

        val metadataInfo = parseMetadata(activity.metadata)
        if (metadataInfo.isNotEmpty()) {
            tvMetadata.text = metadataInfo
            tvMetadata.visibility = View.VISIBLE
        }

        if (!activity.reason.isNullOrBlank()) {
            tvReason.text = "原因：${activity.reason}"
            tvReason.visibility = View.VISIBLE
        }

        tvTime.text = formatTime(activity.createdAt)

        itemView.setOnClickListener {
            showActivityDetailDialog(activity)
        }

        containerList.addView(itemView)
    }

    private fun showActivityDetailDialog(activity: UserActivity) {
        val (eventText, _, _) = getEventTypeInfo(activity.eventType)

        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
        }

        container.addView(createDetailRow("操作类型", eventText, activity.eventType))
        container.addView(createDetailRow("事件ID", activity.eventId))

        val resultText = when {
            activity.result.equals("success", ignoreCase = true) -> "成功 ✅"
            activity.result.equals("failed", ignoreCase = true) ||
            activity.result.equals("error", ignoreCase = true) -> "失败 ❌"
            else -> activity.result
        }
        container.addView(createDetailRow("执行结果", resultText, activity.result))

        container.addView(createDetailRow("目标类型", getTargetTypeDisplayName(activity.targetType), activity.targetType))
        container.addView(createDetailRow("目标ID", activity.targetId))
        container.addView(createDetailRow("操作者类型", activity.actorType))

        if (!activity.reason.isNullOrBlank()) {
            container.addView(createDetailRow("失败原因", activity.reason, null, true))
        }

        container.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            )
            setBackgroundColor(0xFFE5E7EB.toInt())
            setPadding(0, 0, 0, dpToPx(8))
        })

        container.addView(TextView(requireContext()).apply {
            text="详细信息"
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
        })

        val metadataInfo = parseMetadataDetailed(activity.metadata)
        if (metadataInfo.isNotEmpty()) {
            metadataInfo.forEach { (key, value) ->
                container.addView(createDetailRow(key, value))
            }
        } else {
            container.addView(TextView(requireContext()).apply {
                text="暂无详细信息"
                textSize = 13f
                setTextColor(0xFF999999.toInt())
                setPadding(0, dpToPx(4), 0, 0)
            })
        }

        container.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            )
            setBackgroundColor(0xFFE5E7EB.toInt())
            setPadding(0, dpToPx(8), 0, 0)
        })

        container.addView(createDetailRow("创建时间", formatDateTimeFull(activity.createdAt)))

        scrollView.addView(container)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("操作详情")
            .setView(scrollView)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun createDetailRow(
        label: String,
        value: String,
        subValue: String? = null,
        isError: Boolean = false
    ): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }

        row.addView(TextView(requireContext()).apply {
            text=label
            textSize = 12f
            setTextColor(0xFF666666.toInt())
        })

        row.addView(TextView(requireContext()).apply {
            text=value
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(if (isError) 0xFFD32F2F.toInt() else 0xFF333333.toInt())
        })

        if (!subValue.isNullOrBlank() && subValue != value) {
            row.addView(TextView(requireContext()).apply {
                text=subValue
                textSize = 11f
                setTextColor(0xFF999999.toInt())
                typeface = android.graphics.Typeface.MONOSPACE
            })
        }

        return row
    }

    private fun parseMetadataDetailed(metadata: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        if (metadata.isBlank()) return result

        try {
            val json = JSONObject(metadata)

            json.optString("fileName")?.takeIf { it.isNotBlank() }?.let {
                result.add(Pair("文件名", it))
            }
            json.opt("fileSize")?.let { size ->
                when (size) {
                    is Number -> result.add(Pair("文件大小", "${formatFileSize(size.toLong())} (${size.toLong()} bytes)"))
                    is String -> if (size.isNotBlank()) result.add(Pair("文件大小", size))
                }
            } ?: json.opt("size")?.let { size ->
                when (size) {
                    is Number -> result.add(Pair("文件大小", "${formatFileSize(size.toLong())} (${size.toLong()} bytes)"))
                    is String -> if (size.isNotBlank()) result.add(Pair("文件大小", size))
                }
            }
            json.optString("duration")?.takeIf { it.isNotBlank() }?.let {
                result.add(Pair("操作耗时", "${it} 秒"))
            }
            json.optString("path")?.takeIf { it.isNotBlank() }?.let {
                result.add(Pair("文件路径", it))
            }
            json.optString("ip")?.takeIf { it.isNotBlank() }?.let {
                result.add(Pair("访问IP", it))
            }
            json.optString("userAgent")?.takeIf { it.isNotBlank() }?.let {
                result.add(Pair("用户代理", it))
            }
            json.optString("extension")?.takeIf { it.isNotBlank() }?.let {
                result.add(Pair("文件扩展名", it))
            }
            json.opt("mimeType")?.let {
                result.add(Pair("MIME 类型", it.toString()))
            }

            if (result.isEmpty()) {
                result.add(Pair("原始数据", metadata))
            }
        } catch (e: Exception) {
            result.add(Pair("原始数据", metadata))
        }

        return result
    }

    private fun formatDateTimeFull(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss.SSS", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * requireContext().resources.displayMetrics.density).toInt()

    private fun getEventTypeInfo(eventType: String): Triple<String, Int, Int> {
        return when (eventType.lowercase()) {
            "upload", "file.upload", "resource.upload", "resource_create" -> Triple(
                "上传文件",
                android.R.drawable.ic_menu_upload,
                0xFFE3F2FD.toInt()
            )
            "download", "file.download", "resource.download" -> Triple(
                "下载文件",
                android.R.drawable.ic_menu_save,
                0xFFE8F5E9.toInt()
            )
            "list", "resource.list", "resource_list", "view", "browse" -> Triple(
                "查看文件列表",
                android.R.drawable.ic_menu_view,
                0xFFE3F2FD.toInt()
            )
            "recycle.list", "resource.recycle_list", "resource_recycle_list" -> Triple(
                "查看回收站",
                android.R.drawable.ic_menu_recent_history,
                0xFFE8F5E9.toInt()
            )
            "delete", "file.delete", "resource.delete", "resource_soft_delete", "soft_delete" -> Triple(
                "删除文件",
                android.R.drawable.ic_menu_delete,
                0xFFFFEBEE.toInt()
            )
            "purge", "recycle.purge", "resource_recycle_purge" -> Triple(
                "清空回收站",
                android.R.drawable.ic_menu_delete,
                0xFFFFCDD2.toInt()
            )
            "share.create", "link.create" -> Triple(
                "创建分享",
                android.R.drawable.ic_menu_share,
                0xFFF3E5F5.toInt()
            )
            "share.revoke", "link.revoke" -> Triple(
                "撤销分享",
                android.R.drawable.ic_menu_close_clear_cancel,
                0xFFECEFF1.toInt()
            )
            "folder.create", "directory.create", "resource_folder_create", "folder_create" -> Triple(
                "创建文件夹",
                android.R.drawable.ic_menu_add,
                0xFFFBE9E7.toInt()
            )
            "rename", "file.rename", "resource.rename", "resource_rename" -> Triple(
                "重命名",
                android.R.drawable.ic_menu_edit,
                0xFFE0F2F1.toInt()
            )
            "move", "file.move", "resource.move" -> Triple(
                "移动文件",
                android.R.drawable.ic_menu_directions,
                0xFFE1F5FE.toInt()
            )
            "copy", "file.copy", "resource.copy" -> Triple(
                "复制文件",
                android.R.drawable.ic_menu_agenda,
                0xFFF1F8E9.toInt()
            )
            "restore", "resource.restore", "resource_restore" -> Triple(
                "恢复文件",
                android.R.drawable.ic_menu_revert,
                0xFFEDE7F6.toInt()
            )
            "login", "user.login" -> Triple(
                "登录系统",
                android.R.drawable.ic_lock_idle_lock,
                0xFFECEFF1.toInt()
            )
            else -> Triple(
                eventType,
                android.R.drawable.ic_menu_manage,
                0xFFF5F5F5.toInt()
            )
        }
    }

    private fun getTargetTypeDisplayName(targetType: String): String {
        return when (targetType.lowercase()) {
            "file", "resource" -> "文件"
            "folder", "directory" -> "文件夹"
            "share", "link" -> "分享链接"
            "group" -> "群组"
            "user" -> "用户"
            else -> targetType
        }
    }

    private fun parseMetadata(metadata: String): String {
        if (metadata.isBlank()) return ""

        return try {
            val json = JSONObject(metadata)
            val parts = mutableListOf<String>()

            json.optString("fileName")?.takeIf { it.isNotBlank() }?.let {
                parts.add("文件：$it")
            }
            json.optString("fileSize")?.takeIf { it.isNotBlank() }?.let {
                try {
                    val sizeBytes = it.toLongOrNull() ?: it.toLong()
                    parts.add("大小：${formatFileSize(sizeBytes)}")
                } catch (_: Exception) {
                    parts.add("大小：$it")
                }
            }
            json.optString("duration")?.takeIf { it.isNotBlank() }?.let {
                parts.add("耗时：${it}s")
            }
            json.opt("size")?.let { size ->
                when (size) {
                    is Number -> parts.add("大小：${formatFileSize(size.toLong())}")
                    is String -> if (size.isNotBlank()) parts.add("大小：$size")
                }
            }
            json.optString("path")?.takeIf { it.isNotBlank() }?.let {
                parts.add("路径：$it")
            }
            json.optString("ip")?.takeIf { it.isNotBlank() }?.let {
                parts.add("IP：$it")
            }

            parts.joinToString(" · ")
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatTime(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateStr)

            val today = Date()
            val outputDateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

            if (date != null) {
                val todayStr = outputDateFormat.format(today)
                val dateStrFormatted = outputDateFormat.format(date)

                if (todayStr == dateStrFormatted) {
                    "今天 ${outputFormat.format(date)}"
                } else {
                    "${outputDateFormat.format(date)} ${outputFormat.format(date)}"
                }
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.CHINA, "%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format(Locale.CHINA, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    private fun showError(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
        containerList.visibility = View.GONE
    }

    private inline fun <reified T> parseData(data: Any?): T? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<T>() {}.type)
        } catch (_: Exception) { null }
    }
}
