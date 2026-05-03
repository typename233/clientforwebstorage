package com.example.clientforwebstorage.ui.files

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.CreateFolderRequest
import com.example.clientforwebstorage.network.models.CreateShareRequest
import com.example.clientforwebstorage.network.models.RenameRequest
import com.example.clientforwebstorage.network.models.Resource
import com.example.clientforwebstorage.network.models.ResourceListData
import com.example.clientforwebstorage.ui.PreviewActivity
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
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: FileListAdapter
    private lateinit var categoryContainer: LinearLayout
    private lateinit var storageProgress: LinearProgressIndicator
    private var requestPickFiles: (() -> Unit)? = null

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

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        recycler = view.findViewById(R.id.recycler_files)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add)
        storageProgress = view.findViewById(R.id.storage_progress)
        categoryContainer = view.findViewById(R.id.chip_container)

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> Toast.makeText(requireContext(), "搜索", Toast.LENGTH_SHORT).show()
                R.id.action_more -> Toast.makeText(requireContext(), "更多", Toast.LENGTH_SHORT).show()
            }
            true
        }

        setupCategoryChips()

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = FileListAdapter(emptyList(),
            onItemClick = { item -> onFileItemClick(item) },
            onItemLongClick = { item -> onFileItemLongClick(item) }
        )
        recycler.adapter = adapter

        fab.setOnClickListener { showActionBottomSheet() }

        loadResources()
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
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "文件夹已创建: $name", Toast.LENGTH_SHORT).show()
                        loadResources()
                    } else {
                        Toast.makeText(requireContext(), "创建失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
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
        } else {
            sheetView.findViewById<View>(R.id.btn_download).visibility = View.GONE
            sheetView.findViewById<View>(R.id.btn_share).visibility = View.GONE
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
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已重命名", Toast.LENGTH_SHORT).show()
                        loadResources()
                    } else {
                        Toast.makeText(requireContext(), "重命名失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
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
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                        loadResources()
                    } else {
                        Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun downloadFile(item: FileItem) {
        RetrofitClient.api.getDownloadUrl(item.id)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val url = parseStringFromData(response.body()?.data, "downloadUrl")
                        if (url != null) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } else {
                            Toast.makeText(requireContext(), "获取下载链接失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "获取下载链接失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
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
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val shareCode = parseStringFromData(response.body()?.data, "shareCode")
                            ?: parseStringFromData(response.body()?.data, "code")

                        showShareResultDialog(
                            fileName = item.name,
                            shareCode = shareCode ?: "未知",
                            hasCode = needCode,
                            userCode = code
                        )
                    } else {
                        Toast.makeText(requireContext(), "分享失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showShareResultDialog(fileName: String, shareCode: String, hasCode: Boolean, userCode: String?) {
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
            .setPositiveButton("复制链接") { _, _ ->
                val shareLink = "https://your-domain.com/s/$shareCode${if (hasCode && !userCode.isNullOrBlank()) "?code=$userCode" else ""}"
                copyToClipboard(shareLink)
            }
            .setNegativeButton("关闭", null)
            .show()
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

    private fun loadResources() {
        RetrofitClient.api.getResources(currentParentId, null, 1, 50)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
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

        val items = mutableListOf<FileItem>()
        if (currentParentId != null) {
            items.add(FileItem("..", ".. 返回上级", FileType.FOLDER, updatedAt = ""))
        }
        filtered.forEach { res ->
            items.add(FileItem(res.id, res.name,
                if (res.type == "folder") FileType.FOLDER else FileType.FILE,
                res.size, res.extension, res.updatedAt ?: ""))
        }
        adapter = FileListAdapter(items,
            onItemClick = { item -> onFileItemClick(item) },
            onItemLongClick = { item -> onFileItemLongClick(item) }
        )
        recycler.adapter = adapter
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

    fun handleBack(): Boolean {
        if (currentParentId != null && pathStack.isNotEmpty()) {
            val entry = pathStack.removeAt(pathStack.size - 1)
            currentParentId = entry.first
            loadResources()
            return true
        }
        return false
    }

    fun handleUpload(uri: Uri) {
        Toast.makeText(requireContext(), "上传文件: ${getFileName(uri)}", Toast.LENGTH_SHORT).show()
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
