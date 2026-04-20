package com.example.clientforwebstorage.ui

import android.app.Activity
import android.app.AlertDialog
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
import com.example.clientforwebstorage.network.models.RecycleResource
import com.example.clientforwebstorage.network.models.RecycleResourceListData
import com.example.clientforwebstorage.network.models.PurgeRecycleRequest
import com.example.clientforwebstorage.network.models.PurgeRecycleData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileScreen(
    private val activity: Activity,
    private val onLogout: () -> Unit
) {

    private var recycleBinOverlay: View? = null
    private var recycleBinPanel: LinearLayout? = null
    private var recycleBinContent: LinearLayout? = null
    private var recycleBinEmptyView: TextView? = null
    private var recycleBinSelectionTopBar: LinearLayout? = null
    private var recycleBinSelectionCountText: TextView? = null
    private var recycleBinSelectionBottomBar: LinearLayout? = null
    private var recycleBinResources: List<RecycleResource> = emptyList()
    private var recycleBinSelectedIds = mutableSetOf<String>()
    private var isRecycleBinSelectionMode = false

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
            text = "我的"
            textSize = 20f
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        toolbar.addView(titleText)

        val scrollView = ScrollView(activity).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        val scrollContent = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val profileCard = createProfileCard()
        scrollContent.addView(profileCard)

        val funcCard = createFunctionCard()
        scrollContent.addView(funcCard)

        val logoutBtn = Button(activity).apply {
            text = "退出登录"
            textSize = 16f
            setTextColor(Color.WHITE)
            setAllCaps(false)
            background = createRoundedBackground(Color.parseColor("#FF3B30"), dpToPx(24).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            ).apply {
                topMargin = dpToPx(24)
            }
            setOnClickListener {
                TokenManager.clearTokens()
                onLogout()
            }
        }
        scrollContent.addView(logoutBtn)

        scrollView.addView(scrollContent)

        rootLayout.addView(toolbar)
        rootLayout.addView(scrollView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        constraintSet.connect(toolbar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(toolbar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(toolbar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(scrollView.id, ConstraintSet.TOP, toolbar.id, ConstraintSet.BOTTOM)
        constraintSet.connect(scrollView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(scrollView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(scrollView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        constraintSet.applyTo(rootLayout)

        return rootLayout
    }

    private fun createProfileCard(): CardView {
        return CardView(activity).apply {
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(24))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val avatarBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#007AFF"))
            }

            val avatarText = TextView(activity).apply {
                val nickname = TokenManager.getNickname() ?: "用户"
                text = nickname.firstOrNull()?.toString() ?: "U"
                textSize = 28f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = avatarBg
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(64),
                    dpToPx(64)
                ).apply {
                    marginEnd = dpToPx(20)
                }
            }

            val infoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val nicknameText = TextView(activity).apply {
                text = TokenManager.getNickname() ?: "未知用户"
                textSize = 20f
                setTextColor(Color.parseColor("#333333"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val emailText = TextView(activity).apply {
                text = "个人空间"
                textSize = 14f
                setTextColor(Color.parseColor("#999999"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(6)
                }
            }

            infoLayout.addView(nicknameText)
            infoLayout.addView(emailText)

            val arrowText = TextView(activity).apply {
                text = "›"
                textSize = 24f
                setTextColor(Color.parseColor("#CCCCCC"))
            }

            innerLayout.addView(avatarText)
            innerLayout.addView(infoLayout)
            innerLayout.addView(arrowText)
            addView(innerLayout)
        }
    }

    private fun createFunctionCard(): CardView {
        return CardView(activity).apply {
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16)
            }

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val recycleBinItem = createFunctionItem("🗑️", "回收站") {
                showRecycleBin()
            }

            innerLayout.addView(recycleBinItem)
            addView(innerLayout)
        }
    }

    private fun createFunctionItem(icon: String, label: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val iconView = TextView(activity).apply {
                text = icon
                textSize = 22f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(16)
                }
            }

            val labelView = TextView(activity).apply {
                text = label
                textSize = 16f
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val arrowView = TextView(activity).apply {
                text = "›"
                textSize = 20f
                setTextColor(Color.parseColor("#CCCCCC"))
            }

            addView(iconView)
            addView(labelView)
            addView(arrowView)

            setOnClickListener { onClick() }
        }
    }

    private fun showRecycleBin() {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val overlay = View(activity).apply {
            setBackgroundColor(Color.parseColor("#66000000"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { hideRecycleBin() }
        }

        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (rootView.height * 0.85).toInt()
            )
        }

        val headerLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val backBtn = TextView(activity).apply {
                text = "← 返回"
                textSize = 16f
                setTextColor(Color.parseColor("#007AFF"))
                setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                setOnClickListener { hideRecycleBin() }
            }

            val titleView = TextView(activity).apply {
                text = "回收站"
                textSize = 18f
                setTextColor(Color.parseColor("#333333"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            addView(backBtn)
            addView(titleView)
        }

        val selectionTopBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#007AFF"))
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val cancelBtn = TextView(activity).apply {
            text = "取消"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener { exitRecycleBinSelectionMode() }
        }

        val selectionCountText = TextView(activity).apply {
            text = "已选中 0 个文件"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val selectAllBtn = TextView(activity).apply {
            text = "全选"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener { selectAllRecycleBin() }
        }

        selectionTopBar.addView(cancelBtn)
        selectionTopBar.addView(selectionCountText)
        selectionTopBar.addView(selectAllBtn)

        val scrollView = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val contentLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        scrollView.addView(contentLayout)

        val emptyView = TextView(activity).apply {
            text = "回收站为空"
            textSize = 16f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(60), 0, 0)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val selectionBottomBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            visibility = View.GONE
            elevation = 8f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val restoreAction = createRecycleBinActionItem("♻️", "恢复")
        restoreAction.setOnClickListener { restoreSelectedResources() }
        val deleteAction = createRecycleBinActionItem("🗑️", "彻底删除")
        deleteAction.setOnClickListener { purgeSelectedResources() }
        val purgeAllAction = createRecycleBinActionItem("🧹", "清空回收站")
        purgeAllAction.setOnClickListener { showPurgeAllConfirmDialog() }

        selectionBottomBar.addView(restoreAction)
        selectionBottomBar.addView(deleteAction)
        selectionBottomBar.addView(purgeAllAction)

        panel.addView(headerLayout)
        panel.addView(selectionTopBar)
        panel.addView(scrollView)
        panel.addView(emptyView)
        panel.addView(selectionBottomBar)

        val panelWrapper = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        panelWrapper.addView(panel)

        rootView.addView(overlay)
        rootView.addView(panelWrapper)

        recycleBinOverlay = overlay
        recycleBinPanel = panelWrapper
        recycleBinContent = contentLayout
        recycleBinEmptyView = emptyView
        recycleBinSelectionTopBar = selectionTopBar
        recycleBinSelectionCountText = selectionCountText
        recycleBinSelectionBottomBar = selectionBottomBar

        isRecycleBinSelectionMode = false
        recycleBinSelectedIds.clear()

        loadRecycleBinResources()
    }

    private fun hideRecycleBin() {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        recycleBinOverlay?.let { rootView.removeView(it) }
        recycleBinPanel?.let { rootView.removeView(it) }
        recycleBinOverlay = null
        recycleBinPanel = null
        recycleBinContent = null
        recycleBinEmptyView = null
        recycleBinSelectionTopBar = null
        recycleBinSelectionCountText = null
        recycleBinSelectionBottomBar = null
        isRecycleBinSelectionMode = false
        recycleBinSelectedIds.clear()
    }

    private fun loadRecycleBinResources() {
        RetrofitClient.api.getRecycleResources(null, 1, 100)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            val recycleResourceListData = parseRecycleResourceListData(apiResponse.data)
                            if (recycleResourceListData != null) {
                                displayRecycleBinResources(recycleResourceListData.items)
                            } else {
                                displayRecycleBinResources(emptyList())
                            }
                        } else {
                            displayRecycleBinResources(emptyList())
                        }
                    } else {
                        displayRecycleBinResources(emptyList())
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    displayRecycleBinResources(emptyList())
                }
            })
    }

    private fun parseRecycleResourceListData(data: Any?): RecycleResourceListData? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            val type = object : TypeToken<RecycleResourceListData>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun displayRecycleBinResources(resources: List<RecycleResource>) {
        recycleBinResources = resources
        val content = recycleBinContent ?: return
        val emptyView = recycleBinEmptyView ?: return
        content.removeAllViews()

        if (resources.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE

        for (resource in resources) {
            val item = createRecycleBinItem(
                name = resource.name,
                type = resource.type,
                size = resource.size,
                extension = resource.extension,
                isSelected = recycleBinSelectedIds.contains(resource.id)
            )
            item.setOnClickListener {
                if (isRecycleBinSelectionMode) {
                    toggleRecycleBinSelection(resource.id)
                } else {
                    enterRecycleBinSelectionMode(resource.id)
                }
            }
            item.setOnLongClickListener {
                if (!isRecycleBinSelectionMode) {
                    enterRecycleBinSelectionMode(resource.id)
                }
                true
            }
            content.addView(item)
        }
    }

    private fun createRecycleBinItem(name: String, type: String, size: Long, extension: String?, isSelected: Boolean): View {
        val card = CardView(activity).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(if (isSelected) Color.parseColor("#E3F2FD") else Color.WHITE)
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

        val checkText = TextView(activity).apply {
            text = if (isSelected) "✅" else if (isRecycleBinSelectionMode) "⭕" else ""
            textSize = 20f
            setTextColor(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        innerLayout.addView(iconText)
        innerLayout.addView(textLayout)
        innerLayout.addView(checkText)
        card.addView(innerLayout)

        return card
    }

    private fun enterRecycleBinSelectionMode(resourceId: String) {
        isRecycleBinSelectionMode = true
        recycleBinSelectedIds.clear()
        recycleBinSelectedIds.add(resourceId)
        recycleBinSelectionTopBar?.visibility = View.VISIBLE
        recycleBinSelectionBottomBar?.visibility = View.VISIBLE
        updateRecycleBinSelectionCount()
        refreshRecycleBinViews()
    }

    private fun exitRecycleBinSelectionMode() {
        isRecycleBinSelectionMode = false
        recycleBinSelectedIds.clear()
        recycleBinSelectionTopBar?.visibility = View.GONE
        recycleBinSelectionBottomBar?.visibility = View.GONE
        refreshRecycleBinViews()
    }

    private fun toggleRecycleBinSelection(resourceId: String) {
        if (recycleBinSelectedIds.contains(resourceId)) {
            recycleBinSelectedIds.remove(resourceId)
            if (recycleBinSelectedIds.isEmpty()) {
                exitRecycleBinSelectionMode()
                return
            }
        } else {
            recycleBinSelectedIds.add(resourceId)
        }
        updateRecycleBinSelectionCount()
        refreshRecycleBinViews()
    }

    private fun selectAllRecycleBin() {
        for (resource in recycleBinResources) {
            recycleBinSelectedIds.add(resource.id)
        }
        updateRecycleBinSelectionCount()
        refreshRecycleBinViews()
    }

    private fun updateRecycleBinSelectionCount() {
        recycleBinSelectionCountText?.text = "已选中 ${recycleBinSelectedIds.size} 个文件"
    }

    private fun refreshRecycleBinViews() {
        displayRecycleBinResources(recycleBinResources)
    }

    private fun restoreSelectedResources() {
        val ids = recycleBinSelectedIds.toList()
        var successCount = 0
        var failCount = 0
        val total = ids.size

        for (id in ids) {
            RetrofitClient.api.restoreResource(id)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse?.code == 0) {
                                successCount++
                            } else {
                                failCount++
                            }
                        } else {
                            failCount++
                        }

                        if (successCount + failCount == total) {
                            if (failCount == 0) {
                                Toast.makeText(activity, "恢复成功", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(activity, "恢复完成，成功 $successCount 个，失败 $failCount 个", Toast.LENGTH_SHORT).show()
                            }
                            exitRecycleBinSelectionMode()
                            loadRecycleBinResources()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        failCount++
                        if (successCount + failCount == total) {
                            Toast.makeText(activity, "恢复完成，成功 $successCount 个，失败 $failCount 个", Toast.LENGTH_SHORT).show()
                            exitRecycleBinSelectionMode()
                            loadRecycleBinResources()
                        }
                    }
                })
        }
    }

    private fun purgeSelectedResources() {
        val ids = recycleBinSelectedIds.toList()
        if (ids.isEmpty()) {
            Toast.makeText(activity, "请选择要删除的文件", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(activity)
            .setTitle("确认删除")
            .setMessage("确定要彻底删除选中的 ${ids.size} 个文件吗？此操作不可恢复！")
            .setPositiveButton("删除") { _, _ ->
                val request = PurgeRecycleRequest(purgeAll = false, resourceIds = ids)
                RetrofitClient.api.purgeRecycle(request)
                    .enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (response.isSuccessful) {
                                val apiResponse = response.body()
                                if (apiResponse?.code == 0) {
                                    Toast.makeText(activity, "删除成功", Toast.LENGTH_SHORT).show()
                                    exitRecycleBinSelectionMode()
                                    loadRecycleBinResources()
                                } else {
                                    Toast.makeText(activity, "删除失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(activity, "删除失败", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPurgeAllConfirmDialog() {
        AlertDialog.Builder(activity)
            .setTitle("清空回收站")
            .setMessage("确定要清空整个回收站吗？此操作不可恢复！")
            .setPositiveButton("清空") { _, _ ->
                val request = PurgeRecycleRequest(purgeAll = true, resourceIds = null)
                RetrofitClient.api.purgeRecycle(request)
                    .enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (response.isSuccessful) {
                                val apiResponse = response.body()
                                if (apiResponse?.code == 0) {
                                    Toast.makeText(activity, "回收站已清空", Toast.LENGTH_SHORT).show()
                                    exitRecycleBinSelectionMode()
                                    loadRecycleBinResources()
                                } else {
                                    Toast.makeText(activity, "清空失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(activity, "清空失败", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createRecycleBinActionItem(icon: String, label: String): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            val iconView = TextView(activity).apply {
                text = icon
                textSize = 24f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val labelView = TextView(activity).apply {
                text = label
                textSize = 12f
                setTextColor(Color.parseColor("#666666"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(4)
                }
            }

            addView(iconView)
            addView(labelView)
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

    fun handleBackPressed(): Boolean {
        if (recycleBinOverlay != null && recycleBinPanel != null) {
            hideRecycleBin()
            return true
        }
        return false
    }
}
