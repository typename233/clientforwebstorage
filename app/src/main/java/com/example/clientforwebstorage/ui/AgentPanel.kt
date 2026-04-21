package com.example.clientforwebstorage.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class AgentPanel(
    private val activity: Activity,
    private val onNavigateToFiles: () -> Unit = {},
    private val onNavigateToUpload: () -> Unit = {},
    private val onNavigateToShares: () -> Unit = {},
    private val onNavigateToRecycle: () -> Unit = {}
) {

    fun createView(): View {
        val rootLayout = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#fafafa"))
            id = View.generateViewId()
        }

        val toolbar = createToolbar()
        val scrollView = createScrollView()
        val bottomInput = createBottomInput()

        rootLayout.addView(toolbar)
        rootLayout.addView(scrollView)
        rootLayout.addView(bottomInput)

        val cs = ConstraintSet()
        cs.clone(rootLayout)

        val toolbarId = toolbar.id
        cs.connect(toolbarId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        cs.connect(toolbarId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(toolbarId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        val scrollId = scrollView.id
        cs.connect(scrollId, ConstraintSet.TOP, toolbarId, ConstraintSet.BOTTOM)
        cs.connect(scrollId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(scrollId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        cs.connect(scrollId, ConstraintSet.BOTTOM, bottomInput.id, ConstraintSet.TOP)

        val inputId = bottomInput.id
        cs.connect(inputId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        cs.connect(inputId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        cs.connect(inputId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        cs.applyTo(rootLayout)

        return rootLayout
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

            val avatarBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            val avatarText = TextView(activity).apply {
                text = "🤖"
                textSize = 22f
                gravity = Gravity.CENTER
                background = avatarBg
                layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply { marginEnd = dpToPx(12) }
            }
            addView(avatarText)

            val titleText = TextView(activity).apply {
                text = "智能助手"
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(titleText)

            val moreIcon = TextView(activity).apply {
                text = "⋮"
                textSize = 24f
                setTextColor(Color.WHITE)
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            }
            addView(moreIcon)
        }
    }

    private fun createScrollView(): ScrollView {
        return ScrollView(activity).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            )

            val contentLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            contentLayout.addView(createWelcomeCard())
            contentLayout.addView(createQuickActionsGrid())
            contentLayout.addView(createConversationHistory())

            addView(contentLayout)
        }
    }

    private fun createWelcomeCard(): CardView {
        return CardView(activity).apply {
            radius = dpToPx(16).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor("#1976D2"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(24))

                val welcomeText = TextView(activity).apply {
                    text = "👋 你好，我是智能助手"
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                addView(welcomeText)

                val descText = TextView(activity).apply {
                    text = "我可以帮助你管理文件、搜索内容、回答问题等"
                    textSize = 14f
                    setTextColor(Color.parseColor("#B3D9FF"))
                    alpha = 0.9f
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(8) }
                }
                addView(descText)
            }
            addView(innerLayout)
        }
    }

    private fun createQuickActionsGrid(): LinearLayout {
        val gridContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(16) }
        }

        val titleText = TextView(activity).apply {
            text = "快捷操作"
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(12) }
        }
        gridContainer.addView(titleText)

        val gridLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL

            val leftColumn = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dpToPx(8) }
            }
            val rightColumn = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dpToPx(8) }
            }

            leftColumn.addActionCard("🔍", "搜索文件", "快速查找文件", Color.parseColor("#4CAF50"), onNavigateToFiles)
            leftColumn.addActionCard("📤", "上传文件", "从本地上传", Color.parseColor("#FF9800"), onNavigateToUpload)
            rightColumn.addActionCard("🔗", "分享文件", "创建分享链接", Color.parseColor("#2196F3"), onNavigateToShares)
            rightColumn.addActionCard("🗑️", "回收站", "管理已删除文件", Color.parseColor("#9C27B0"), onNavigateToRecycle)

            addView(leftColumn)
            addView(rightColumn)
        }
        gridContainer.addView(gridLayout)

        return gridContainer
    }

    private fun LinearLayout.addActionCard(icon: String, title: String, desc: String, color: Int, onClick: () -> Unit) {
        val card = CardView(context).apply {
            radius = dpToPx(12).toFloat()
            cardElevation = 2f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(8) }

            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))

                val iconBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(dpToPx(2), Color.parseColor("#1976D2"))
                    setColor(Color.WHITE)
                }
                val iconTv = TextView(context).apply {
                    text = icon; textSize = 18f; gravity = Gravity.CENTER; background = iconBg
                    layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
                }
                addView(iconTv)

                val textLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dpToPx(12) }
                }

                val titleTv = TextView(context).apply {
                    text = title; textSize = 14f; setTextColor(Color.parseColor("#333333"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                textLayout.addView(titleTv)

                val descTv = TextView(context).apply {
                    text = desc; textSize = 12f; setTextColor(Color.parseColor("#999999"))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(2) }
                }
                textLayout.addView(descTv)
                addView(textLayout)
            }
            addView(inner)
        }.also {
            it.setOnClickListener { onClick() }
        }
        addView(card)
    }

    private fun createConversationHistory(): LinearLayout {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(20) }
        }

        val titleLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(12) }
        }

        val titleText = TextView(activity).apply {
            text = "对话记录"
            textSize = 12f
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        titleLayout.addView(titleText)
        container.addView(titleLayout)

        val divider = View(activity).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { bottomMargin = dpToPx(12) }
        }
        container.addView(divider)

        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val promptCard = CardView(activity).apply {
            radius = dpToPx(8).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(8) }

            val innerLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            }

            val promptText = TextView(activity).apply {
                text = "👋 你好！有什么可以帮助你的吗？"
                textSize = 14f
                setTextColor(Color.parseColor("#333333"))
            }
            innerLayout.addView(promptText)

            val timeText = TextView(activity).apply {
                text = currentTime
                textSize = 12f
                setTextColor(Color.parseColor("#999999"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dpToPx(4) }
            }
            innerLayout.addView(timeText)
            addView(innerLayout)
        }
        container.addView(promptCard)

        return container
    }

    private fun createBottomInput(): LinearLayout {
        return LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            elevation = dpToPx(4).toFloat()
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )

            val editText = EditText(activity).apply {
                hint = "输入消息..."
                textSize = 14f
                setTextColor(Color.parseColor("#333333"))
                setHintTextColor(Color.parseColor("#999999"))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(20).toFloat()
                    setColor(Color.parseColor("#F5F5F5"))
                }
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                maxLines = 1
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f)
            }
            addView(editText)

            val sendBtn = TextView(activity).apply {
                text = "发送"
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(16).toFloat()
                    setColor(Color.parseColor("#1976D2"))
                }
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dpToPx(12) }
            }
            addView(sendBtn)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), activity.resources.displayMetrics).toInt()
    }
}
