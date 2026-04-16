package com.example.clientforwebstorage.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.constraintlayout.widget.ConstraintSet
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.RegisterRequest
import com.example.clientforwebstorage.network.models.VerificationRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

class RegisterScreen(
    private val activity: Activity,
    private val onSwitchToLogin: () -> Unit
) {

    fun createView(): View {
        // 根布局：ConstraintLayout
        val rootLayout = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
            id = View.generateViewId()
        }

        // 顶部图标
        val logoImage = ImageView(activity).apply {
            id = View.generateViewId()
            setImageResource(R.mipmap.ic_launcher) // 请替换为你的应用图标
            layoutParams = LayoutParams(
                dpToPx(80),
                dpToPx(80)
            )
        }

        // 标题
        val titleText = TextView(activity).apply {
            id = View.generateViewId()
            text = "创建账号"
            textSize = 28f
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        // 副标题
        val subtitleText = TextView(activity).apply {
            id = View.generateViewId()
            text = "填写信息完成注册"
            textSize = 16f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
        }

        // 卡片容器
        val inputCard = CardView(activity).apply {
            id = View.generateViewId()
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(24), 0, dpToPx(24), 0)
            }
        }

        // 卡片内部布局
        val cardInnerLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(24))
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(24), 0, dpToPx(24), 0)
            }
        }

        // 邮箱输入框
        val emailInput = createInputField("邮箱", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)

        // 密码输入框
        val passwordInput = createInputField("密码", InputType.TYPE_TEXT_VARIATION_PASSWORD).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        // 用户名输入框
        val nicknameInput = createInputField("用户名", InputType.TYPE_CLASS_TEXT)

        // 验证码行（水平布局）
        val codeRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(24)
            }
        }

        // 验证码输入框（占2/3宽度）
        val codeInput = EditText(activity).apply {
            hint = "验证码"
            inputType = InputType.TYPE_CLASS_NUMBER
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            background = createUnderlineBackground()
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                2f
            ).apply {
                marginEnd = dpToPx(16)
            }
        }

        // 发送验证码按钮（占1/3宽度）
        val sendCodeButton = Button(activity).apply {
            text = "获取验证码"
            textSize = 14f
            setTextColor(Color.WHITE)
            setAllCaps(false)
            background = createRoundedBackground(Color.parseColor("#007AFF"), dpToPx(20).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                0,
                dpToPx(44),
                1f
            )
        }

        codeRow.addView(codeInput)
        codeRow.addView(sendCodeButton)

        // 将所有输入框添加到卡片内
        cardInnerLayout.addView(emailInput)
        cardInnerLayout.addView(passwordInput)
        cardInnerLayout.addView(nicknameInput)
        cardInnerLayout.addView(codeRow)  // 验证码行替代单独的验证码输入框
        inputCard.addView(cardInnerLayout)

        // 注册按钮
        val registerButton = Button(activity).apply {
            id = View.generateViewId()
            text = "注 册"
            textSize = 18f
            setTextColor(Color.WHITE)
            setAllCaps(false)
            background = createRoundedBackground(Color.parseColor("#007AFF"), dpToPx(25).toFloat())
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpToPx(50)
            ).apply {
                setMargins(dpToPx(24), 0, dpToPx(24), 0)
            }
        }

        // 登录提示布局
        val loginPromptLayout = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        val loginPrompt = TextView(activity).apply {
            text = "已有账号？"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
        }

        val loginButton = TextView(activity).apply {
            text = "去登录"
            textSize = 14f
            setTextColor(Color.parseColor("#007AFF"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(4), 0, 0, 0)
            setOnClickListener { onSwitchToLogin() }
        }

        loginPromptLayout.addView(loginPrompt)
        loginPromptLayout.addView(loginButton)

        // 将所有视图添加到根布局
        rootLayout.addView(logoImage)
        rootLayout.addView(titleText)
        rootLayout.addView(subtitleText)
        rootLayout.addView(inputCard)
        rootLayout.addView(registerButton)
        rootLayout.addView(loginPromptLayout)

        // 使用 ConstraintSet 建立约束
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        // 图标约束
        constraintSet.connect(logoImage.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dpToPx(60))
        constraintSet.connect(logoImage.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(logoImage.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 标题约束
        constraintSet.connect(titleText.id, ConstraintSet.TOP, logoImage.id, ConstraintSet.BOTTOM, dpToPx(16))
        constraintSet.connect(titleText.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(titleText.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 副标题约束
        constraintSet.connect(subtitleText.id, ConstraintSet.TOP, titleText.id, ConstraintSet.BOTTOM, dpToPx(8))
        constraintSet.connect(subtitleText.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(subtitleText.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 输入卡片约束
        constraintSet.connect(inputCard.id, ConstraintSet.TOP, subtitleText.id, ConstraintSet.BOTTOM, dpToPx(32))
        constraintSet.connect(inputCard.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(inputCard.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 注册按钮约束
        constraintSet.connect(registerButton.id, ConstraintSet.TOP, inputCard.id, ConstraintSet.BOTTOM, dpToPx(32))
        constraintSet.connect(registerButton.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(registerButton.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 登录提示布局约束（底部居中）
        constraintSet.connect(loginPromptLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(40))
        constraintSet.connect(loginPromptLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(loginPromptLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.applyTo(rootLayout)

        // 发送验证码点击事件
        sendCodeButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(activity, "请输入邮箱", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RetrofitClient.api.sendVerificationCode(VerificationRequest(email = email))
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(activity, "验证码已发送，请注意查收", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, "发送失败，请稍后重试", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Toast.makeText(activity, "网络连接失败", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // 注册按钮点击事件
        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val nickname = nicknameInput.text.toString().trim()
            val code = codeInput.text.toString().trim()

            when {
                email.isEmpty() -> Toast.makeText(activity, "请输入邮箱", Toast.LENGTH_SHORT).show()
                password.isEmpty() -> Toast.makeText(activity, "请输入密码", Toast.LENGTH_SHORT).show()
                nickname.isEmpty() -> Toast.makeText(activity, "请输入用户名", Toast.LENGTH_SHORT).show()
                code.isEmpty() -> Toast.makeText(activity, "请输入验证码", Toast.LENGTH_SHORT).show()
                else -> {
                    RetrofitClient.api.register(RegisterRequest(email, code, password, nickname))
                        .enqueue(object : Callback<ApiResponse> {
                            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                                if (response.isSuccessful) {
                                    Toast.makeText(activity, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                                    onSwitchToLogin()
                                } else {
                                    Toast.makeText(activity, "注册失败，请检查信息", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                Toast.makeText(activity, "网络错误，请稍后重试", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            }
        }

        return rootLayout
    }

    /**
     * 创建统一样式的输入框
     */
    private fun createInputField(hint: String, inputType: Int): EditText {
        return EditText(activity).apply {
            this.hint = hint
            this.inputType = inputType
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            background = createUnderlineBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(24)
            }
        }
    }

    /**
     * 创建底部线条背景
     */
    private fun createUnderlineBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(1, Color.parseColor("#E0E0E0"))
        }
    }

    /**
     * 创建圆角背景
     */
    private fun createRoundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = radius
        }
    }

    /**
     * dp 转 px 工具
     */
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            activity.resources.displayMetrics
        ).toInt()
    }
}