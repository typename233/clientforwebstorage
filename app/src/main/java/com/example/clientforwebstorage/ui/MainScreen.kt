package com.example.clientforwebstorage.ui

import android.app.Activity
import android.graphics.Color
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
    private lateinit var tabFiles: LinearLayout
    private lateinit var tabGroups: LinearLayout
    private lateinit var tabAgent: LinearLayout
    private lateinit var tabProfile: LinearLayout
    private lateinit var resourceScreen: ResourceScreen
    private lateinit var profileScreen: ProfileScreen
    private lateinit var agentPanel: AgentPanel
    private lateinit var groupsScreen: GroupsScreen

    private var currentTab = TAB_FILES
    private var lastSwitchTime: Long = 0
    private val switchDebounceTime = 250L

    fun createView(): View {
        val rootLayout = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#fafafa"))
            id = View.generateViewId()
        }

        contentFrame = FrameLayout(activity).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )
        }

        val bottomBar = createBottomNav()

        rootLayout.addView(contentFrame)
        rootLayout.addView(bottomBar)

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        constraintSet.connect(contentFrame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(contentFrame.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(contentFrame.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(contentFrame.id, ConstraintSet.BOTTOM, bottomBar.id, ConstraintSet.TOP)

        constraintSet.connect(bottomBar.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constraintSet.connect(bottomBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(bottomBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.applyTo(rootLayout)

        resourceScreen = ResourceScreen(activity, requestPickFiles)
        profileScreen = ProfileScreen(activity, onLogout)
        agentPanel = AgentPanel(
            activity = activity,
            onNavigateToFiles = { switchTab(TAB_FILES) },
            onNavigateToUpload = {
                switchTab(TAB_FILES)
                resourceScreen.showUploadFromAgent()
            },
            onNavigateToShares = { switchTab(TAB_PROFILE) },
            onNavigateToRecycle = {
                switchTab(TAB_PROFILE)
                profileScreen.navigateToRecycleBin()
            }
        )
        groupsScreen = GroupsScreen(activity)

        showTabContent(TAB_FILES)

        return rootLayout
    }

    private fun createBottomNav(): LinearLayout {
        val bottomBar = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            elevation = 8f
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val divider = View(activity).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
        }

        val navContent = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(0, dpToPx(6), 0, dpToPx(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56)
            )
        }

        tabFiles = createNavItem("📁", "文件", true)
        tabGroups = createNavItem("👥", "群组", false)
        tabAgent = createNavItem("🤖", "Agent", false)
        tabProfile = createNavItem("👤", "我的", false)

        tabFiles.setOnClickListener { switchTab(TAB_FILES) }
        tabGroups.setOnClickListener { switchTab(TAB_GROUPS) }
        tabAgent.setOnClickListener { switchTab(TAB_AGENT) }
        tabProfile.setOnClickListener { switchTab(TAB_PROFILE) }

        navContent.addView(tabFiles)
        navContent.addView(tabGroups)
        navContent.addView(tabAgent)
        navContent.addView(tabProfile)

        bottomBar.addView(divider)
        bottomBar.addView(navContent)

        return bottomBar
    }

    private fun createNavItem(icon: String, label: String, selected: Boolean): LinearLayout {
        return LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            val iconView = TextView(activity).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                alpha = if (selected) 1.0f else 0.5f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val labelView = TextView(activity).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (selected) Color.parseColor("#1976D2") else Color.parseColor("#999999"))
                setTypeface(null, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
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

    private fun updateTabStyle() {
        val tabs = listOf(
            Pair(tabFiles, TAB_FILES),
            Pair(tabGroups, TAB_GROUPS),
            Pair(tabAgent, TAB_AGENT),
            Pair(tabProfile, TAB_PROFILE)
        )

        for (entry in tabs) {
            val tab = entry.first
            val tabType = entry.second
            val isSelected = currentTab == tabType
            val iconView = tab.getChildAt(0) as TextView
            val labelView = tab.getChildAt(1) as TextView
            iconView.alpha = if (isSelected) 1.0f else 0.5f
            labelView.setTextColor(if (isSelected) Color.parseColor("#1976D2") else Color.parseColor("#999999"))
            labelView.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    private fun showTabContent(tab: Int) {
        contentFrame.removeAllViews()
        val view = when (tab) {
            TAB_FILES -> resourceScreen.createView()
            TAB_GROUPS -> groupsScreen.createView()
            TAB_AGENT -> agentPanel.createView()
            TAB_PROFILE -> profileScreen.createView()
            else -> resourceScreen.createView()
        }
        contentFrame.addView(view)
    }

    fun onFilesPicked(uris: List<Uri>) {
        if (currentTab == TAB_FILES) {
            uris.forEach { uri ->
                resourceScreen.handleUpload(uri)
            }
        }
    }

    fun handleBackPressed(): Boolean {
        return when (currentTab) {
            TAB_FILES -> resourceScreen.handleBackPressed()
            TAB_PROFILE -> profileScreen.handleBackPressed()
            else -> false
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            activity.resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val TAB_FILES = 0
        const val TAB_GROUPS = 1
        const val TAB_AGENT = 2
        const val TAB_PROFILE = 3
    }
}
