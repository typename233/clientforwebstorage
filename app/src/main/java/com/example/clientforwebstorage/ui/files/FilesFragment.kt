package com.example.clientforwebstorage.ui.files

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.CompletedPart
import com.example.clientforwebstorage.network.models.CreateFolderRequest
import com.example.clientforwebstorage.network.models.CreateShareRequest
import com.example.clientforwebstorage.network.models.RenameRequest
import com.example.clientforwebstorage.network.models.Resource
import com.example.clientforwebstorage.network.models.ResourceListData
import com.example.clientforwebstorage.network.models.UploadCompleteRequest
import com.example.clientforwebstorage.network.models.UploadInitRequest
import com.example.clientforwebstorage.ui.PreviewActivity
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FilesFragment : Fragment() {

    private var currentParentId: String? = null
    private val pathStack = mutableListOf<Pair<String?, String>>()
    private var currentResources: List<Resource> = emptyList()
    private var currentCategory: String = "全部"
    private var currentSortType: SortType = SortType.TIME_DESC
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FileListAdapter
    private lateinit var categoryContainer: LinearLayout
    private lateinit var storageProgress: LinearProgressIndicator
    private lateinit var pathContainer: LinearLayout
    private var requestPickFiles: (() -> Unit)? = null
    private var uploadDialog: androidx.appcompat.app.AlertDialog? = null
    private var uploadProgressIndicator: LinearProgressIndicator? = null
    private var uploadProgressText: TextView? = null

    enum class SortType {
        NAME_ASC, NAME_DESC, TIME_DESC, TIME_ASC, SIZE_DESC, SIZE_ASC
    }

    fun setRequestPickFiles(callback: () -> Unit) {
        requestPickFiles = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_files, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FavoritesManager.init(requireContext())

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        recycler = view.findViewById(R.id.recycler_files)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add)
        storageProgress = view.findViewById(R.id.storage_progress)
        categoryContainer = view.findViewById(R.id.chip_container)
        pathContainer = view.findViewById(R.id.path_container)

        val btnMore = view.findViewById<ImageButton>(R.id.btn_more)
        btnMore.setOnClickListener { showMoreMenu(it) }

        setupCategoryChips()

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = FileListAdapter(emptyList(),
            onItemClick = { item -> onFileItemClick(item) },
            onItemLongClick = { item -> onFileItemLongClick(item) },
            onFavoriteClick = { item -> toggleFavorite(item) }
        )
        recycler.adapter = adapter

        fab.setOnClickListener { showActionBottomSheet() }

        loadResources()
    }

    private fun showMoreMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_files_menu, null)

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            setBackgroundDrawable(resources.getDrawable(android.R.color.white, null))
            isOutsideTouchable = true
            isFocusable = true
        }

        popupView.findViewById<View>(R.id.menu_refresh)?.setOnClickListener {
            loadResources()
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_sort)?.setOnClickListener {
            popupWindow.dismiss()
            showSortMenu(anchor)
        }

        val offsetX = anchor.width - popupWidth
        popupWindow.showAsDropDown(anchor, offsetX, 0)
    }

    private fun showSortMenu(anchor: View) {
        val sortPopupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_sort_menu, null)

        sortPopupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val sortPopupWidth = sortPopupView.measuredWidth

        val sortPopupWindow = PopupWindow(
            sortPopupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            setBackgroundDrawable(resources.getDrawable(android.R.color.white, null))
            isOutsideTouchable = true
            isFocusable = true
        }

        val sortOptions = mapOf(
            R.id.sort_name_asc to SortType.NAME_ASC,
            R.id.sort_name_desc to SortType.NAME_DESC,
            R.id.sort_time_desc to SortType.TIME_DESC,
            R.id.sort_time_asc to SortType.TIME_ASC,
            R.id.sort_size_desc to SortType.SIZE_DESC,
            R.id.sort_size_asc to SortType.SIZE_ASC
        )

        sortOptions.forEach { (id, sortType) ->
            sortPopupView.findViewById<View>(id)?.setOnClickListener {
                currentSortType = sortType
                displayResources(currentResources)
                sortPopupWindow.dismiss()
            }
        }

        updateSortMenuUI(sortPopupView)

        val offsetX = anchor.width - sortPopupWidth
        sortPopupWindow.showAsDropDown(anchor, offsetX, 0)
    }
    
    private fun updateSortMenuUI(popupView: View) {
        val sortTypes = mapOf(
            R.id.sort_name_asc to SortType.NAME_ASC,
            R.id.sort_name_desc to SortType.NAME_DESC,
            R.id.sort_time_desc to SortType.TIME_DESC,
            R.id.sort_time_asc to SortType.TIME_ASC,
            R.id.sort_size_desc to SortType.SIZE_DESC,
            R.id.sort_size_asc to SortType.SIZE_ASC
        )
        
        sortTypes.forEach { (id, sortType) ->
            val itemView = popupView.findViewById<TextView>(id)
            itemView?.alpha = if (currentSortType == sortType) 1.0f else 0.6f
        }
    }

    private fun setupCategoryChips() {
        val categories = listOf("全部", "图片", "文档", "视频")
        categories.forEach { cat ->
            val chip = TextView(requireContext()).apply {
                text = cat
                textSize = 12f
                setPadding(dpToPx(10), dpToPx(2), dpToPx(10), dpToPx(2))
                background = if (cat == currentCategory) resources.getDrawable(R.drawable.bg_chip_selected, null)
                    else resources.getDrawable(R.drawable.bg_chip_outline, null)
                setTextColor(if (cat == currentCategory) resources.getColor(R.color.primary_blue, null)
                    else resources.getColor(R.color.white, null))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dpToPx(8) }
                setOnClickListener { selectCategory(cat) }
            }
            categoryContainer.addView(chip)
        }
    }

    private fun selectCategory(category: String) {
        currentCategory = category
        for (i in 0 until categoryContainer.childCount) {
            val chip = categoryContainer.getChildAt(i) as TextView
            val catText = chip.text.toString()
            val isSelected = catText == category
            chip.background = if (isSelected) resources.getDrawable(R.drawable.bg_chip_selected, null)
                else resources.getDrawable(R.drawable.bg_chip_outline, null)
            chip.setTextColor(if (isSelected) resources.getColor(R.color.primary_blue, null)
                else resources.getColor(R.color.white, null))
        }
        displayResources(currentResources)
    }

    private fun showActionBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_bottom_actions, null)

        sheetView.findViewById<View>(R.id.btn_create_folder).setOnClickListener {
            dialog.dismiss()
            showCreateFolderDialog()
        }

        sheetView.findViewById<View>(R.id.btn_import_file).setOnClickListener {
            dialog.dismiss()
            requestPickFiles?.invoke()
        }

        dialog.setContentView(sheetView)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.5).toInt()
            }
        }
        dialog.show()
    }

    private fun showCreateFolderDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "请输入文件夹名称"
            setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14))
            isSingleLine = true
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("新建文件夹")
            .setView(editText)
            .setPositiveButton("创建") { _, _ ->
                val name = editText.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) createFolder(name)
                else Toast.makeText(requireContext(), "请输入文件夹名称", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createFolder(name: String) {
        RetrofitClient.api.createFolder(CreateFolderRequest(currentParentId, name))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "文件夹已创建: $name", Toast.LENGTH_SHORT).show()
                        loadResources()
                    } else {
                        Toast.makeText(requireContext(), "创建失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showItemActionSheet(item: FileItem) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_item_actions, null)

        sheetView.findViewById<View>(R.id.btn_rename).setOnClickListener {
            dialog.dismiss()
            showRenameDialog(item)
        }
        sheetView.findViewById<View>(R.id.btn_delete).setOnClickListener {
            dialog.dismiss()
            showDeleteConfirm(item)
        }

        if (item.type == FileType.FILE) {
            sheetView.findViewById<View>(R.id.btn_download).visibility = View.VISIBLE
            sheetView.findViewById<View>(R.id.btn_download).setOnClickListener {
                dialog.dismiss()
                downloadFile(item)
            }
            sheetView.findViewById<View>(R.id.btn_share).visibility = View.VISIBLE
            sheetView.findViewById<View>(R.id.btn_share).setOnClickListener {
                dialog.dismiss()
                shareResource(item)
            }
            
            val layoutFavoriteRow = sheetView.findViewById<View>(R.id.layout_favorite_row)
            val btnFavorite = sheetView.findViewById<View>(R.id.btn_favorite)
            val ivFavoriteIcon = sheetView.findViewById<android.widget.ImageView>(R.id.iv_favorite_icon)
            val tvFavoriteText = sheetView.findViewById<TextView>(R.id.tv_favorite_text)

            layoutFavoriteRow.visibility = View.VISIBLE
            updateFavoriteButtonUI(item.isFavorite, ivFavoriteIcon, tvFavoriteText)

            btnFavorite.setOnClickListener {
                dialog.dismiss()
                toggleFavoriteWithAnimation(item)
            }
        } else {
            sheetView.findViewById<View>(R.id.btn_download).visibility = View.GONE
            sheetView.findViewById<View>(R.id.btn_share).visibility = View.GONE
            
            sheetView.findViewById<View>(R.id.layout_favorite_row).visibility = View.GONE
        }

        dialog.setContentView(sheetView)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.5).toInt()
            }
        }
        dialog.show()
    }

    private fun updateFavoriteButtonUI(
        isFavorited: Boolean,
        icon: android.widget.ImageView,
        text: TextView
    ) {
        if (isFavorited) {
            icon.setImageResource(android.R.drawable.btn_star_big_on)
            icon.setColorFilter(android.graphics.Color.parseColor("#FFA000"))
            text.text = "取消收藏"
            text.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        } else {
            icon.setImageResource(android.R.drawable.btn_star_big_off)
            icon.setColorFilter(android.graphics.Color.parseColor("#FFA000"))
            text.text = "收藏"
            text.setTextColor(android.graphics.Color.parseColor("#FFA000"))
        }
    }

    private fun showRenameDialog(item: FileItem) {
        val editText = EditText(requireContext()).apply {
            setText(item.name)
            isSingleLine = true
            setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14))
            setSelection(item.name.length)
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("重命名")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text?.toString()?.trim().orEmpty()
                if (newName.isNotEmpty()) renameResource(item.id, newName)
                else Toast.makeText(requireContext(), "名称不能为空", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun renameResource(resourceId: String, newName: String) {
        RetrofitClient.api.renameResource(resourceId, RenameRequest(newName))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已重命名", Toast.LENGTH_SHORT).show()
                        loadResources()
                    } else {
                        Toast.makeText(requireContext(), "重命名失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showDeleteConfirm(item: FileItem) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除确认")
            .setMessage("确定要删除「${item.name}」？")
            .setPositiveButton("删除") { _, _ -> deleteResource(item.id) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteResource(resourceId: String) {
        RetrofitClient.api.deleteResource(resourceId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                        loadResources()
                    } else {
                        Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun downloadFile(item: FileItem) {
        android.util.Log.d("DownloadDebug", "开始下载文件: ${item.name}, ID: ${item.id}")

        RetrofitClient.api.getDownloadUrl(item.id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse

                    android.util.Log.d("DownloadDebug", "响应状态: ${response.isSuccessful}")
                    android.util.Log.d("DownloadDebug", "响应code: ${response.body()?.code}")

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = response.body()?.data

                        val url = parseStringFromData(data, "url")
                            ?: parseStringFromData(data, "downloadUrl")
                            ?: parseStringFromData(data, "download_url")
                            ?: parseStringFromData(data, "link")

                        android.util.Log.d("DownloadDebug", "解析到的URL: $url")

                        if (!url.isNullOrEmpty()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                                android.util.Log.d("DownloadDebug", "已启动浏览器打开下载链接")
                                Toast.makeText(requireContext(), "正在下载: ${item.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.util.Log.e("DownloadDebug", "打开下载链接失败", e)
                                Toast.makeText(requireContext(), "无法打开下载链接: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            android.util.Log.e("DownloadDebug", "无法解析出URL字段, data=$data")
                            Toast.makeText(requireContext(), "获取下载链接失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.util.Log.e("DownloadDebug", "API返回错误: code=${response.body()?.code}, message=${response.body()?.message}")
                        Toast.makeText(requireContext(), "获取下载链接失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    android.util.Log.e("DownloadDebug", "网络请求失败", t)
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun shareResource(item: FileItem) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_share, null)

        sheetView.findViewById<TextView>(R.id.tv_file_name).text = item.name

        val switchNeedCode = sheetView.findViewById<Switch>(R.id.switch_need_code)
        val layoutCodeInput = sheetView.findViewById<View>(R.id.layout_code_input)
        val etShareCode = sheetView.findViewById<EditText>(R.id.et_share_code)
        val switchAllowPreview = sheetView.findViewById<Switch>(R.id.switch_allow_preview)
        val switchAllowDownload = sheetView.findViewById<Switch>(R.id.switch_allow_download)

        switchNeedCode.setOnCheckedChangeListener { _, isChecked ->
            layoutCodeInput.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                etShareCode.requestFocus()
            }
        }

        sheetView.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.btn_create_share).setOnClickListener {
            val needCode = switchNeedCode.isChecked
            var code: String? = etShareCode.text?.toString()?.trim()

            if (needCode && code.isNullOrBlank()) {
                code = null
            } else if (needCode && code != null) {
                if (!code.matches(Regex("^[a-zA-Z0-9]{4,6}$"))) {
                    Toast.makeText(requireContext(), "提取码为 4-6 位字母或数字", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            dialog.dismiss()

            createShareWithSettings(
                item = item,
                needCode = needCode,
                code = code,
                allowPreview = switchAllowPreview.isChecked,
                allowDownload = switchAllowDownload.isChecked
            )
        }

        dialog.setContentView(sheetView)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.5).toInt()
            }
        }
        dialog.show()
    }

    private fun createShareWithSettings(
        item: FileItem,
        needCode: Boolean,
        code: String?,
        allowPreview: Boolean,
        allowDownload: Boolean
    ) {
        RetrofitClient.api.createShare(CreateShareRequest(
            resourceIds = listOf(item.id),
            expiredAt = null,
            needCode = needCode,
            code = code,
            allowPreview = allowPreview,
            allowDownload = allowDownload,
            maxAccessCount = null
        ))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val shareCode = parseStringFromData(response.body()?.data, "shareCode")
                            ?: parseStringFromData(response.body()?.data, "code")

                        showShareResultDialog(
                            fileName = item.name,
                            shareCode = shareCode ?: "未知",
                            resourceId = item.id,
                            hasCode = needCode,
                            userCode = code
                        )
                    } else {
                        Toast.makeText(requireContext(), "分享失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showShareResultDialog(fileName: String, shareCode: String, resourceId: String, hasCode: Boolean, userCode: String?) {
        val message = StringBuilder()
        message.appendLine("文件：$fileName")
        message.appendLine()
        message.appendLine("分享码：$shareCode")

        if (hasCode) {
            message.appendLine()
            message.appendLine("提取码：${userCode ?: "（系统自动生成）"}")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✅ 分享成功")
            .setMessage(message.toString())
            .setPositiveButton("复制下载链接") { _, _ ->
                fetchAndCopyShareDownloadUrl(shareCode, resourceId)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun fetchAndCopyShareDownloadUrl(shareCode: String, resourceId: String) {
        Toast.makeText(requireContext(), "正在获取下载链接...", Toast.LENGTH_SHORT).show()

        android.util.Log.d("ShareDebug", "请求分享下载链接: shareCode=$shareCode, resourceId=$resourceId")

        RetrofitClient.api.getShareDownloadUrl(shareCode, resourceId, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse

                    android.util.Log.d("ShareDebug", "HTTP状态码: ${response.code()}")
                    android.util.Log.d("ShareDebug", "响应体: ${response.body()}")

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = response.body()?.data
                        val downloadUrl = parseStringFromData(data, "url")

                        if (!downloadUrl.isNullOrEmpty()) {
                            copyToClipboard(downloadUrl)
                            android.util.Log.d("ShareDebug", "成功获取下载URL: $downloadUrl")
                        } else {
                            android.util.Log.e("ShareDebug", "无法解析分享下载URL, data=$data")
                            fallbackToSharePageLink(shareCode, null)
                        }
                    } else {
                        val errorMsg = response.body()?.message ?: "HTTP ${response.code()}"
                        android.util.Log.e("ShareDebug", "获取分享下载链接失败: HTTP=${response.code()}, code=${response.body()?.code}, msg=$errorMsg")

                        fallbackToSharePageLink(shareCode, null)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    android.util.Log.e("ShareDebug", "网络请求失败", t)

                    fallbackToSharePageLink(shareCode, null)
                }
            })
    }

    private fun fallbackToSharePageLink(shareCode: String, verifyToken: String? = null) {
        var sharePageLink = "http://115.29.173.36:8081/s/$shareCode"
        
        copyToClipboard(sharePageLink)
        Toast.makeText(requireContext(), "✅ 已复制分享页面链接（下载链接不可用时自动降级）", Toast.LENGTH_LONG).show()
        android.util.Log.d("ShareDebug", "降级为分享页面链接: $sharePageLink")
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("分享链接", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "链接已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun parseStringFromData(data: Any?, key: String): String? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            val map = Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type) as? Map<String, Any>
            map?.get(key)?.toString()
        } catch (_: Exception) { null }
    }

    private fun spToPx(sp: Float): Float {
        return sp * requireContext().resources.displayMetrics.scaledDensity
    }

    private fun updatePathDisplay() {
        pathContainer.removeAllViews()

        val pathRow = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val rootItem = TextView(requireContext()).apply {
            text = "📁"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            isClickable = true
            isFocusable = true
            setPadding(0, dpToPx(4), dpToPx(4), dpToPx(4))
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener {
                if (pathStack.isNotEmpty()) {
                    while (pathStack.isNotEmpty()) {
                        pathStack.removeAt(pathStack.size - 1)
                    }
                    currentParentId = null
                    loadResources()
                }
            }
        }
        pathRow.addView(rootItem)

        pathStack.forEachIndexed { index, entry ->
            val separator = TextView(requireContext()).apply {
                text = " › "
                textSize = 16f
                setTextColor(0xB3FFFFFF.toInt())
            }
            pathRow.addView(separator)

            val pathItem = TextView(requireContext()).apply {
                text = entry.second
                textSize = 16f
                maxLines = Int.MAX_VALUE
                if (index == pathStack.size - 1) {
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFFFFFFFF.toInt())
                    isClickable = false
                    isFocusable = false
                } else {
                    setTextColor(0xE0FFFFFF.toInt())
                    isClickable = true
                    isFocusable = true
                    setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        while (pathStack.size > index + 1) {
                            pathStack.removeAt(pathStack.size - 1)
                        }
                        currentParentId = entry.first
                        loadResources()
                    }
                }
            }
            pathRow.addView(pathItem)
        }

        pathContainer.addView(pathRow)

        if (currentParentId == null && pathStack.isEmpty()) {
            val rootLabel = TextView(requireContext()).apply {
                text = " 根目录"
                textSize = 16f
                setTextColor(0x99FFFFFF.toInt())
            }
            pathRow.addView(rootLabel)
        }
    }

    private fun loadResources() {
        updatePathDisplay()
        RetrofitClient.api.getResources(currentParentId, null, 1, 50)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            val data = parseResourceListData(apiResponse.data)
                            if (data != null) displayResources(data.items)
                            else displayResources(emptyList())
                        } else {
                            Toast.makeText(requireContext(), "加载失败: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun parseResourceListData(data: Any?): ResourceListData? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<ResourceListData>() {}.type)
        } catch (e: Exception) { null }
    }

    private fun displayResources(resources: List<Resource>) {
        currentResources = resources
        val filtered = filterByCategory(resources)
        val sorted = sortResources(filtered)

        val items = mutableListOf<FileItem>()
        if (currentParentId != null) {
            items.add(FileItem("..", ".. 返回上级", FileType.FOLDER, updatedAt = ""))
        }
        sorted.forEach { res ->
            val isFav = FavoritesManager.isFavorite(res.id)
            items.add(FileItem(res.id, res.name,
                if (res.type == "folder") FileType.FOLDER else FileType.FILE,
                res.size, res.extension, res.updatedAt ?: "", isFav))
        }
        adapter = FileListAdapter(items,
            onItemClick = { item -> onFileItemClick(item) },
            onItemLongClick = { item -> onFileItemLongClick(item) },
            onFavoriteClick = { item -> toggleFavorite(item) }
        )
        recycler.adapter = adapter
    }

    private fun sortResources(resources: List<Resource>): List<Resource> {
        return when (currentSortType) {
            SortType.NAME_ASC -> resources.sortedWith(
                compareBy<Resource> { if (it.type != "folder") 1 else 0 }
                    .thenBy { it.name?.lowercase() ?: "" })
            SortType.NAME_DESC -> resources.sortedWith(
                compareBy<Resource> { if (it.type != "folder") 1 else 0 }
                    .thenByDescending { it.name?.lowercase() ?: "" })
            SortType.TIME_DESC -> resources.sortedWith(
                compareBy<Resource> { if (it.type != "folder") 1 else 0 }
                    .thenByDescending { it.updatedAt ?: "" })
            SortType.TIME_ASC -> resources.sortedWith(
                compareBy<Resource> { if (it.type != "folder") 1 else 0 }
                    .thenBy { it.updatedAt ?: "" })
            SortType.SIZE_DESC -> resources.sortedWith(
                compareBy<Resource> { if (it.type != "folder") 1 else 0 }
                    .thenByDescending { it.size ?: 0L })
            SortType.SIZE_ASC -> resources.sortedWith(
                compareBy<Resource> { if (it.type != "folder") 1 else 0 }
                    .thenBy { it.size ?: 0L })
        }
    }

    private fun filterByCategory(resources: List<Resource>): List<Resource> {
        return resources.filter { res ->
            if (res.type == "folder") true else when (currentCategory) {
                "全部" -> true
                "图片" -> isImageFile(res.extension)
                "文档" -> isDocumentFile(res.extension) || !isImageFile(res.extension) && !isVideoFile(res.extension)
                "视频" -> isVideoFile(res.extension)
                else -> true
            }
        }
    }

    private fun isImageFile(ext: String?) = ext?.lowercase() in listOf("jpg","jpeg","png","gif","bmp","webp")
    private fun isDocumentFile(ext: String?) = ext?.lowercase() in listOf("pdf","doc","docx","xls","xlsx","ppt","pptx","txt","log")
    private fun isVideoFile(ext: String?) = ext?.lowercase() in listOf("mp4","avi","mov","mkv","flv")

    private fun onFileItemClick(item: FileItem) {
        if (item.id == "..") {
            if (pathStack.isNotEmpty()) {
                val entry = pathStack.removeAt(pathStack.size - 1)
                currentParentId = entry.first
                loadResources()
            }
            return
        }
        if (item.type == FileType.FOLDER) {
            pathStack.add(Pair(currentParentId, item.name))
            currentParentId = item.id
            loadResources()
        } else {
            startActivity(Intent(requireContext(), PreviewActivity::class.java).apply {
                putExtra("resource_id", item.id)
                putExtra("resource_name", item.name)
            })
        }
    }

    private fun onFileItemLongClick(item: FileItem): Boolean {
        if (item.id == "..") return false
        showItemActionSheet(item)
        return true
    }

    private fun toggleFavorite(item: FileItem) {
        val newFavoriteState = !item.isFavorite

        if (newFavoriteState) {
            RetrofitClient.api.favoriteResource(item.id)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (!isAdded) return@onResponse
                        if (response.isSuccessful && response.body()?.code == 0) {
                            FavoritesManager.addFavorite(item.id)
                            item.isFavorite = true
                            updateAdapterItemFavoriteStatus(item.id, true)
                            showFavoriteAnimation(true)
                            Toast.makeText(requireContext(), "已添加到收藏", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "收藏失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        if (!isAdded) return@onFailure
                        Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            RetrofitClient.api.unfavoriteResource(item.id)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (!isAdded) return@onResponse
                        if (response.isSuccessful && response.body()?.code == 0) {
                            FavoritesManager.removeFavorite(item.id)
                            item.isFavorite = false
                            updateAdapterItemFavoriteStatus(item.id, false)
                            showFavoriteAnimation(false)
                            Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "取消收藏失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        if (!isAdded) return@onFailure
                        Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun toggleFavoriteWithAnimation(item: FileItem) {
        toggleFavorite(item)
    }

    private fun updateAdapterItemFavoriteStatus(resourceId: String, isFavorited: Boolean) {
        val currentPosition = adapter.currentList.indexOfFirst { it.id == resourceId }
        if (currentPosition >= 0) {
            adapter.currentList[currentPosition].isFavorite = isFavorited
            adapter.notifyItemChanged(currentPosition)
        }
    }

    private fun showFavoriteAnimation(isFavorited: Boolean) {
        view?.let { rootView ->
            val overlayView = View(requireContext()).apply {
                setBackgroundColor(if (isFavorited) 0x1AFFA000 else 0x1A999999)
                alpha = 0f
            }

            val params = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )

            (rootView as ViewGroup).addView(overlayView, params)

            overlayView.animate()
                .alpha(1.0f)
                .setDuration(150)
                .withEndAction {
                    overlayView.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction { (rootView as ViewGroup).removeView(overlayView) }
                        .start()
                }
                .start()
        }
    }

    fun handleBack(): Boolean {
        if (currentParentId != null && pathStack.isNotEmpty()) {
            val entry = pathStack.removeAt(pathStack.size - 1)
            currentParentId = entry.first
            loadResources()
            return true
        }
        return false
    }

    private val PART_SIZE = 5 * 1024 * 1024L // 5MB per part

    fun handleUpload(uri: Uri) {
        val fileName = getFileName(uri)
        val fileSize = getFileSize(uri)

        if (fileSize <= 0) {
            Toast.makeText(requireContext(), "无法获取文件大小", Toast.LENGTH_SHORT).show()
            return
        }

        showUploadDialog(fileName)
        uploadFile(uri, fileName, fileSize)
    }

    private fun getFileSize(uri: Uri): Long {
        var size: Long = 0
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && idx >= 0) size = cursor.getLong(idx)
        }
        return size
    }

    private fun showUploadDialog(fileName: String) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(20))

            addView(TextView(requireContext()).apply {
                text = "正在上传: $fileName"
                textSize = 16f
                setPadding(0, 0, 0, dpToPx(16))
            })

            uploadProgressIndicator = LinearProgressIndicator(requireContext()).apply {
                isIndeterminate = false
                progress = 0
            }
            addView(uploadProgressIndicator!!)

            uploadProgressText = TextView(requireContext()).apply {
                text = "准备上传..."
                textSize = 12f
                setPadding(0, dpToPx(8), 0, 0)
            }
            addView(uploadProgressText!!)
        }

        uploadDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("文件上传")
            .setView(layout)
            .setCancelable(false)
            .show()
    }

    private fun updateProgress(progress: Int, message: String) {
        activity?.runOnUiThread {
            uploadProgressIndicator?.progress = progress
            uploadProgressText?.text = message
        }
    }

    private fun dismissUploadDialog() {
        activity?.runOnUiThread {
            uploadDialog?.dismiss()
            uploadDialog = null
        }
    }

    private fun uploadFile(uri: Uri, fileName: String, fileSize: Long) {
        val request = UploadInitRequest(
            parentId = currentParentId,
            filename = fileName,
            size = fileSize,
            sha256 = null,
            partSize = PART_SIZE
        )

        RetrofitClient.api.initUpload(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (!isAdded) return@onResponse
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    val uploadId = parseStringFromData(data, "uploadId")
                    val uploadedPartsJson = parseArrayFromData(data, "uploadedParts")

                    if (uploadId != null) {
                        val existingParts = uploadedPartsJson ?: emptyList()
                        if (existingParts.isNotEmpty()) {
                            uploadNextPart(uri, fileSize, uploadId, existingParts.size + 1, existingParts)
                        } else {
                            uploadNextPart(uri, fileSize, uploadId, 1, emptyList())
                        }
                    } else {
                        updateProgress(0, "初始化失败: 未获取到uploadId")
                    }
                } else {
                    updateProgress(0, "初始化失败: ${response.body()?.message}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                if (!isAdded) return@onFailure
                updateProgress(0, "网络错误: ${t.message}")
            }
        })
    }

    private fun uploadNextPart(
        uri: Uri,
        fileSize: Long,
        uploadId: String,
        partNumber: Int,
        uploadedParts: List<CompletedPart>
    ) {
        val totalParts = (fileSize + PART_SIZE - 1) / PART_SIZE

        if (partNumber > totalParts) {
            completeUpload(uploadId, uploadedParts)
            return
        }

        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream == null) {
                updateProgress(0, "无法打开文件")
                return
            }

            val offset = (partNumber - 1) * PART_SIZE
            inputStream.skip(offset)

            val bytesToRead = minOf(PART_SIZE, fileSize - offset)
            val buffer = ByteArray(bytesToRead.toInt())
            var totalBytesRead = inputStream.read(buffer, 0, bytesToRead.toInt())

            while (totalBytesRead < bytesToRead.toInt() && totalBytesRead != -1) {
                val read = inputStream.read(buffer, totalBytesRead, bytesToRead.toInt() - totalBytesRead)
                if (read == -1) break
                totalBytesRead += read
            }

            inputStream.close()

            if (totalBytesRead <= 0) {
                updateProgress(0, "读取文件失败")
                return
            }

            val requestBody = buffer.sliceArray(0 until totalBytesRead).toRequestBody()
            val progress = ((partNumber - 1).toFloat() / totalParts * 100).toInt()
            updateProgress(progress, "上传中... $progress%")

            RetrofitClient.api.uploadPart(uploadId, partNumber, requestBody)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(
                        call: Call<ApiResponse>,
                        response: Response<ApiResponse>
                    ) {
                        if (!isAdded) return@onResponse
                        if (response.isSuccessful && response.body()?.code == 0) {
                            val data = response.body()?.data
                            val etag = parseStringFromData(data, "etag") ?: ""

                            val newPart = CompletedPart(partNumber, etag)
                            val updatedParts = uploadedParts + newPart

                            uploadNextPart(uri, fileSize, uploadId, partNumber + 1, updatedParts)
                        } else {
                            updateProgress(0, "上传分片 $partNumber 失败")
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        if (!isAdded) return@onFailure
                        updateProgress(0, "网络错误: ${t.message}")
                    }
                })
        } catch (e: Exception) {
            updateProgress(0, "读取文件失败: ${e.message}")
        }
    }

    private fun completeUpload(uploadId: String, uploadedParts: List<CompletedPart>) {
        updateProgress(100, "正在完成上传...")

        val request = UploadCompleteRequest(parts = uploadedParts)

        RetrofitClient.api.completeUpload(uploadId, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (!isAdded) return@onResponse
                    if (response.isSuccessful && response.body()?.code == 0) {
                        dismissUploadDialog()
                        Toast.makeText(requireContext(), "文件上传成功", Toast.LENGTH_SHORT).show()
                        loadResources()
                    } else {
                        updateProgress(100, "完成上传失败: ${response.body()?.message}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    updateProgress(100, "网络错误: ${t.message}")
                }
            })
    }

    private fun parseArrayFromData(data: Any?, key: String): List<CompletedPart>? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            val map = Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type) as? Map<String, Any>
            val listJson = Gson().toJson(map?.get(key))
            Gson().fromJson(listJson, object : TypeToken<List<CompletedPart>>() {}.type)
        } catch (_: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }

    private fun dpToPx(dp: Int): Int = (dp * requireContext().resources.displayMetrics.density).toInt()
}
