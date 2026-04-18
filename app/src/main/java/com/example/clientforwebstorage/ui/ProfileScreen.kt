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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.clientforwebstorage.network.TokenManager

class ProfileScreen(
    private val activity: Activity,
    private val onLogout: () -> Unit
) {

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
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        toolbar.addView(titleText)

        val centerLayout = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )
        }

        val avatarText = TextView(activity).apply {
            text = "👤"
            textSize = 64f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val placeholderText = TextView(activity).apply {
            text = "个人中心"
            textSize = 20f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16)
            }
        }

        val hintTextView = TextView(activity).apply {
            text = "功能开发中..."
            textSize = 14f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
        }

        val logoutButton = Button(activity).apply {
            text = "退出登录"
            textSize = 16f
            setTextColor(Color.WHITE)
            setAllCaps(false)
            background = createRoundedBackground(Color.parseColor("#FF3B30"), dpToPx(24).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(200),
                dpToPx(48)
            ).apply {
                topMargin = dpToPx(40)
            }
            setOnClickListener {
                TokenManager.clearTokens()
                onLogout()
            }
        }

        centerLayout.addView(avatarText)
        centerLayout.addView(placeholderText)
        centerLayout.addView(hintTextView)
        centerLayout.addView(logoutButton)

        rootLayout.addView(toolbar)
        rootLayout.addView(centerLayout)

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        constraintSet.connect(toolbar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(toolbar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(toolbar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(centerLayout.id, ConstraintSet.TOP, toolbar.id, ConstraintSet.BOTTOM)
        constraintSet.connect(centerLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(centerLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constraintSet.connect(centerLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

        constraintSet.applyTo(rootLayout)

        return rootLayout
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
