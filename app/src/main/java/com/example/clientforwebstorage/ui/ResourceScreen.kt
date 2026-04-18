package com.example.clientforwebstorage.ui

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.CompletedPart
import com.example.clientforwebstorage.network.models.CreateFolderRequest
import com.example.clientforwebstorage.network.models.Resource
import com.example.clientforwebstorage.network.models.ResourceListData
import com.example.clientforwebstorage.network.models.UploadCompleteRequest
import com.example.clientforwebstorage.network.models.UploadInitData
import com.example.clientforwebstorage.network.models.UploadInitRequest
import com.example.clientforwebstorage.network.models.UploadPartData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

class ResourceScreen(
    private val activity: Activity,
    private val requestPickFiles: () -> Unit
) {

    private var currentParentId: String? = null
    private data class PathEntry(val parentId: String?, val name: String)
    private val pathStack = mutableListOf<PathEntry>()
    private var currentResources: List<Resource> = emptyList()
    private lateinit var contentContainer: LinearLayout
    private lateinit var pathText: TextView
    private lateinit var emptyView: TextView
    private lateinit var fab: TextView
    private lateinit var overlay: View
    private lateinit var bottomSheet: LinearLayout

    private var uploadCancelled = false
    private var currentUploadId: String? = null
    private var uploadDialog: AlertDialog? = null
    private var uploadProgressBar: ProgressBar? = null
    private var uploadStatusText: TextView? = null
    private var pendingUploadUris: List<Uri> = emptyList()

    fun createView(): View {
        val rootLayout = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            id = View.generateViewId()
        }

        val toolbar = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(activity).apply {
            text = "我的资源"
            textSize = 20f
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        toolbar.addView(titleText)

        pathText = TextView(activity).apply {
            id = View.generateViewId()
            text = "根目录"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10))
            setBackgroundColor(Color.WHITE)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val scrollView = ScrollView(activity).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        contentContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        scrollView.addView(contentContainer)

        emptyView = TextView(activity).apply {
            id = View.generateViewId()
            text = "暂无资源"
            textSize = 16f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )
        }

        fab = TextView(activity).apply {
            id = View.generateViewId()
            text = "+"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#007AFF"))
            }
            elevation = 6f
            layoutParams = ConstraintLayout.LayoutParams(
                dpToPx(56),
                dpToPx(56)
            )
            setOnClickListener { showBottomSheet() }
        }

        overlay = View(activity).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.parseColor("#66000000"))
            visibility = View.GONE
            setOnClickListener { hideBottomSheet() }
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }

        bottomSheet = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            elevation = 16f
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            ).apply {
                matchConstraintPercentHeight = 0.5f
            }
        }

        val sheetHandle = View(activity).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#DDDDDD"))
                cornerRadius = dpToPx(2).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(4)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(12)
            }
        }

        val sheetTitle = TextView(activity).apply {
            text = "操作"
            textSize = 18f
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(20), 0, dpToPx(20), dpToPx(16))
        }

        bottomSheet.addView(sheetHandle)
        bottomSheet.addView(sheetTitle)

        val createFolderBtn = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))
            isClickable = true
            isFocusable = true

            val folderIcon = TextView(activity).apply {
                text = "📁"
                textSize = 22f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(16)
                }
            }

            val folderLabel = TextView(activity).apply {
                text = "新建文件夹"
                textSize = 16f
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            addView(folderIcon)
            addView(folderLabel)

            setOnClickListener {
                hideBottomSheet()
                showCreateFolderDialog()
            }
        }

        bottomSheet.addView(createFolderBtn)

        val uploadFileBtn = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))
            isClickable = true
            isFocusable = true

            val uploadIcon = TextView(activity).apply {
                text = "📤"
                textSize = 22f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(16)
                }
            }

            val uploadLabel = TextView(activity).apply {
                text = "从本地上传"
                textSize = 16f
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            addView(uploadIcon)
            addView(uploadLabel)

            setOnClickListener {
                hideBottomSheet()
                requestPickFiles()
            }
        }

        bottomSheet.addView(uploadFileBtn)

        rootLayout.addView(toolbar)
        rootLayout.addView(pathText)
        rootLayout.addView(scrollView)
        rootLayout.addView(emptyView)
        rootLayout.addView(overlay)
        rootLayout.addView(bottomSheet)
        rootLayout.addView(fab)

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        constraintSet.connect(toolbar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(toolbar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(toolbar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(pathText.id, ConstraintSet.TOP, toolbar.id, ConstraintSet.BOTTOM)
        constraintSet.connect(pathText.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(pathText.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(scrollView.id, ConstraintSet.TOP, pathText.id, ConstraintSet.BOTTOM)
        constraintSet.connect(scrollView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(scrollView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(scrollView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        constraintSet.connect(emptyView.id, ConstraintSet.TOP, pathText.id, ConstraintSet.BOTTOM)
        constraintSet.connect(emptyView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(emptyView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(emptyView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        constraintSet.connect(fab.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(fab.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.setMargin(fab.id, ConstraintSet.END, dpToPx(20))
        constraintSet.setMargin(fab.id, ConstraintSet.BOTTOM, dpToPx(20))

        constraintSet.connect(overlay.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(overlay.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(overlay.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(overlay.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        constraintSet.connect(bottomSheet.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(bottomSheet.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(bottomSheet.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(bottomSheet.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.constrainPercentHeight(bottomSheet.id, 0.5f)

        constraintSet.applyTo(rootLayout)

        loadResources()

        return rootLayout
    }

    private fun showBottomSheet() {
        overlay.visibility = View.VISIBLE
        bottomSheet.visibility = View.VISIBLE
        fab.visibility = View.GONE
    }

    private fun hideBottomSheet() {
        overlay.visibility = View.GONE
        bottomSheet.visibility = View.GONE
        fab.visibility = View.VISIBLE
    }

    private fun showCreateFolderDialog() {
        val defaultName = "新建文件夹"
        val finalName = resolveDuplicateName(defaultName)

        val input = EditText(activity).apply {
            text = android.text.Editable.Factory.getInstance().newEditable(finalName)
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            textSize = 16f
            setSelection(finalName.length)
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(8), dpToPx(24), 0)
            addView(input)
        }

        AlertDialog.Builder(activity)
            .setTitle("新建文件夹")
            .setView(container)
            .setPositiveButton("确认") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(activity, "文件夹名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                createFolder(name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun resolveDuplicateName(baseName: String): String {
        val folderNames = currentResources
            .filter { it.type == "folder" }
            .map { it.name }
            .toSet()

        if (baseName !in folderNames) return baseName

        var index = 1
        while ("$baseName($index)" in folderNames) {
            index++
        }
        return "$baseName($index)"
    }

    private fun createFolder(name: String) {
        val request = CreateFolderRequest(parentId = currentParentId, name = name)
        RetrofitClient.api.createFolder(request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            Toast.makeText(activity, "文件夹创建成功", Toast.LENGTH_SHORT).show()
                            loadResources()
                        } else {
                            Toast.makeText(activity, "创建失败: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(activity, "创建失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadResources() {
        RetrofitClient.api.getResources(currentParentId, null, 1, 50)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            val resourceListData = parseResourceListData(apiResponse.data)
                            if (resourceListData != null) {
                                displayResources(resourceListData.items)
                            } else {
                                displayResources(emptyList())
                            }
                        } else {
                            Toast.makeText(activity, "加载失败: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                            displayResources(emptyList())
                        }
                    } else {
                        Toast.makeText(activity, "加载失败", Toast.LENGTH_SHORT).show()
                        displayResources(emptyList())
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    displayResources(emptyList())
                }
            })
    }

    private fun parseResourceListData(data: Any?): ResourceListData? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            val type = object : TypeToken<ResourceListData>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun displayResources(resources: List<Resource>) {
        currentResources = resources
        contentContainer.removeAllViews()

        if (currentParentId != null) {
            val backItem = createResourceItem(
                name = ".. 返回上级",
                type = "folder",
                size = 0,
                extension = null
            )
            backItem.setOnClickListener {
                if (pathStack.isNotEmpty()) {
                    val entry = pathStack.removeAt(pathStack.size - 1)
                    currentParentId = entry.parentId
                    updatePathText()
                    loadResources()
                }
            }
            contentContainer.addView(backItem)
        }

        if (resources.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE

        val folders = resources.filter { it.type == "folder" }
        val files = resources.filter { it.type != "folder" }

        for (folder in folders) {
            val item = createResourceItem(
                name = folder.name,
                type = folder.type,
                size = folder.size,
                extension = folder.extension
            )
            item.setOnClickListener {
                pathStack.add(PathEntry(currentParentId, folder.name))
                currentParentId = folder.id
                updatePathText()
                loadResources()
            }
            contentContainer.addView(item)
        }

        for (file in files) {
            val item = createResourceItem(
                name = file.name,
                type = file.type,
                size = file.size,
                extension = file.extension
            )
            contentContainer.addView(item)
        }
    }

    private fun createResourceItem(name: String, type: String, size: Long, extension: String?): View {
        val card = CardView(activity).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4))
            }
        }

        val innerLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val iconText = TextView(activity).apply {
            text = if (type == "folder") "📁" else getFileIcon(extension)
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(14)
            }
        }

        val textLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameText = TextView(activity).apply {
            text = name
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val sizeText = TextView(activity).apply {
            text = if (type == "folder") "文件夹" else formatFileSize(size)
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        textLayout.addView(nameText)
        textLayout.addView(sizeText)

        val arrowText = TextView(activity).apply {
            text = if (type == "folder") "›" else ""
            textSize = 20f
            setTextColor(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        innerLayout.addView(iconText)
        innerLayout.addView(textLayout)
        innerLayout.addView(arrowText)
        card.addView(innerLayout)

        return card
    }

    private fun updatePathText() {
        if (pathStack.isEmpty()) {
            pathText.text = "根目录"
        } else {
            val names = pathStack.map { it.name }
            pathText.text = "根目录 > ${names.joinToString(" > ")}"
        }
    }

    private fun getFileIcon(extension: String?): String {
        return when (extension?.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "🖼️"
            "mp4", "avi", "mkv", "mov" -> "🎬"
            "mp3", "wav", "flac", "aac" -> "🎵"
            "pdf" -> "📄"
            "doc", "docx" -> "📝"
            "xls", "xlsx" -> "📊"
            "ppt", "pptx" -> "📑"
            "zip", "rar", "7z", "tar", "gz" -> "📦"
            "txt" -> "📃"
            else -> "📎"
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun createRoundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            activity.resources.displayMetrics
        ).toInt()
    }

    fun onFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        pendingUploadUris = uris
        showUploadConfirmDialog(uris.size)
    }

    private fun showUploadConfirmDialog(fileCount: Int) {
        AlertDialog.Builder(activity)
            .setTitle("确认上传")
            .setMessage("已选择 $fileCount 个文件，是否开始上传？")
            .setPositiveButton("确定") { _, _ ->
                startUpload(pendingUploadUris)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startUpload(uris: List<Uri>) {
        uploadCancelled = false
        showUploadProgressDialog(uris.size)
        uploadFileSequentially(uris, 0)
    }

    private fun showUploadProgressDialog(totalFiles: Int) {
        uploadProgressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        uploadStatusText = TextView(activity).apply {
            text = "正在准备上传..."
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(16), dpToPx(24), 0)
            addView(uploadProgressBar)
            addView(uploadStatusText)
        }

        uploadDialog = AlertDialog.Builder(activity)
            .setTitle("上传进度")
            .setView(container)
            .setPositiveButton("后台上传") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("取消上传") { dialog, _ ->
                uploadCancelled = true
                currentUploadId?.let { cancelCurrentUpload(it) }
                dialog.dismiss()
                uploadDialog = null
                Toast.makeText(activity, "上传已取消", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .create()
        uploadDialog?.show()
    }

    private fun updateUploadProgress(fileIndex: Int, totalFiles: Int, fileName: String, partIndex: Int, totalParts: Int) {
        val overallProgress = ((fileIndex - 1) * 100 + (partIndex * 100 / totalParts)) / totalFiles
        uploadProgressBar?.progress = overallProgress
        uploadStatusText?.text = "正在上传 $fileIndex/$totalFiles 文件\n文件: $fileName\n分片: $partIndex/$totalParts"
    }

    private fun uploadFileSequentially(uris: List<Uri>, index: Int) {
        if (index >= uris.size || uploadCancelled) {
            if (!uploadCancelled) {
                uploadDialog?.dismiss()
                uploadDialog = null
                Toast.makeText(activity, "上传完成", Toast.LENGTH_SHORT).show()
                loadResources()
            }
            return
        }

        val uri = uris[index]
        val fileInfo = getFileInfo(uri)

        if (fileInfo.size == 0L) {
            Toast.makeText(activity, "${fileInfo.name} 为空文件，已跳过", Toast.LENGTH_SHORT).show()
            uploadFileSequentially(uris, index + 1)
            return
        }

        val partSize = 5L * 1024 * 1024
        val totalParts = ((fileInfo.size + partSize - 1) / partSize).toInt()
        val totalFiles = uris.size
        val request = UploadInitRequest(currentParentId, fileInfo.name, fileInfo.size, null, partSize)

        RetrofitClient.api.initUpload(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.code == 0) {
                        val initData = parseData(apiResponse.data, UploadInitData::class.java)
                        if (initData != null) {
                            currentUploadId = initData.uploadId
                            updateUploadProgress(index + 1, totalFiles, fileInfo.name, 0, totalParts)
                            uploadParts(uri, initData.uploadId, fileInfo.size, partSize, 1, totalParts, index + 1, totalFiles, fileInfo.name, mutableListOf()) {
                                uploadFileSequentially(uris, index + 1)
                            }
                        } else {
                            Toast.makeText(activity, "上传 ${fileInfo.name} 初始化失败", Toast.LENGTH_SHORT).show()
                            uploadFileSequentially(uris, index + 1)
                        }
                    } else {
                        Toast.makeText(activity, "上传 ${fileInfo.name} 失败: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        uploadFileSequentially(uris, index + 1)
                    }
                } else {
                    Toast.makeText(activity, "上传 ${fileInfo.name} 失败", Toast.LENGTH_SHORT).show()
                    uploadFileSequentially(uris, index + 1)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                uploadFileSequentially(uris, index + 1)
            }
        })
    }

    private fun uploadParts(
        uri: Uri,
        uploadId: String,
        fileSize: Long,
        partSize: Long,
        currentPart: Int,
        totalParts: Int,
        fileIndex: Int,
        totalFiles: Int,
        fileName: String,
        completedParts: MutableList<CompletedPart>,
        onComplete: () -> Unit
    ) {
        if (uploadCancelled) {
            cancelCurrentUpload(uploadId)
            return
        }

        if (currentPart > totalParts) {
            completeUpload(uploadId, completedParts, onComplete)
            return
        }

        val offset = (currentPart - 1) * partSize
        val length = minOf(partSize, fileSize - offset)
        val bytes = readPartBytes(uri, offset, length)

        if (bytes == null) {
            Toast.makeText(activity, "读取文件失败", Toast.LENGTH_SHORT).show()
            cancelCurrentUpload(uploadId)
            return
        }

        val requestBody = bytes.toRequestBody("application/octet-stream".toMediaType())

        RetrofitClient.api.uploadPart(uploadId, currentPart, requestBody)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            val partData = parseData(apiResponse.data, UploadPartData::class.java)
                            val etag = partData?.etag ?: ""
                            completedParts.add(CompletedPart(currentPart, etag))
                            updateUploadProgress(fileIndex, totalFiles, fileName, currentPart, totalParts)
                            uploadParts(uri, uploadId, fileSize, partSize, currentPart + 1, totalParts, fileIndex, totalFiles, fileName, completedParts, onComplete)
                        } else {
                            Toast.makeText(activity, "上传分片失败", Toast.LENGTH_SHORT).show()
                            cancelCurrentUpload(uploadId)
                        }
                    } else {
                        Toast.makeText(activity, "上传分片失败", Toast.LENGTH_SHORT).show()
                        cancelCurrentUpload(uploadId)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    cancelCurrentUpload(uploadId)
                }
            })
    }

    private fun completeUpload(uploadId: String, parts: List<CompletedPart>, onComplete: () -> Unit) {
        val request = UploadCompleteRequest(parts)
        RetrofitClient.api.completeUpload(uploadId, request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            currentUploadId = null
                            onComplete()
                        } else {
                            Toast.makeText(activity, "完成上传失败: ${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                    } else {
                        Toast.makeText(activity, "完成上传失败", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            })
    }

    private fun cancelCurrentUpload(uploadId: String) {
        currentUploadId = null
        RetrofitClient.api.cancelUpload(uploadId).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {}
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun readPartBytes(uri: Uri, offset: Long, length: Long): ByteArray? {
        return try {
            val inputStream = activity.contentResolver.openInputStream(uri) ?: return null
            var skipped = 0L
            while (skipped < offset) {
                val s = inputStream.skip(offset - skipped)
                if (s <= 0L) break
                skipped += s
            }
            val bytes = ByteArray(length.toInt())
            var totalRead = 0
            while (totalRead < length.toInt()) {
                val read = inputStream.read(bytes, totalRead, length.toInt() - totalRead)
                if (read == -1) break
                totalRead += read
            }
            inputStream.close()
            if (totalRead < length.toInt()) null else bytes
        } catch (e: Exception) {
            null
        }
    }

    private data class FileInfo(val name: String, val size: Long)

    private fun getFileInfo(uri: Uri): FileInfo {
        var name = "unknown"
        var size = 0L
        val cursor = activity.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = it.getString(nameIndex)
                if (sizeIndex >= 0) size = it.getLong(sizeIndex)
            }
        }
        return FileInfo(name, size)
    }

    private fun <T> parseData(data: Any?, clazz: Class<T>): T? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }
}
