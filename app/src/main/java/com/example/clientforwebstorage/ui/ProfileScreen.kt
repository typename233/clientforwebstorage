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
import android.widget.HorizontalScrollView
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
import com.example.clientforwebstorage.network.models.UserActivity
import com.example.clientforwebstorage.network.models.UserActivityListData
import com.example.clientforwebstorage.network.models.Share
import com.example.clientforwebstorage.network.models.ShareListData
import com.example.clientforwebstorage.network.models.RevokeShareResponse
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
    private var activityOverlay: View? = null
    private var activityPanel: LinearLayout? = null
    private var activityContent: LinearLayout? = null
    private var activityEmptyView: TextView? = null
    private var activityList: List<UserActivity> = emptyList()
    private var shareOverlay: View? = null
    private var sharePanel: LinearLayout? = null
    private var shareContent: LinearLayout? = null
    private var shareEmptyView: TextView? = null
    private var shareList: List<Share> = emptyList()

    fun createView(): View {
        val rootLayout = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#FAFAFA"))
            id = View.generateViewId()
        }

        val toolbar = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1976D2"))
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            elevation = dpToPx(4).toFloat()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(activity).apply {
            text = "我的"
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        toolbar.addView(titleText)

        val scrollView = ScrollView(activity).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )
            setBackgroundColor(Color.parseColor("#FAFAFA"))
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

        val menuSections = createMenuSections()
        scrollContent.addView(menuSections)

        val logoutBtn = Button(activity).apply {
            text = "退出登录"
            textSize = 16f
            setTextColor(Color.WHITE)
            setAllCaps(false)
            background = createRoundedBackground(Color.parseColor("#FF3B30"), dpToPx(12).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            ).apply {
                topMargin = dpToPx(16)
            }
            setOnClickListener {
                AlertDialog.Builder(activity)
                    .setTitle("退出登录")
                    .setMessage("确定要退出登录吗？")
                    .setPositiveButton("确定") { _, _ ->
                        TokenManager.clearTokens()
                        onLogout()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
        scrollContent.addView(logoutBtn)

        val versionText = TextView(activity).apply {
            text = "NetDisk v1.0.0"
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(24)
            }
        }
        scrollContent.addView(versionText)

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
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val userInfoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
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
                    marginEnd = dpToPx(16)
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
                text = "user@example.com"
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(4)
                }
            }

            infoLayout.addView(nicknameText)
            infoLayout.addView(emailText)
            userInfoLayout.addView(avatarText)
            userInfoLayout.addView(infoLayout)

            val divider = View(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(1)
                ).apply {
                    topMargin = dpToPx(16)
                    bottomMargin = dpToPx(16)
                }
                setBackgroundColor(Color.parseColor("#E0E0E0"))
            }

            val storageLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val storageHeader = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val storageLabel = TextView(activity).apply {
                text = "存储空间使用情况"
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val storageValue = TextView(activity).apply {
                text = "15.2 GB / 100 GB"
                textSize = 14f
                setTextColor(Color.parseColor("#333333"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            storageHeader.addView(storageLabel)
            storageHeader.addView(storageValue)

            val progressBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(3).toFloat()
                setColor(Color.parseColor("#E0E0E0"))
            }

            val progressBar = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackground(progressBg)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(6)
                ).apply {
                    topMargin = dpToPx(8)
                }
            }

            val progressFill = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#007AFF"))
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(150),
                    dpToPx(6)
                )
            }

            progressBar.addView(progressFill)

            val storageFooter = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(8)
                }
            }

            val remainingText = TextView(activity).apply {
                text = "剩余 84.8 GB"
                textSize = 12f
                setTextColor(Color.parseColor("#999999"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val upgradeText = TextView(activity).apply {
                text = "升级容量"
                textSize = 12f
                setTextColor(Color.parseColor("#007AFF"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            storageFooter.addView(remainingText)
            storageFooter.addView(upgradeText)

            storageLayout.addView(storageHeader)
            storageLayout.addView(progressBar)
            storageLayout.addView(storageFooter)

            innerLayout.addView(userInfoLayout)
            innerLayout.addView(divider)
            innerLayout.addView(storageLayout)

            addView(innerLayout)
        }
    }

    private fun createMenuSections(): LinearLayout {
        val sectionsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val storageSection = createMenuSection("存储与分享", listOf(
            MenuItem("存储管理", "📦", action = null),
            MenuItem("我的分享", "🔗", action = { showShares() }),
            MenuItem("操作记录", "📋", action = { showUserActivities() }),
            MenuItem("收藏夹", "⭐", action = null)
        ))
        sectionsContainer.addView(storageSection)

        val settingsSection = createMenuSection("设置", listOf(
            MenuItem("消息通知", "🔔", action = null, hasSwitch = true),
            MenuItem("隐私与安全", "🔒", action = null),
            MenuItem("语言设置", "🌐", action = null)
        ))
        sectionsContainer.addView(settingsSection)

        val otherSection = createMenuSection("其他", listOf(
            MenuItem("关于我们", "ℹ️", action = null),
            MenuItem("账号设置", "⚙️", action = null)
        ))
        sectionsContainer.addView(otherSection)

        return sectionsContainer
    }

    data class MenuItem(
        val label: String,
        val icon: String,
        val action: (() -> Unit)?,
        val hasSwitch: Boolean = false
    )

    private fun createMenuSection(title: String, items: List<MenuItem>): CardView {
        return CardView(activity).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val titleText = TextView(activity).apply {
                text = title
                textSize = 12f
                setTextColor(Color.parseColor("#999999"))
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            innerLayout.addView(titleText)

            items.forEachIndexed { index, item ->
                val itemLayout = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    if (item.action != null) {
                        setOnClickListener { item.action.invoke() }
                    } else if (!item.hasSwitch) {
                        alpha = 0.5f
                    }
                }

                val iconText = TextView(activity).apply {
                    text = item.icon
                    textSize = 20f
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(40),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val labelText = TextView(activity).apply {
                    text = item.label
                    textSize = 16f
                    setTextColor(Color.parseColor("#333333"))
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val rightView = if (item.hasSwitch) {
                    val switchView = android.widget.Switch(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    switchView
                } else if (item.action != null || item.label == "存储管理") {
                    val arrowText = TextView(activity).apply {
                        text = "›"
                        textSize = 20f
                        setTextColor(Color.parseColor("#CCCCCC"))
                    }
                    arrowText
                } else {
                    val lockText = TextView(activity).apply {
                        text = "🔒"
                        textSize = 16f
                    }
                    lockText
                }

                itemLayout.addView(iconText)
                itemLayout.addView(labelText)
                itemLayout.addView(rightView)

                innerLayout.addView(itemLayout)

                if (index < items.size - 1) {
                    val divider = View(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(1)
                        )
                        setPadding(dpToPx(56), 0, 0, 0)
                        setBackgroundColor(Color.parseColor("#F0F0F0"))
                    }
                    innerLayout.addView(divider)
                }
            }

            addView(innerLayout)
        }
    }

    private fun showShares() {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val overlay = View(activity).apply {
            setBackgroundColor(Color.parseColor("#66000000"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { hideShares() }
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
            elevation = dpToPx(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val backBtn = TextView(activity).apply {
            text = "← 返回"
            textSize = 16f
            setTextColor(Color.parseColor("#007AFF"))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener { hideShares() }
        }

        val titleView = TextView(activity).apply {
            text = "我的分享"
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

        val placeholderView = TextView(activity).apply {
            text = "← 返回"
            textSize = 16f
            setTextColor(Color.parseColor("#007AFF"))
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(60),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.INVISIBLE
        }

        headerLayout.addView(backBtn)
        headerLayout.addView(titleView)
        headerLayout.addView(placeholderView)

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
            text = "暂无分享"
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

        panel.addView(headerLayout)
        panel.addView(scrollView)
        panel.addView(emptyView)

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

        shareOverlay = overlay
        sharePanel = panelWrapper
        shareContent = contentLayout
        shareEmptyView = emptyView

        loadShares()
    }

    private fun hideShares() {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        shareOverlay?.let { rootView.removeView(it) }
        sharePanel?.let { rootView.removeView(it) }
        shareOverlay = null
        sharePanel = null
        shareContent = null
        shareEmptyView = null
    }

    private fun loadShares() {
        android.util.Log.d("ProfileScreen", "loadShares called")
        RetrofitClient.api.getShareList(1, 100)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            val shareListData = parseShareListData(apiResponse.data)
                            if (shareListData != null) {
                                displayShares(shareListData.items)
                            } else {
                                displayShares(emptyList())
                            }
                        } else {
                            displayShares(emptyList())
                        }
                    } else {
                        displayShares(emptyList())
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    displayShares(emptyList())
                }
            })
    }

    private fun parseShareListData(data: Any?): ShareListData? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            val type = object : TypeToken<ShareListData>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun displayShares(shares: List<Share>) {
        shareList = shares
        val content = shareContent ?: return
        val emptyView = shareEmptyView ?: return
        content.removeAllViews()

        if (shares.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE

        for (share in shares) {
            val item = createShareItem(share)
            content.addView(item)
        }
    }

    private fun createShareItem(share: Share): View {
        val card = CardView(activity).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(1).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4))
            }
        }

        val innerLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val topLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val shareTitle = share.title ?: share.shareCode
        val shareCodeText = TextView(activity).apply {
            text = shareTitle
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val statusBadge = TextView(activity).apply {
            val isRevoked = share.status == "revoked" || share.revoked == true || share.alreadyRevoked == true
            val isExpired = isShareExpired(share)
            text = when {
                isRevoked -> "已撤销"
                isExpired -> "已过期"
                else -> "有效"
            }
            textSize = 12f
            setTextColor(Color.WHITE)
            background = createRoundedBackground(
                when {
                    isRevoked -> Color.parseColor("#999999")
                    isExpired -> Color.parseColor("#FF9800")
                    else -> Color.parseColor("#4CAF50")
                },
                dpToPx(12).toFloat()
            )
            setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(8)
            }
        }

        topLayout.addView(shareCodeText)
        topLayout.addView(statusBadge)

        val infoLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
        }

        val resourceCountText = TextView(activity).apply {
            val resourceCount = share.resourceCount ?: share.resourceIds?.size ?: 0
            text = "包含 $resourceCount 个资源"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val accessCountText = TextView(activity).apply {
            text = "访问次数：${share.currentAccessCount}${if (share.maxAccessCount != null) "/${share.maxAccessCount}" else ""}"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        val permissionText = TextView(activity).apply {
            val permissions = buildString {
                if (share.allowPreview) append("允许预览")
                if (share.allowDownload) append(if (isNotEmpty()) " · 允许下载" else "允许下载")
                if (share.needCode) append(if (isNotEmpty()) " · 需要验证码" else "需要验证码")
            }
            text = permissions
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        val createTimeText = TextView(activity).apply {
            text = "创建时间：${share.createdAt.replace("T", " ")}"
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        infoLayout.addView(resourceCountText)
        infoLayout.addView(accessCountText)
        infoLayout.addView(permissionText)
        infoLayout.addView(createTimeText)

        val actionLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
        }

        val isRevoked = share.status == "revoked" || share.revoked == true || share.alreadyRevoked == true
        if (!isRevoked) {
            val revokeBtn = TextView(activity).apply {
                text = "撤销分享"
                textSize = 14f
                setTextColor(Color.parseColor("#FF3B30"))
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                background = createRoundedBackground(Color.parseColor("#FFE5E5"), dpToPx(16).toFloat())
                setOnClickListener { revokeShare(share.id) }
            }
            actionLayout.addView(revokeBtn)
        }

        innerLayout.addView(topLayout)
        innerLayout.addView(infoLayout)
        innerLayout.addView(actionLayout)
        card.addView(innerLayout)

        return card
    }

    private fun isShareExpired(share: Share): Boolean {
        if (share.expiredAt == null) return false
        return try {
            val expiredTime = java.time.LocalDateTime.parse(share.expiredAt)
            val now = java.time.LocalDateTime.now()
            expiredTime.isBefore(now)
        } catch (e: Exception) {
            false
        }
    }

    private fun revokeShare(shareId: String) {
        AlertDialog.Builder(activity)
            .setTitle("确认撤销")
            .setMessage("确定要撤销此分享吗？撤销后将无法恢复。")
            .setPositiveButton("撤销") { _, _ ->
                RetrofitClient.api.revokeShare(shareId)
                    .enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (response.isSuccessful) {
                                val apiResponse = response.body()
                                if (apiResponse?.code == 0) {
                                    val revokeResponse = parseRevokeShareResponse(apiResponse.data)
                                    if (revokeResponse != null) {
                                        if (revokeResponse.alreadyRevoked) {
                                            Toast.makeText(activity, "该分享已经是撤销状态", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(activity, "撤销成功", Toast.LENGTH_SHORT).show()
                                            markShareAsRevoked(shareId)
                                        }
                                    } else {
                                        Toast.makeText(activity, "撤销成功", Toast.LENGTH_SHORT).show()
                                        markShareAsRevoked(shareId)
                                    }
                                } else {
                                    Toast.makeText(activity, "撤销失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(activity, "撤销失败", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Toast.makeText(activity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun parseRevokeShareResponse(data: Any?): RevokeShareResponse? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            val type = object : TypeToken<RevokeShareResponse>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun markShareAsRevoked(shareId: String) {
        val updatedShares = shareList.map { share ->
            if (share.id == shareId) {
                share.copy(status = "revoked", revoked = true, alreadyRevoked = true)
            } else {
                share
            }
        }
        shareList = updatedShares
        displayShares(updatedShares)
    }

    private fun showUserActivities() {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val overlay = View(activity).apply {
            setBackgroundColor(Color.parseColor("#66000000"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { hideUserActivities() }
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
            elevation = dpToPx(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val backBtn = TextView(activity).apply {
            text = "← 返回"
            textSize = 16f
            setTextColor(Color.parseColor("#007AFF"))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener { hideUserActivities() }
        }

        val titleView = TextView(activity).apply {
            text = "操作记录"
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

        val placeholderView = TextView(activity).apply {
            text = "← 返回"
            textSize = 16f
            setTextColor(Color.parseColor("#007AFF"))
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(60),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.INVISIBLE
        }

        headerLayout.addView(backBtn)
        headerLayout.addView(titleView)
        headerLayout.addView(placeholderView)

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
            text = "暂无操作记录"
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

        panel.addView(headerLayout)
        panel.addView(scrollView)
        panel.addView(emptyView)

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

        activityOverlay = overlay
        activityPanel = panelWrapper
        activityContent = contentLayout
        activityEmptyView = emptyView

        loadUserActivities()
    }

    private fun hideUserActivities() {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        activityOverlay?.let { rootView.removeView(it) }
        activityPanel?.let { rootView.removeView(it) }
        activityOverlay = null
        activityPanel = null
        activityContent = null
        activityEmptyView = null
    }

    private fun loadUserActivities() {
        RetrofitClient.api.getUserActivities(1, 50)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            val activityListData = parseActivityListData(apiResponse.data)
                            if (activityListData != null) {
                                displayActivities(activityListData.items)
                            } else {
                                displayActivities(emptyList())
                            }
                        } else {
                            displayActivities(emptyList())
                        }
                    } else {
                        displayActivities(emptyList())
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    displayActivities(emptyList())
                }
            })
    }

    private fun parseActivityListData(data: Any?): UserActivityListData? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            val type = object : TypeToken<UserActivityListData>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    private fun displayActivities(activities: List<UserActivity>) {
        activityList = activities
        val content = activityContent ?: return
        val emptyView = activityEmptyView ?: return
        content.removeAllViews()

        if (activities.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE

        for (userActivity in activities) {
            val item = createActivityItem(userActivity)
            content.addView(item)
        }
    }

    private fun createActivityItem(userActivity: UserActivity): View {
        val card = CardView(activity).apply {
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(1).toFloat()
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
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val iconText = TextView(activity).apply {
            text = when {
                userActivity.eventType.contains("上传", ignoreCase = true) -> "📤"
                userActivity.eventType.contains("下载", ignoreCase = true) -> "📥"
                userActivity.eventType.contains("删除", ignoreCase = true) -> "🗑️"
                userActivity.eventType.contains("重命名", ignoreCase = true) -> "✏️"
                userActivity.eventType.contains("移动", ignoreCase = true) -> "📁"
                userActivity.eventType.contains("复制", ignoreCase = true) -> "📋"
                userActivity.eventType.contains("分享", ignoreCase = true) -> "🔗"
                else -> "📄"
            }
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(40),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val infoLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(12)
            }
        }

        val actionText = TextView(activity).apply {
            text = userActivity.eventType
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val timeText = TextView(activity).apply {
            text = userActivity.createdAt.replace("T", " ").substringBefore(".")
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        infoLayout.addView(actionText)
        infoLayout.addView(timeText)
        innerLayout.addView(iconText)
        innerLayout.addView(infoLayout)
        card.addView(innerLayout)

        return card
    }

    fun handleBackPressed(): Boolean {
        if (shareOverlay != null && sharePanel != null) {
            hideShares()
            return true
        }
        if (activityOverlay != null && activityPanel != null) {
            hideUserActivities()
            return true
        }
        if (recycleBinOverlay != null && recycleBinPanel != null) {
            hideRecycleBin()
            return true
        }
        return false
    }

    fun navigateToRecycleBin() {
        showRecycleBin()
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
            elevation = dpToPx(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

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

        val placeholderView = TextView(activity).apply {
            text = "← 返回"
            textSize = 16f
            setTextColor(Color.parseColor("#007AFF"))
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(60),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.INVISIBLE
        }

        headerLayout.addView(backBtn)
        headerLayout.addView(titleView)
        headerLayout.addView(placeholderView)

        val selectionTopBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val cancelBtn = TextView(activity).apply {
            text = "取消"
            textSize = 14f
            setTextColor(Color.parseColor("#007AFF"))
            setOnClickListener { exitRecycleBinSelectionMode() }
        }

        val selectionCountText = TextView(activity).apply {
            text = "已选择 0 个"
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val selectAllBtn = TextView(activity).apply {
            text = "全选"
            textSize = 14f
            setTextColor(Color.parseColor("#007AFF"))
            setOnClickListener { selectAllRecycleBinItems() }
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
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            elevation = dpToPx(4).toFloat()
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val restoreBtn = TextView(activity).apply {
            text = "恢复"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#007AFF"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(24), dpToPx(10), dpToPx(24), dpToPx(10))
            setOnClickListener { restoreSelectedRecycleBinItems() }
        }

        val deleteBtn = TextView(activity).apply {
            text = "彻底删除"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#FF3B30"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(24), dpToPx(10), dpToPx(24), dpToPx(10))
            setOnClickListener { permanentlyDeleteSelectedRecycleBinItems() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(16)
            }
        }

        selectionBottomBar.addView(restoreBtn)
        selectionBottomBar.addView(deleteBtn)

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
        exitRecycleBinSelectionMode()
    }

    private fun loadRecycleBinResources() {
        RetrofitClient.api.getRecycleResources(null, null, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            val recycleListData = parseRecycleListData(apiResponse.data)
                            if (recycleListData != null) {
                                recycleBinResources = recycleListData.items
                                displayRecycleBinResources(recycleBinResources)
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

    private fun parseRecycleListData(data: Any?): RecycleResourceListData? {
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
        val content = recycleBinContent ?: return
        val emptyView = recycleBinEmptyView ?: return
        content.removeAllViews()

        if (resources.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE

        for (resource in resources) {
            val item = createRecycleBinItem(resource)
            content.addView(item)
        }
    }

    private fun createRecycleBinItem(resource: RecycleResource): View {
        val card = CardView(activity).apply {
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(1).toFloat()
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
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isSelected = recycleBinSelectedIds.contains(resource.id)
        }

        val checkbox = android.widget.CheckBox(activity).apply {
            isChecked = recycleBinSelectedIds.contains(resource.id)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(12)
            }
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    recycleBinSelectedIds.add(resource.id)
                } else {
                    recycleBinSelectedIds.remove(resource.id)
                }
                updateRecycleBinSelectionUI()
            }
        }

        val iconText = TextView(activity).apply {
            text = if (resource.type == "folder") "📁" else "📄"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(40),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val infoLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(12)
            }
        }

        val nameText = TextView(activity).apply {
            text = resource.name
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val sizeText = TextView(activity).apply {
            text = "删除时间：${resource.deletedAt.replace("T", " ").substringBefore(".")}"
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        infoLayout.addView(nameText)
        infoLayout.addView(sizeText)
        innerLayout.addView(checkbox)
        innerLayout.addView(iconText)
        innerLayout.addView(infoLayout)
        card.addView(innerLayout)

        return card
    }

    private fun updateRecycleBinSelectionUI() {
        val count = recycleBinSelectedIds.size
        recycleBinSelectionCountText?.text = "已选择 $count 个"

        if (count > 0) {
            recycleBinSelectionTopBar?.visibility = View.VISIBLE
            recycleBinSelectionBottomBar?.visibility = View.VISIBLE
            isRecycleBinSelectionMode = true
        } else {
            recycleBinSelectionTopBar?.visibility = View.GONE
            recycleBinSelectionBottomBar?.visibility = View.GONE
            isRecycleBinSelectionMode = false
        }
    }

    private fun exitRecycleBinSelectionMode() {
        recycleBinSelectedIds.clear()
        recycleBinSelectionTopBar?.visibility = View.GONE
        recycleBinSelectionBottomBar?.visibility = View.GONE
        isRecycleBinSelectionMode = false
        displayRecycleBinResources(recycleBinResources)
    }

    private fun selectAllRecycleBinItems() {
        recycleBinSelectedIds.clear()
        recycleBinSelectedIds.addAll(recycleBinResources.map { it.id })
        displayRecycleBinResources(recycleBinResources)
        updateRecycleBinSelectionUI()
    }

    private fun restoreSelectedRecycleBinItems() {
        if (recycleBinSelectedIds.isEmpty()) {
            Toast.makeText(activity, "请先选择要恢复的文件", Toast.LENGTH_SHORT).show()
            return
        }

        val idsToRestore = recycleBinSelectedIds.toList()
        var restoredCount = 0
        val totalCount = idsToRestore.size

        fun restoreNext(index: Int) {
            if (index >= totalCount) {
                Toast.makeText(activity, "恢复成功", Toast.LENGTH_SHORT).show()
                recycleBinSelectedIds.clear()
                loadRecycleBinResources()
                return
            }

            RetrofitClient.api.restoreResource(idsToRestore[index])
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        restoredCount++
                        restoreNext(index + 1)
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        restoreNext(index + 1)
                    }
                })
        }

        restoreNext(0)
    }

    private fun permanentlyDeleteSelectedRecycleBinItems() {
        if (recycleBinSelectedIds.isEmpty()) {
            Toast.makeText(activity, "请先选择要删除的文件", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(activity)
            .setTitle("确认彻底删除")
            .setMessage("彻底删除后无法恢复，确定要删除这 ${recycleBinSelectedIds.size} 个文件吗？")
            .setPositiveButton("彻底删除") { _, _ ->
                val idsToDelete = recycleBinSelectedIds.toList()
                val purgeRequest = PurgeRecycleRequest(purgeAll = false, resourceIds = idsToDelete)
                RetrofitClient.api.purgeRecycle(purgeRequest)
                    .enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (response.isSuccessful) {
                                val apiResponse = response.body()
                                if (apiResponse?.code == 0) {
                                    Toast.makeText(activity, "删除成功", Toast.LENGTH_SHORT).show()
                                    recycleBinSelectedIds.clear()
                                    loadRecycleBinResources()
                                } else {
                                    Toast.makeText(activity, "删除失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(activity, "删除失败", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Toast.makeText(activity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createRoundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
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
