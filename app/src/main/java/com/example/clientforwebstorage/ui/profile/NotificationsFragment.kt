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
import com.example.clientforwebstorage.network.models.NotificationItem
import com.example.clientforwebstorage.network.models.NotificationListData
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvTotalCount: TextView
    private lateinit var tvUnreadCount: TextView
    private lateinit var tvReadCount: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var containerList: LinearLayout
    private lateinit var btnMarkAllRead: TextView

    private lateinit var chipAll: TextView
    private lateinit var chipSystem: TextView
    private lateinit var chipShare: TextView
    private lateinit var chipGroup: TextView
    private lateinit var chipFile: TextView
    private lateinit var chipUnread: TextView

    private lateinit var paginationLayout: LinearLayout
    private lateinit var btnPrevPage: TextView
    private lateinit var btnNextPage: TextView
    private lateinit var tvPageInfo: TextView

    private var currentPage = 1
    private val pageSize = 10
    private var totalPages = 0
    private var totalRecords = 0

    private var currentFilter = "all"
    private var unreadOnly = false

    private var allNotifications: List<NotificationItem> = emptyList()

    private val filterChips by lazy {
        listOf(chipAll, chipSystem, chipShare, chipGroup, chipFile, chipUnread)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        setupFilterChips()
        setupPaginationControls()
        setupMarkAllRead()
        loadNotifications()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_notifications)
        tvTotalCount = view.findViewById(R.id.tv_total_count)
        tvUnreadCount = view.findViewById(R.id.tv_unread_count)
        tvReadCount = view.findViewById(R.id.tv_read_count)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        progressLoading = view.findViewById(R.id.progress_loading)
        containerList = view.findViewById(R.id.container_notification_list)
        btnMarkAllRead = view.findViewById(R.id.btn_mark_all_read)

        chipAll = view.findViewById(R.id.chip_all)
        chipSystem = view.findViewById(R.id.chip_system)
        chipShare = view.findViewById(R.id.chip_share)
        chipGroup = view.findViewById(R.id.chip_group)
        chipFile = view.findViewById(R.id.chip_file)
        chipUnread = view.findViewById(R.id.chip_unread)

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

    private fun setupFilterChips() {
        val chipMap = mapOf(
            "all" to chipAll,
            "system" to chipSystem,
            "share" to chipShare,
            "group" to chipGroup,
            "file" to chipFile,
            "unread" to chipUnread
        )

        chipMap.forEach { (filter, chip) ->
            chip.setOnClickListener {
                currentFilter = filter
                unreadOnly = filter == "unread"
                updateChipSelection(filter)
                currentPage = 1
                loadNotifications()
            }
        }
    }

    private fun updateChipSelection(selectedFilter: String) {
        filterChips.forEach { chip ->
            chip.setBackgroundResource(0)
            chip.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        val selectedChip = when (selectedFilter) {
            "all" -> chipAll
            "system" -> chipSystem
            "share" -> chipShare
            "group" -> chipGroup
            "file" -> chipFile
            "unread" -> chipUnread
            else -> chipAll
        }

        selectedChip.setBackgroundResource(R.drawable.bg_tag_selected)
        selectedChip.setTextColor(resources.getColor(R.color.primary_blue, null))
    }

    private fun setupPaginationControls() {
        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                loadNotifications()
            }
        }

        btnNextPage.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                loadNotifications()
            }
        }
    }

    private fun setupMarkAllRead() {
        btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }
    }

    private fun loadNotifications() {
        progressLoading.visibility = View.VISIBLE
        containerList.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
        paginationLayout.visibility = View.GONE

        val effectiveUnreadOnly = unreadOnly || currentFilter == "unread"

        RetrofitClient.api.getNotifications(currentPage, pageSize, effectiveUnreadOnly)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return
                    progressLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseData<NotificationListData>(response.body()?.data)
                        val notifications = data?.items ?: emptyList()
                        totalRecords = data?.total ?: 0

                        if (totalRecords > 0 && pageSize > 0) {
                            totalPages = (totalRecords + pageSize - 1) / pageSize
                        } else {
                            totalPages = 0
                        }

                        allNotifications = if (currentFilter != "all" && currentFilter != "unread") {
                            notifications.filter { it.type.equals(currentFilter, ignoreCase = true) }
                        } else {
                            notifications
                        }

                        updateStatistics(notifications)
                        displayNotifications(allNotifications)
                        updatePaginationUI()
                    } else {
                        showError("加载失败：${response.body()?.message ?: "未知错误"}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return
                    progressLoading.visibility = View.GONE
                    showError("网络错误：${t.message}")
                    updatePaginationUI()
                }
            })
    }

    private fun updateStatistics(notifications: List<NotificationItem>) {
        val total = totalRecords
        val unread = notifications.count { !it.isRead }
        val read = notifications.count { it.isRead }

        tvTotalCount.text = total.toString()
        tvUnreadCount.text = unread.toString()
        tvReadCount.text = read.toString()
    }

    private fun displayNotifications(notifications: List<NotificationItem>) {
        containerList.removeAllViews()

        if (notifications.isEmpty()) {
            tvEmptyState.text = if (currentFilter == "unread") {
                "没有未读通知\n所有通知都已阅读"
            } else {
                "暂无通知\n您的通知将在这里显示"
            }
            tvEmptyState.visibility = View.VISIBLE
            containerList.visibility = View.GONE
            return
        }

        tvEmptyState.visibility = View.GONE
        containerList.visibility = View.VISIBLE

        notifications.forEach { notification ->
            addNotificationItemView(notification)
        }
    }

    private fun addNotificationItemView(notification: NotificationItem) {
        val itemView = layoutInflater.inflate(R.layout.item_notification, containerList, false)

        val ivIcon = itemView.findViewById<ImageView>(R.id.iv_notification_icon)
        val viewUnreadDot = itemView.findViewById<View>(R.id.view_unread_dot)
        val tvTitle = itemView.findViewById<TextView>(R.id.tv_notification_title)
        val tvTime = itemView.findViewById<TextView>(R.id.tv_notification_time)
        val badgeType = itemView.findViewById<TextView>(R.id.badge_notification_type)
        val tvContent = itemView.findViewById<TextView>(R.id.tv_notification_content)

        val (typeLabel, iconRes, bgColorRes, typeBgRes) = getNotificationTypeInfo(notification.type)
        tvTitle.text = notification.title
        ivIcon.setImageResource(iconRes)
        ivIcon.setBackgroundColor(resources.getColor(bgColorRes, null))
        badgeType.text = typeLabel
        badgeType.setBackgroundResource(typeBgRes)
        tvContent.text = notification.content
        tvTime.text = formatTime(notification.createdAt)

        if (!notification.isRead) {
            viewUnreadDot.visibility = View.VISIBLE
            tvTitle.alpha = 1.0f
            tvContent.alpha = 1.0f
        } else {
            viewUnreadDot.visibility = View.GONE
            tvTitle.alpha = 0.6f
            tvContent.alpha = 0.6f
        }

        itemView.setOnClickListener {
            if (!notification.isRead) {
                markAsRead(notification.notificationId)
            }
            showNotificationDetail(notification)
        }

        itemView.setOnLongClickListener {
            showNotificationOptions(notification)
            true
        }

        containerList.addView(itemView)
    }

    private fun showNotificationDetail(notification: NotificationItem) {
        val scrollView = android.widget.ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
        }

        val (typeLabel, _, _, _) = getNotificationTypeInfo(notification.type)

        container.addView(createDetailRow("标题", notification.title))
        container.addView(createDetailRow("类型", typeLabel))
        container.addView(createDetailRow("状态", if (notification.isRead) "已读" else "未读"))
        container.addView(createDetailRow("内容", notification.content))
        container.addView(createDetailRow("时间", formatDateTimeFull(notification.createdAt)))
        container.addView(createDetailRow("通知ID", notification.notificationId))

        if (notification.data != null && notification.data.isNotEmpty()) {
            container.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(1)
                )
                setBackgroundColor(0xFFE5E7EB.toInt())
                setPadding(0, dpToPx(8), 0, dpToPx(8))
            })

            container.addView(TextView(requireContext()).apply {
                text = "附加数据"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFF333333.toInt())
            })

            notification.data.forEach { (key, value) ->
                container.addView(createDetailRow(key, value?.toString() ?: "null"))
            }
        }

        scrollView.addView(container)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("通知详情")
            .setView(scrollView)
            .setPositiveButton("确定", null)
            .apply {
                if (!notification.isRead) {
                    setNeutralButton("标记已读") { _, _ ->
                        markAsRead(notification.notificationId)
                    }
                }
            }
            .show()
    }

    private fun showNotificationOptions(notification: NotificationItem) {
        val options = mutableListOf<String>()

        if (!notification.isRead) {
            options.add("标记为已读")
        }
        options.add("删除通知")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("操作")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "标记为已读" -> markAsRead(notification.notificationId)
                    "删除通知" -> deleteNotification(notification.notificationId)
                }
            }
            .show()
    }

    private fun createDetailRow(label: String, value: String): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }

        row.addView(TextView(requireContext()).apply {
            text = label
            textSize = 12f
            setTextColor(0xFF666666.toInt())
        })

        row.addView(TextView(requireContext()).apply {
            text = value
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
        })

        return row
    }

    private fun markAsRead(notificationId: String) {
        RetrofitClient.api.markNotificationRead(notificationId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已标记为已读", Toast.LENGTH_SHORT).show()
                        loadNotifications()
                    } else {
                        Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun markAllAsRead() {
        RetrofitClient.api.markAllNotificationsRead()
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已全部标记为已读", Toast.LENGTH_SHORT).show()
                        loadNotifications()
                    } else {
                        Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteNotification(notificationId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除通知")
            .setMessage("确定要删除这条通知吗？")
            .setPositiveButton("删除") { _, _ ->
                RetrofitClient.api.deleteNotification(notificationId)
                    .enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (!isAdded) return
                            if (response.isSuccessful && response.body()?.code == 0) {
                                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                                loadNotifications()
                            } else {
                                Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            if (!isAdded) return
                            Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("取消", null)
            .show()
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

    private fun getNotificationTypeInfo(type: String): NotificationTypeInfo {
        return when (type.lowercase()) {
            "system" -> NotificationTypeInfo(
                "系统",
                android.R.drawable.ic_dialog_info,
                R.color.notification_icon_bg_system,
                R.drawable.bg_notification_type_system
            )
            "share" -> NotificationTypeInfo(
                "分享",
                android.R.drawable.ic_menu_share,
                R.color.notification_icon_bg_share,
                R.drawable.bg_notification_type_share
            )
            "group" -> NotificationTypeInfo(
                "群组",
                android.R.drawable.ic_menu_myplaces,
                R.color.notification_icon_bg_group,
                R.drawable.bg_notification_type_group
            )
            "file" -> NotificationTypeInfo(
                "文件",
                android.R.drawable.ic_menu_save,
                R.color.notification_icon_bg_file,
                R.drawable.bg_notification_type_file
            )
            else -> NotificationTypeInfo(
                "通知",
                android.R.drawable.ic_dialog_info,
                R.color.notification_icon_bg_system,
                R.drawable.bg_notification_type_system
            )
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

    private fun formatDateTimeFull(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun showError(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
        containerList.visibility = View.GONE
    }

    private fun dpToPx(dp: Int): Int = (dp * requireContext().resources.displayMetrics.density).toInt()

    private inline fun <reified T> parseData(data: Any?): T? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<T>() {}.type)
        } catch (_: Exception) {
            null
        }
    }

    private data class NotificationTypeInfo(
        val label: String,
        val iconRes: Int,
        val bgColorRes: Int,
        val typeBgRes: Int
    )
}
