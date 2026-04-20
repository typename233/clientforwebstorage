package com.example.clientforwebstorage.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class MainScreen(
    private val activity: Activity,
    private val onLogout: () -> Unit,
    private val requestPickFiles: () -> Unit
) {

    private lateinit var contentFrame: FrameLayout
    private lateinit var tabResource: LinearLayout
    private lateinit var tabProfile: LinearLayout
    private lateinit var resourceScreen: ResourceScreen
    private lateinit var profileScreen: ProfileScreen

    private var currentTab = TAB_RESOURCE
    private var lastSwitchTime: Long = 0
    private val switchDebounceTime = 250L // 防抖时间（毫秒）

    fun createView(): View {
        val rootLayout = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
            id = View.generateViewId()
        }

        contentFrame = FrameLayout(activity).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )
        }

        val navBar = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            elevation = 8f
        }

        val divider = View(activity).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
        }

        val navContentLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        tabResource = createTabItem("📁", "资源", true)
        tabProfile = createTabItem("👤", "我的", false)

        tabResource.setOnClickListener { switchTab(TAB_RESOURCE) }
        tabProfile.setOnClickListener { switchTab(TAB_PROFILE) }

        navContentLayout.addView(tabResource)
        navContentLayout.addView(tabProfile)

        navBar.addView(divider)
        navBar.addView(navContentLayout)

        rootLayout.addView(contentFrame)
        rootLayout.addView(navBar)

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        constraintSet.connect(contentFrame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(contentFrame.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(contentFrame.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(contentFrame.id, ConstraintSet.BOTTOM, navBar.id, ConstraintSet.TOP)

        constraintSet.connect(navBar.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(navBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(navBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.applyTo(rootLayout)

        resourceScreen = ResourceScreen(activity, requestPickFiles)
        profileScreen = ProfileScreen(activity, onLogout)

        showTabContent(TAB_RESOURCE)

        return rootLayout
    }

    private fun switchTab(tab: Int) {
        if (tab == currentTab) return
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSwitchTime < switchDebounceTime) {
            return
        }
        
        lastSwitchTime = currentTime
        currentTab = tab
        updateTabStyle()
        showTabContent(tab)
    }

    private fun showTabContent(tab: Int) {
        contentFrame.removeAllViews()
        val view = when (tab) {
            TAB_RESOURCE -> resourceScreen.createView()
            TAB_PROFILE -> profileScreen.createView()
            else -> resourceScreen.createView()
        }
        contentFrame.addView(view)
    }

    private fun updateTabStyle() {
        val resSelected = currentTab == TAB_RESOURCE
        updateTabItemStyle(tabResource, resSelected)
        updateTabItemStyle(tabProfile, !resSelected)
    }

    private fun updateTabItemStyle(tab: LinearLayout, selected: Boolean) {
        val iconView = tab.getChildAt(0) as TextView
        val labelView = tab.getChildAt(1) as TextView
        iconView.alpha = if (selected) 1.0f else 0.4f
        labelView.setTextColor(if (selected) Color.parseColor("#007AFF") else Color.parseColor("#999999"))
    }

    private fun createTabItem(icon: String, label: String, selected: Boolean): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setPadding(0, dpToPx(4), 0, dpToPx(4))

            val iconView = TextView(activity).apply {
                text = icon
                textSize = 22f
                gravity = Gravity.CENTER
                alpha = if (selected) 1.0f else 0.4f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val labelView = TextView(activity).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (selected) Color.parseColor("#007AFF") else Color.parseColor("#999999"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(2)
                }
            }

            addView(iconView)
            addView(labelView)
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
        if (currentTab == TAB_RESOURCE) {
            resourceScreen.onFilesPicked(uris)
        }
    }

    fun handleBackPressed(): Boolean {
        return when (currentTab) {
            TAB_RESOURCE -> resourceScreen.handleBackPressed()
            TAB_PROFILE -> profileScreen.handleBackPressed()
            else -> false
        }
    }

    companion object {
        const val TAB_RESOURCE = 0
        const val TAB_PROFILE = 1
    }
}
