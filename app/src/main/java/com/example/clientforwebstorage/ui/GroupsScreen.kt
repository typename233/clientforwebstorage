package com.example.clientforwebstorage.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView

class GroupsScreen(private val activity: Activity) {

    fun createView(): View {
        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#fafafa"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = createToolbar()
        val scrollView = createScrollView()

        rootLayout.addView(toolbar)
        rootLayout.addView(scrollView)

        return rootLayout
    }

    private fun createToolbar(): View {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1976D2"))
            elevation = dpToPx(2).toFloat()
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56)
            )
        }.apply {
            val titleText = TextView(activity).apply {
                text = "群组空间"
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
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

    private fun createScrollView(): ScrollView {
        return ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )

            val contentLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val personalSpaceCard = createPersonalSpaceCard()
            contentLayout.addView(personalSpaceCard)

            val sectionTitle = TextView(activity).apply {
                text = "我的群组 (0)"
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                setPadding(0, dpToPx(16), 0, dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(20)
                }
            }
            contentLayout.addView(sectionTitle)

            val groupsList = createGroupsList()
            contentLayout.addView(groupsList)

            addView(contentLayout)
        }
    }

    private fun createPersonalSpaceCard(): CardView {
        return CardView(activity).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor("#1976D2"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))

                val headerLayout = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL

                    val folderIcon = TextView(activity).apply {
                        text = "📁"
                        textSize = 22f
                        layoutParams = LinearLayout.LayoutParams(
                            dpToPx(32),
                            dpToPx(32)
                        ).apply {
                            marginEnd = dpToPx(12)
                        }
                    }
                    addView(folderIcon)

                    val titleText = TextView(activity).apply {
                        text = "我的个人空间"
                        textSize = 18f
                        setTextColor(Color.WHITE)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    addView(titleText)
                }
                addView(headerLayout)

                val descText = TextView(activity).apply {
                    text = "暂无使用数据 · 总容量可用"
                    textSize = 13f
                    setTextColor(Color.parseColor("#B3D9FF"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(4)
                    }
                }
                addView(descText)
            }
            addView(innerLayout)
        }
    }

    private fun createGroupsList(): CardView {
        return CardView(activity).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(200)
            )

            val emptyLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val emptyIcon = TextView(activity).apply {
                    text = "👥"
                    textSize = 48f
                    alpha = 0.5f
                }
                addView(emptyIcon)

                val emptyText = TextView(activity).apply {
                    text = "暂无群组"
                    textSize = 14f
                    setTextColor(Color.parseColor("#999999"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(8)
                    }
                }
                addView(emptyText)

                val hintText = TextView(activity).apply {
                    text = "点击右下角按钮创建或加入群组"
                    textSize = 12f
                    setTextColor(Color.parseColor("#CCCCCC"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(4)
                    }
                }
                addView(hintText)
            }
            addView(emptyLayout)
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
