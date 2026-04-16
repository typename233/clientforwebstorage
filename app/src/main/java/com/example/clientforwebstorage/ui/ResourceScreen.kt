package com.example.clientforwebstorage.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.Resource
import com.example.clientforwebstorage.network.models.ResourceListData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

class ResourceScreen(
    private val activity: Activity,
    private val onLogout: () -> Unit
) {

    private var currentParentId: String? = null
    private val pathStack = mutableListOf<String?>()
    private lateinit var contentContainer: LinearLayout
    private lateinit var pathText: TextView
    private lateinit var emptyView: TextView

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
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val logoutButton = TextView(activity).apply {
            text = "退出"
            textSize = 14f
            setTextColor(Color.parseColor("#007AFF"))
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            background = createRoundedBackground(Color.parseColor("#E3F2FD"), dpToPx(12).toFloat())
            setOnClickListener {
                TokenManager.clearTokens()
                onLogout()
            }
        }

        toolbar.addView(titleText)
        toolbar.addView(logoutButton)

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

        rootLayout.addView(toolbar)
        rootLayout.addView(pathText)
        rootLayout.addView(scrollView)
        rootLayout.addView(emptyView)

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

        constraintSet.applyTo(rootLayout)

        loadResources()

        return rootLayout
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
        contentContainer.removeAllViews()

        if (resources.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE

        if (currentParentId != null) {
            val backItem = createResourceItem(
                name = ".. 返回上级",
                type = "folder",
                size = 0,
                extension = null
            )
            backItem.setOnClickListener {
                if (pathStack.isNotEmpty()) {
                    pathStack.removeAt(pathStack.size - 1)
                    currentParentId = if (pathStack.isNotEmpty()) pathStack.last() else null
                    updatePathText()
                    loadResources()
                }
            }
            contentContainer.addView(backItem)
        }

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
                pathStack.add(currentParentId)
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
            pathText.text = "根目录 > ..."
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
}
