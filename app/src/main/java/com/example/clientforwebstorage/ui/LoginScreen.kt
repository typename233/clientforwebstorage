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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.LoginData
import com.example.clientforwebstorage.network.models.LoginRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

class LoginScreen(
    private val activity: Activity,
    private val onSwitchToRegister: () -> Unit,
    private val onLoginSuccess: () -> Unit = {}
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

        // 顶部装饰图标（可用你的应用图标，这里用系统图标示例）
        val logoImage = ImageView(activity).apply {
            id = View.generateViewId()
            setImageResource(R.mipmap.ic_launcher) // 请替换为你的应用图标
            layoutParams = ConstraintLayout.LayoutParams(
                dpToPx(80),
                dpToPx(80)
            )
        }

        // 标题文字
        val titleText = TextView(activity).apply {
            id = View.generateViewId()
            text = "欢迎回来"
            textSize = 28f
            setTextColor(Color.parseColor("#333333"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        // 副标题
        val subtitleText = TextView(activity).apply {
            id = View.generateViewId()
            text = "登录您的账号以继续"
            textSize = 16f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
        }

        // 卡片容器（包含输入框）
        val inputCard = CardView(activity).apply {
            id = View.generateViewId()
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(24), 0, dpToPx(24), 0)
            }
        }

        // 卡片内部布局（垂直排列）
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

        cardInnerLayout.addView(emailInput)
        cardInnerLayout.addView(passwordInput)
        inputCard.addView(cardInnerLayout)

        // 登录按钮
        val loginButton = Button(activity).apply {
            id = View.generateViewId()
            text = "登 录"
            textSize = 18f
            setTextColor(Color.WHITE)
            setAllCaps(false)
            background = createRoundedBackground(Color.parseColor("#007AFF"), dpToPx(25).toFloat())
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                dpToPx(50)
            ).apply {
                setMargins(dpToPx(24), 0, dpToPx(24), 0)
            }
        }

        // 注册提示
        val registerPrompt = TextView(activity).apply {
            id = View.generateViewId()
            text = "还没有账号？"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
        }

        // 注册按钮（透明背景）
        val registerButton = TextView(activity).apply {
            id = View.generateViewId()
            text = "立即注册"
            textSize = 14f
            setTextColor(Color.parseColor("#007AFF"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(4), 0, 0, 0)
            setOnClickListener { onSwitchToRegister() }
        }

        // 将注册提示和按钮放在一个水平布局中
        val registerLayout = LinearLayout(activity).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            addView(registerPrompt)
            addView(registerButton)
        }

        // 将所有视图添加到根布局
        rootLayout.addView(logoImage)
        rootLayout.addView(titleText)
        rootLayout.addView(subtitleText)
        rootLayout.addView(inputCard)
        rootLayout.addView(loginButton)
        rootLayout.addView(registerLayout)

        // 使用 ConstraintSet 建立约束关系
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
        constraintSet.connect(inputCard.id, ConstraintSet.TOP, subtitleText.id, ConstraintSet.BOTTOM, dpToPx(40))
        constraintSet.connect(inputCard.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(inputCard.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 登录按钮约束
        constraintSet.connect(loginButton.id, ConstraintSet.TOP, inputCard.id, ConstraintSet.BOTTOM, dpToPx(32))
        constraintSet.connect(loginButton.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(loginButton.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        // 注册布局约束（居中底部）
        constraintSet.connect(registerLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(40))
        constraintSet.connect(registerLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(registerLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.applyTo(rootLayout)

        // 登录按钮点击事件
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(activity, "请填写邮箱和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RetrofitClient.api.login(LoginRequest(email, password))
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse?.code == 0) {
                                val loginData = parseLoginData(apiResponse.data)
                                if (loginData != null) {
                                    TokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)
                                    Toast.makeText(activity, "登录成功", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(activity, "登录响应异常", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(activity, "登录失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(activity, "登录失败，请检查账号密码", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Toast.makeText(activity, "网络连接失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    }
                })
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

    private fun parseLoginData(data: Any?): LoginData? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            gson.fromJson(json, LoginData::class.java)
        } catch (e: Exception) {
            null
        }
    }
}