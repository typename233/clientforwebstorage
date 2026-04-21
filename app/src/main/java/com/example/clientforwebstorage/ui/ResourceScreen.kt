package com.example.clientforwebstorage.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
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
import com.example.clientforwebstorage.network.models.CreateFolderRequest
import com.example.clientforwebstorage.network.models.RenameRequest
import com.example.clientforwebstorage.network.models.Resource
import com.example.clientforwebstorage.network.models.ResourceListData
import com.example.clientforwebstorage.network.models.UploadCompleteRequest
import com.example.clientforwebstorage.network.models.UploadInitData
import com.example.clientforwebstorage.network.models.UploadInitRequest
import com.example.clientforwebstorage.network.models.CreateShareRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResourceScreen(
    private val activity: Activity,
    private val requestPickFiles: () -> Unit
) {

    private var currentParentId: String? = null
    private data class PathEntry(val parentId: String?, val name: String)
    private val pathStack = mutableListOf<PathEntry>()
    private var currentResources: List<Resource> = emptyList()
    private var currentCategory: String = "全部"
    private lateinit var contentContainer: LinearLayout
    private lateinit var emptyView: TextView
    private lateinit var fab: TextView
    private lateinit var overlay: View
    private lateinit var bottomSheet: LinearLayout

    private var isSelectionMode = false
    private val selectedIds = mutableSetOf<String>()
    private lateinit var selectionTopBar: LinearLayout
    private lateinit var selectionBottomBar: LinearLayout
    private lateinit var selectionCountText: TextView
    private lateinit var moreOverlay: View
    private lateinit var moreSheet: LinearLayout
    private lateinit var renameActionLayout: LinearLayout

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
            setBackgroundColor(Color.parseColor("#fafafa"))
            id = View.generateViewId()
        }

        selectionTopBar = createSelectionTopBar()
        val toolbar = createToolbar()
        val categoryTabs = createCategoryTabs()
        val storageProgress = createStorageProgress()
        val scrollView = createScrollView()
        emptyView = createEmptyView()
        selectionBottomBar = createSelectionBottomBar()
        fab = createFab()
        overlay = createOverlay()
        bottomSheet = createBottomSheet()
        moreOverlay = createMoreOverlay()
        moreSheet = createMoreSheet()

        rootLayout.addView(selectionTopBar)
        rootLayout.addView(toolbar)
        rootLayout.addView(categoryTabs)
        rootLayout.addView(storageProgress)
        rootLayout.addView(scrollView)
        rootLayout.addView(emptyView)
        rootLayout.addView(selectionBottomBar)
        rootLayout.addView(fab)
        rootLayout.addView(overlay)
        rootLayout.addView(bottomSheet)
        rootLayout.addView(moreOverlay)
        rootLayout.addView(moreSheet)

        setupConstraints(rootLayout)

        loadResources()

        return rootLayout
    }

    private fun createSelectionTopBar(): LinearLayout {
        return LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            elevation = dpToPx(2).toFloat()
            visibility = View.GONE
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }.apply {
            val selectAllBtn = TextView(activity).apply {
                text = "全选"
                textSize = 14f
                setTextColor(Color.parseColor("#1976D2"))
                setOnClickListener { selectAll() }
            }
            addView(selectAllBtn)

            selectionCountText = TextView(activity).apply {
                text = "已选中 0 个"
                textSize = 15f
                setTextColor(Color.parseColor("#333333"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(selectionCountText)

            val cancelBtn = TextView(activity).apply {
                text = "取消"
                textSize = 14f
                setTextColor(Color.parseColor("#1976D2"))
                setOnClickListener { exitSelectionMode() }
            }
            addView(cancelBtn)
        }
    }

    private fun createToolbar(): LinearLayout {
        return LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1976D2"))
            elevation = dpToPx(2).toFloat()
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )

            val titleText = TextView(activity).apply {
                text = "我的文件"
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(titleText)

            val searchIcon = TextView(activity).apply {
                text = "🔍"
                textSize = 20f
                setTextColor(Color.WHITE)
                setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
            }
            addView(searchIcon)

            val moreIcon = TextView(activity).apply {
                text = "⋮"
                textSize = 24f
                setTextColor(Color.WHITE)
                setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
            }
            addView(moreIcon)
        }
    }

    private fun createCategoryTabs(): ScrollView {
        val scrollView = ScrollView(activity)
        scrollView.id = View.generateViewId()
        scrollView.isHorizontalScrollBarEnabled = false
        scrollView.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(8)
        }

        val tabsContainer = LinearLayout(activity)
        tabsContainer.orientation = LinearLayout.HORIZONTAL
        tabsContainer.setPadding(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(4))

        val tabs = listOf("全部", "图片", "文档", "视频")
        tabs.forEachIndexed { index, tab ->
            val tabView = TextView(activity)
            tabView.text = tab
            tabView.textSize = 13f
            tabView.setTextColor(if (tab == currentCategory) Color.parseColor("#1976D2") else Color.parseColor("#999999"))
            tabView.setTypeface(null, if (tab == currentCategory) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            tabView.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(if (tab == currentCategory) Color.parseColor("#E3F2FD") else Color.TRANSPARENT)
            }
            tabView.setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(6))
            tabView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8)
            }
            tabView.setOnClickListener {
                selectCategory(tab, tabsContainer)
            }
            tabsContainer.addView(tabView)
        }

        scrollView.addView(tabsContainer)
        return scrollView
    }

    private fun selectCategory(category: String, tabsContainer: LinearLayout) {
        currentCategory = category
        
        // 更新标签样式
        for (i in 0 until tabsContainer.childCount) {
            val tabView = tabsContainer.getChildAt(i) as TextView
            val tabText = tabView.text.toString()
            val isSelected = tabText == category
            
            tabView.setTextColor(if (isSelected) Color.parseColor("#1976D2") else Color.parseColor("#999999"))
            tabView.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            tabView.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(if (isSelected) Color.parseColor("#E3F2FD") else Color.TRANSPARENT)
            }
        }
        
        // 重新显示资源
        displayResources(currentResources)
    }

    private fun createStorageProgress(): CardView {
        return CardView(activity).apply {
            id = View.generateViewId()
            radius = dpToPx(0).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
                marginStart = dpToPx(16)
                marginEnd = dpToPx(16)
            }

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))

                val headerRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL

                    val storageLabel = TextView(activity).apply {
                        text = "存储空间"
                        textSize = 12f
                        setTextColor(Color.parseColor("#666666"))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    addView(storageLabel)

                    val storageValue = TextView(activity).apply {
                        text = "0 / 100 GB"
                        textSize = 11f
                        setTextColor(Color.parseColor("#999999"))
                    }
                    addView(storageValue)
                }
                addView(headerRow)

                val progressBarBg = FrameLayout(activity).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dpToPx(4).toFloat()
                        setColor(Color.parseColor("#F0F0F0"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(6)
                    ).apply {
                        topMargin = dpToPx(6)
                    }

                    val progressBarFill = View(activity).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dpToPx(4).toFloat()
                            setColor(Color.parseColor("#1976D2"))
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            (dpToPx(200) * 0.05).toInt(),
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                    addView(progressBarFill)
                }
                addView(progressBarBg)
            }
            addView(innerLayout)
        }
    }

    private fun createScrollView(): ScrollView {
        return ScrollView(activity).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )

            contentContainer = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(80))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            addView(contentContainer)
        }
    }

    private fun createEmptyView(): TextView {
        return TextView(activity).apply {
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
    }

    private fun createSelectionBottomBar(): LinearLayout {
        return LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            elevation = dpToPx(4).toFloat()
            visibility = View.GONE
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )

            val shareBtn = TextView(activity).apply {
                text = "🔗 分享"
                textSize = 14f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1976D2"))
                gravity = Gravity.CENTER
                setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10))
                setOnClickListener { shareSelectedResources() }
            }
            addView(shareBtn)

            val deleteBtn = TextView(activity).apply {
                text = "🗑️ 删除"
                textSize = 14f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#FF3B30"))
                gravity = Gravity.CENTER
                setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dpToPx(12) }
                setOnClickListener { deleteSelectedResources() }
            }
            addView(deleteBtn)

            val moreBtn = TextView(activity).apply {
                text = "更多"
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                setBackgroundColor(Color.parseColor("#F0F0F0"))
                gravity = Gravity.CENTER
                setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dpToPx(12) }
                setOnClickListener { showMoreActions() }
            }
            addView(moreBtn)
        }
    }

    private fun createFab(): TextView {
        return TextView(activity).apply {
            id = View.generateViewId()
            text = "+"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1976D2"))
            }
            elevation = 6f
            layoutParams = ConstraintLayout.LayoutParams(dpToPx(56), dpToPx(56)).apply {
                marginEnd = dpToPx(16)
                bottomMargin = dpToPx(80)
            }
            setOnClickListener { showBottomSheet() }
        }
    }

    private fun createOverlay(): View {
        return View(activity).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.parseColor("#66000000"))
            visibility = View.GONE
            setOnClickListener { hideBottomSheet() }
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun createBottomSheet(): LinearLayout {
        return LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            elevation = 16f
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.35).toInt()
            )

            val sheetHandle = View(activity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#DDDDDD"))
                    cornerRadius = dpToPx(2).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(4)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dpToPx(12); bottomMargin = dpToPx(12)
                }
            }
            addView(sheetHandle)

            val sheetTitle = TextView(activity).apply {
                text = "操作"
                textSize = 18f
                setTextColor(Color.parseColor("#333333"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(20), 0, dpToPx(20), dpToPx(16))
            }
            addView(sheetTitle)

            addView(createSheetItem("📁", "新建文件夹") {
                hideBottomSheet(); showCreateFolderDialog()
            })
            addView(createSheetItem("📤", "从本地上传") {
                hideBottomSheet(); requestPickFiles()
            })
        }
    }

    private fun createMoreOverlay(): View {
        return View(activity).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.parseColor("#66000000"))
            visibility = View.GONE
            setOnClickListener { hideMoreActions() }
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun createMoreSheet(): LinearLayout {
        return LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            elevation = 16f
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.25).toInt()
            )

            val moreHandle = View(activity).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#DDDDDD"))
                    cornerRadius = dpToPx(2).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(4)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dpToPx(12); bottomMargin = dpToPx(12)
                }
            }
            addView(moreHandle)

            val moreTitle = TextView(activity).apply {
                text = "更多操作"
                textSize = 18f
                setTextColor(Color.parseColor("#333333"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(20), 0, dpToPx(20), dpToPx(16))
            }
            addView(moreTitle)

            renameActionLayout = createSheetItem("✏️", "重命名") {
                hideMoreActions(); showRenameDialog()
            }
            addView(renameActionLayout)
        }
    }

    private fun createSheetItem(icon: String, label: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))

            val iconText = TextView(activity).apply {
                text = icon; textSize = 22f
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            addView(iconText)

            val labelText = TextView(activity).apply {
                text = label; textSize = 16f
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(labelText)

            setOnClickListener { onClick() }
        }
    }

    private fun setupConstraints(rootLayout: ConstraintLayout) {
        val cs = ConstraintSet()
        cs.clone(rootLayout)

        cs.connect(selectionTopBar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(selectionTopBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(selectionTopBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        val toolbarId = rootLayout.getChildAt(1).id
        cs.connect(toolbarId, ConstraintSet.TOP, selectionTopBar.id, ConstraintSet.BOTTOM)
        cs.connect(toolbarId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(toolbarId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        val tabsId = rootLayout.getChildAt(2).id
        cs.connect(tabsId, ConstraintSet.TOP, toolbarId, ConstraintSet.BOTTOM)
        cs.connect(tabsId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(tabsId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        val storageId = rootLayout.getChildAt(3).id
        cs.connect(storageId, ConstraintSet.TOP, tabsId, ConstraintSet.BOTTOM)
        cs.connect(storageId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(storageId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        val scrollId = rootLayout.getChildAt(4).id
        cs.connect(scrollId, ConstraintSet.TOP, storageId, ConstraintSet.BOTTOM)
        cs.connect(scrollId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(scrollId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.connect(scrollId, ConstraintSet.BOTTOM, selectionBottomBar.id, ConstraintSet.TOP)

        val emptyId = rootLayout.getChildAt(5).id
        cs.connect(emptyId, ConstraintSet.TOP, storageId, ConstraintSet.BOTTOM)
        cs.connect(emptyId, ConstraintSet.BOTTOM, selectionBottomBar.id, ConstraintSet.TOP)

        cs.connect(selectionBottomBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(selectionBottomBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.connect(selectionBottomBar.id, ConstraintSet.BOTTOM, fab.id, ConstraintSet.TOP)

        cs.connect(fab.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.connect(fab.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        cs.connect(overlay.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(overlay.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(overlay.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(overlay.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        cs.connect(bottomSheet.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(bottomSheet.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(bottomSheet.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        cs.connect(moreOverlay.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(moreOverlay.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(moreOverlay.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(moreOverlay.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        cs.connect(moreSheet.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(moreSheet.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(moreSheet.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        cs.applyTo(rootLayout)
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
            val backItem = createFileItem(".. 返回上级", "folder", 0, null, "", true)
            backItem.setOnClickListener {
                if (pathStack.isNotEmpty()) {
                    val entry = pathStack.removeAt(pathStack.size - 1)
                    currentParentId = entry.parentId
                    loadResources()
                }
            }
            contentContainer.addView(backItem)
        }

        // 根据分类过滤资源
        val filteredResources = filterResourcesByCategory(resources)

        if (filteredResources.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE

        val folders = filteredResources.filter { it.type == "folder" }
        val files = filteredResources.filter { it.type != "folder" }

        for (folder in folders) {
            val item = createFileItem(folder.name, folder.type, folder.size, folder.extension, folder.updatedAt ?: "", selectedIds.contains(folder.id))
            item.setOnClickListener {
                if (isSelectionMode) toggleSelection(folder.id)
                else {
                    pathStack.add(PathEntry(currentParentId, folder.name))
                    currentParentId = folder.id
                    loadResources()
                }
            }
            item.setOnLongClickListener {
                if (!isSelectionMode) enterSelectionMode(folder.id)
                true
            }
            contentContainer.addView(item)
        }

        for (file in files) {
            val item = createFileItem(file.name, file.type, file.size, file.extension, file.updatedAt ?: "", selectedIds.contains(file.id))
            item.setOnClickListener {
                if (isSelectionMode) toggleSelection(file.id)
                else previewResource(file)
            }
            item.setOnLongClickListener {
                if (!isSelectionMode) enterSelectionMode(file.id)
                true
            }
            contentContainer.addView(item)
        }
    }

    private fun filterResourcesByCategory(resources: List<Resource>): List<Resource> {
        return resources.filter { resource ->
            if (resource.type == "folder") {
                // 文件夹始终显示
                true
            } else {
                when (currentCategory) {
                    "全部" -> true
                    "图片" -> isImageFile(resource.extension)
                    "文档" -> isDocumentFile(resource.extension) || !isImageFile(resource.extension) && !isVideoFile(resource.extension)
                    "视频" -> isVideoFile(resource.extension)
                    else -> true
                }
            }
        }
    }

    private fun isImageFile(extension: String?): Boolean {
        return extension?.lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }

    private fun isDocumentFile(extension: String?): Boolean {
        return extension?.lowercase() in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "log")
    }

    private fun isVideoFile(extension: String?): Boolean {
        return extension?.lowercase() in listOf("mp4", "avi", "mov", "mkv", "flv")
    }

    private fun createFileItem(name: String, type: String, size: Long, extension: String?, time: String, isSelected: Boolean): CardView {
        return CardView(activity).apply {
            radius = dpToPx(8).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(if (isSelected) Color.parseColor("#E3F2FD") else Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(2)
            }

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val iconText = TextView(activity).apply {
                text = if (type == "folder") "📁" else getFileIcon(extension)
                textSize = 24f
                layoutParams = LinearLayout.LayoutParams(dpToPx(36), LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val infoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dpToPx(12) }

                val nameText = TextView(activity).apply {
                    text = name; textSize = 14f
                    setTextColor(Color.parseColor("#333333"))
                    maxLines = 1
                }
                addView(nameText)

                val metaText = TextView(activity).apply {
                    text = if (type == "folder") "文件夹 · $time" else "${formatFileSize(size)} · $time"
                    textSize = 12f
                    setTextColor(Color.parseColor("#999999"))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(2) }
                }
                addView(metaText)
            }

            val rightIcon = TextView(activity).apply {
                text = when {
                    isSelectionMode -> if (isSelected) "✅" else "⭕"
                    type == "folder" -> "›"
                    else -> ""
                }
                textSize = 18f
                setTextColor(Color.parseColor("#CCCCCC"))
                layoutParams = LinearLayout.LayoutParams(dpToPx(32), LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
            }

            innerLayout.addView(iconText)
            innerLayout.addView(infoLayout)
            innerLayout.addView(rightIcon)
            addView(innerLayout)
        }
    }

    private fun getFileIcon(extension: String?): String {
        return when (extension?.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "🖼"
            "mp4", "avi", "mov", "mkv", "flv" -> "🎬"
            "mp3", "wav", "flac", "aac", "ogg" -> "🎵"
            "pdf" -> "📄"
            "doc", "docx" -> "📃"
            "xls", "xlsx" -> "📊"
            "ppt", "pptx" -> "📼"
            "txt", "log" -> "📝"
            "zip", "rar", "7z", "tar", "gz" -> "📦"
            "html", "css", "js", "json", "xml" -> "🌐"
            "java", "kt", "swift", "c", "cpp", "py" -> "💻"
            else -> "📄"
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "${size} B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun enterSelectionMode(resourceId: String) {
        isSelectionMode = true
        selectedIds.add(resourceId)
        updateSelectionUI()
        showSelectionMode()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds.clear()
        updateSelectionUI()
        hideSelectionMode()
        displayResources(currentResources)
    }

    private fun toggleSelection(resourceId: String) {
        if (selectedIds.contains(resourceId)) selectedIds.remove(resourceId)
        else selectedIds.add(resourceId)
        updateSelectionUI()
        displayResources(currentResources)
    }

    private fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(currentResources.map { it.id })
        updateSelectionUI()
        displayResources(currentResources)
    }

    private fun updateSelectionUI() {
        selectionCountText.text = "已选中 ${selectedIds.size} 个"
    }

    private fun showSelectionMode() {
        selectionTopBar.visibility = View.VISIBLE
        selectionBottomBar.visibility = View.VISIBLE
        fab.visibility = View.GONE
    }

    private fun hideSelectionMode() {
        selectionTopBar.visibility = View.GONE
        selectionBottomBar.visibility = View.GONE
        fab.visibility = View.VISIBLE
    }

    private fun showBottomSheet() {
        overlay.visibility = View.VISIBLE
        bottomSheet.visibility = View.VISIBLE
    }

    private fun hideBottomSheet() {
        overlay.visibility = View.GONE
        bottomSheet.visibility = View.GONE
    }

    fun showUploadFromAgent() {
        showBottomSheet()
    }

    private fun showMoreActions() {
        moreOverlay.visibility = View.VISIBLE
        moreSheet.visibility = View.VISIBLE
        renameActionLayout.alpha = if (selectedIds.size == 1) 1f else 0.5f
        renameActionLayout.isEnabled = selectedIds.size == 1
    }

    private fun hideMoreActions() {
        moreOverlay.visibility = View.GONE
        moreSheet.visibility = View.GONE
    }

    private fun previewResource(resource: Resource) {
        val intent = Intent(activity, PreviewActivity::class.java).apply {
            putExtra("resource_id", resource.id)
            putExtra("resource_name", resource.name)
        }
        activity.startActivity(intent)
    }

    private fun showCreateFolderDialog() {
        val editText = EditText(activity).apply {
            hint = "文件夹名称"; setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))
        }
        AlertDialog.Builder(activity)
            .setTitle("新建文件夹").setView(editText)
            .setPositiveButton("创建") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) createFolder(folderName)
            }.setNegativeButton("取消", null).show()
    }

    private fun createFolder(folderName: String) {
        val request = CreateFolderRequest(currentParentId, folderName)
        RetrofitClient.api.createFolder(request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            Toast.makeText(activity, "创建成功", Toast.LENGTH_SHORT).show()
                            loadResources()
                        } else {
                            Toast.makeText(activity, "创建失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
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

    private fun shareSelectedResources() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(activity, "请先选择要分享的文件", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
        }

        val needCodeCheck = android.widget.CheckBox(activity).apply { text = "需要提取码"; isChecked = true }
        val allowPreviewCheck = android.widget.CheckBox(activity).apply { text = "允许预览"; isChecked = true }
        val allowDownloadCheck = android.widget.CheckBox(activity).apply { text = "允许下载"; isChecked = true }

        dialogView.addView(needCodeCheck)
        dialogView.addView(allowPreviewCheck)
        dialogView.addView(allowDownloadCheck)

        AlertDialog.Builder(activity)
            .setTitle("创建分享").setView(dialogView)
            .setPositiveButton("创建") { _, _ ->
                createShare(selectedIds.toList(), needCodeCheck.isChecked, allowPreviewCheck.isChecked, allowDownloadCheck.isChecked)
            }.setNegativeButton("取消", null).show()
    }

    private fun createShare(resourceIds: List<String>, needCode: Boolean, allowPreview: Boolean, allowDownload: Boolean) {
        val request = CreateShareRequest(resourceIds, null, needCode, null, allowPreview, allowDownload, null)
        RetrofitClient.api.createShare(request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            Toast.makeText(activity, "分享创建成功", Toast.LENGTH_SHORT).show()
                            exitSelectionMode()
                        } else {
                            Toast.makeText(activity, "分享创建失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(activity, "分享创建失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun deleteSelectedResources() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(activity, "请先选择要删除的文件", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("确认删除")
            .setMessage("确定要删除这 ${selectedIds.size} 个文件吗？")
            .setPositiveButton("删除") { _, _ -> deleteResources(selectedIds.toList()) }
            .setNegativeButton("取消", null).show()
    }

    private fun deleteResources(resourceIds: List<String>) {
        var successCount = 0; var failCount = 0
        resourceIds.forEach { id ->
            RetrofitClient.api.deleteResource(id)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful && response.body()?.code == 0) successCount++
                        else failCount++
                        if (successCount + failCount == resourceIds.size) {
                            if (failCount == 0) Toast.makeText(activity, "删除成功", Toast.LENGTH_SHORT).show()
                            else Toast.makeText(activity, "成功删除 $successCount 个，失败 $failCount 个", Toast.LENGTH_SHORT).show()
                            exitSelectionMode(); loadResources()
                        }
                    }
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        failCount++
                        if (successCount + failCount == resourceIds.size) {
                            Toast.makeText(activity, "成功删除 $successCount 个，失败 $failCount 个", Toast.LENGTH_SHORT).show()
                            exitSelectionMode(); loadResources()
                        }
                    }
                })
        }
    }

    private fun showRenameDialog() {
        if (selectedIds.size != 1) {
            Toast.makeText(activity, "请选择一个文件进行重命名", Toast.LENGTH_SHORT).show()
            return
        }
        val resourceId = selectedIds.first()
        val resource = currentResources.find { it.id == resourceId } ?: return
        val editText = EditText(activity).apply {
            setText(resource.name); setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16)); selectAll()
        }
        AlertDialog.Builder(activity)
            .setTitle("重命名").setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != resource.name) renameResource(resourceId, newName)
            }.setNegativeButton("取消", null).show()
    }

    private fun renameResource(resourceId: String, newName: String) {
        RetrofitClient.api.renameResource(resourceId, RenameRequest(newName))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            Toast.makeText(activity, "重命名成功", Toast.LENGTH_SHORT).show()
                            exitSelectionMode(); loadResources()
                        } else {
                            Toast.makeText(activity, "重命名失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(activity, "重命名失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun handleBackPressed(): Boolean {
        if (isSelectionMode) { exitSelectionMode(); return true }
        if (currentParentId != null && pathStack.isNotEmpty()) {
            val entry = pathStack.removeAt(pathStack.size - 1)
            currentParentId = entry.parentId
            loadResources()
            return true
        }
        return false
    }

    fun handleUpload(uri: Uri) {
        pendingUploadUris = listOf(uri)
        showUploadConfirmDialog()
    }

    private fun showUploadConfirmDialog() {
        AlertDialog.Builder(activity)
            .setTitle("确认上传")
            .setMessage("确定要上传 ${pendingUploadUris.size} 个文件吗？")
            .setPositiveButton("上传") { _, _ -> startUpload() }
            .setNegativeButton("取消") { _, _ -> pendingUploadUris = emptyList() }.show()
    }

    private fun startUpload() {
        if (pendingUploadUris.isEmpty()) return
        uploadCancelled = false

        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
        }

        uploadStatusText = TextView(activity).apply { text = "准备上传..."; textSize = 14f }
        uploadProgressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }

        dialogView.addView(uploadStatusText)
        dialogView.addView(uploadProgressBar)

        uploadDialog = AlertDialog.Builder(activity)
            .setTitle("上传进度").setView(dialogView)
            .setPositiveButton("后台上传") { _, _ -> }
            .setNegativeButton("取消上传") { _, _ -> uploadCancelled = true; cancelUpload() }
            .setCancelable(false).show()

        uploadFilesSequentially(0)
    }

    private fun uploadFilesSequentially(index: Int) {
        if (index >= pendingUploadUris.size || uploadCancelled) {
            uploadDialog?.dismiss()
            Toast.makeText(activity, "上传完成", Toast.LENGTH_SHORT).show()
            pendingUploadUris = emptyList(); loadResources(); return
        }

        val uri = pendingUploadUris[index]
        val fileName = getFileName(uri)
        val fileSize = getFileSize(uri)
        uploadStatusText?.text = "上传 ${index + 1}/${pendingUploadUris.size}: $fileName"

        val initRequest = UploadInitRequest(parentId = currentParentId, filename = fileName, size = fileSize, sha256 = null, partSize = fileSize)

        RetrofitClient.api.initUpload(initRequest)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            val uploadInitData = parseUploadInitData(apiResponse.data)
                            if (uploadInitData != null) uploadFileToUrl(uri, uploadInitData, index)
                            else {
                                Toast.makeText(activity, "上传初始化失败", Toast.LENGTH_SHORT).show()
                                uploadFilesSequentially(index + 1)
                            }
                        } else {
                            Toast.makeText(activity, "上传初始化失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                            uploadFilesSequentially(index + 1)
                        }
                    } else {
                        Toast.makeText(activity, "上传初始化失败", Toast.LENGTH_SHORT).show()
                        uploadFilesSequentially(index + 1)
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    uploadFilesSequentially(index + 1)
                }
            })
    }

    private fun parseUploadInitData(data: Any?): UploadInitData? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            val type = object : TypeToken<UploadInitData>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) { null }
    }

    private fun uploadFileToUrl(uri: Uri, uploadInitData: UploadInitData, index: Int) {
        try {
            val inputStream = activity.contentResolver.openInputStream(uri) ?: return
            val bytes = inputStream.readBytes()
            inputStream.close()

            val uploadUrl = uploadInitData.uploadUrl
            if (uploadUrl == null) {
                Toast.makeText(activity, "上传初始化失败：无效的URL", Toast.LENGTH_SHORT).show()
                uploadFilesSequentially(index + 1); return
            }

            val requestBody = bytes.toRequestBody("application/octet-stream".toMediaType())

            val request = okhttp3.Request.Builder().url(uploadUrl).put(requestBody).build()

            RetrofitClient.okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "上传失败：${e.message}", Toast.LENGTH_SHORT).show()
                        uploadFilesSequentially(index + 1)
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    activity.runOnUiThread {
                        if (response.isSuccessful) {
                            uploadProgressBar?.progress = ((index + 1) * 100) / pendingUploadUris.size
                            completeUpload(uploadInitData.uploadId, index)
                        } else {
                            Toast.makeText(activity, "上传失败", Toast.LENGTH_SHORT).show()
                            uploadFilesSequentially(index + 1)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(activity, "上传失败：${e.message}", Toast.LENGTH_SHORT).show()
            uploadFilesSequentially(index + 1)
        }
    }

    private fun completeUpload(uploadId: String, index: Int) {
        RetrofitClient.api.completeUpload(uploadId, UploadCompleteRequest(parts = emptyList()))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) uploadFilesSequentially(index + 1)
                    else {
                        Toast.makeText(activity, "完成上传失败：${response.body()?.message}", Toast.LENGTH_SHORT).show()
                        uploadFilesSequentially(index + 1)
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    uploadFilesSequentially(index + 1)
                }
            })
    }

    private fun cancelUpload() {
        currentUploadId?.let { Toast.makeText(activity, "取消上传", Toast.LENGTH_SHORT).show() }
        currentUploadId = null
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) name = cursor.getString(nameIndex)
        }
        return name
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
        return size
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), activity.resources.displayMetrics).toInt()
    }
}
