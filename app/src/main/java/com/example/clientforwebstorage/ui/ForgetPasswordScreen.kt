package com.example.clientforwebstorage.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
import com.example.clientforwebstorage.network.models.VerificationRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

class ForgetPasswordScreen(
    private val activity: Activity,
    private val onSwitchToLogin: () -> Unit
) {

    private var countdownTimer: CountDownTimer? = null

    fun createView(): View {
        val rootLayout = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#F3F4F6"))
            id = View.generateViewId()
        }

        val logoImage = ImageView(activity).apply {
            id = View.generateViewId()
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LayoutParams(
                dpToPx(80),
                dpToPx(80)
            )
        }

        val titleText = TextView(activity).apply {
            id = View.generateViewId()
            text = "忘记密码"
            textSize = 28f
            setTextColor(Color.parseColor("#111827"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val subtitleText = TextView(activity).apply {
            id = View.generateViewId()
            text = "输入邮箱重置您的密码"
            textSize = 16f
            setTextColor(Color.parseColor("#6B7280"))
            gravity = Gravity.CENTER
        }

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

        val emailInput = createInputField("邮箱", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)

        val codeRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(24)
            }
        }

        val codeInput = EditText(activity).apply {
            hint = "验证码"
            inputType = InputType.TYPE_CLASS_NUMBER
            textSize = 16f
            setTextColor(Color.parseColor("#111827"))
            setHintTextColor(Color.parseColor("#9CA3AF"))
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

        val sendCodeButton = Button(activity).apply {
            text = "获取验证码"
            textSize = 14f
            setTextColor(Color.WHITE)
            setAllCaps(false)
            background = createRoundedBackground(Color.parseColor("#1976D2"), dpToPx(20).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                0,
                dpToPx(44),
                1f
            )
        }

        codeRow.addView(codeInput)
        codeRow.addView(sendCodeButton)

        val newPasswordInput = EditText(activity).apply {
            hint = "新密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            textSize = 16f
            setTextColor(Color.parseColor("#111827"))
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            background = createUnderlineBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(24)
            }
        }

        val confirmPasswordInput = EditText(activity).apply {
            hint = "确认新密码"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            textSize = 16f
            setTextColor(Color.parseColor("#111827"))
            setHintTextColor(Color.parseColor("#9CA3AF"))
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            background = createUnderlineBackground()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        cardInnerLayout.addView(emailInput)
        cardInnerLayout.addView(codeRow)
        cardInnerLayout.addView(newPasswordInput)
        cardInnerLayout.addView(confirmPasswordInput)
        inputCard.addView(cardInnerLayout)

        val resetButton = Button(activity).apply {
            id = View.generateViewId()
            text = "重置密码"
            textSize = 18f
            setTextColor(Color.WHITE)
            setAllCaps(false)
            background = createRoundedBackground(Color.parseColor("#1976D2"), dpToPx(25).toFloat())
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                dpToPx(50)
            ).apply {
                setMargins(dpToPx(24), 0, dpToPx(24), 0)
            }
        }

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
            text = "想起密码了？"
            textSize = 14f
            setTextColor(Color.parseColor("#6B7280"))
        }

        val loginButton = TextView(activity).apply {
            text = "返回登录"
            textSize = 14f
            setTextColor(Color.parseColor("#1976D2"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(4), 0, 0, 0)
            setOnClickListener { onSwitchToLogin() }
        }

        loginPromptLayout.addView(loginPrompt)
        loginPromptLayout.addView(loginButton)

        rootLayout.addView(logoImage)
        rootLayout.addView(titleText)
        rootLayout.addView(subtitleText)
        rootLayout.addView(inputCard)
        rootLayout.addView(resetButton)
        rootLayout.addView(loginPromptLayout)

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootLayout)

        constraintSet.connect(logoImage.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, dpToPx(60))
        constraintSet.connect(logoImage.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(logoImage.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(titleText.id, ConstraintSet.TOP, logoImage.id, ConstraintSet.BOTTOM, dpToPx(16))
        constraintSet.connect(titleText.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(titleText.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(subtitleText.id, ConstraintSet.TOP, titleText.id, ConstraintSet.BOTTOM, dpToPx(8))
        constraintSet.connect(subtitleText.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(subtitleText.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(inputCard.id, ConstraintSet.TOP, subtitleText.id, ConstraintSet.BOTTOM, dpToPx(32))
        constraintSet.connect(inputCard.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(inputCard.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(resetButton.id, ConstraintSet.TOP, inputCard.id, ConstraintSet.BOTTOM, dpToPx(32))
        constraintSet.connect(resetButton.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(resetButton.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.connect(loginPromptLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(40))
        constraintSet.connect(loginPromptLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(loginPromptLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        constraintSet.applyTo(rootLayout)

        rootLayout.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.fade_in))

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
                            startCountdown(sendCodeButton)
                        } else {
                            Toast.makeText(activity, "发送失败，请稍后重试", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Toast.makeText(activity, "网络连接失败", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val code = codeInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            when {
                email.isEmpty() -> Toast.makeText(activity, "请输入邮箱", Toast.LENGTH_SHORT).show()
                code.isEmpty() -> Toast.makeText(activity, "请输入验证码", Toast.LENGTH_SHORT).show()
                newPassword.isEmpty() -> Toast.makeText(activity, "请输入新密码", Toast.LENGTH_SHORT).show()
                confirmPassword.isEmpty() -> Toast.makeText(activity, "请确认新密码", Toast.LENGTH_SHORT).show()
                newPassword != confirmPassword -> Toast.makeText(activity, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                newPassword.length < 6 -> Toast.makeText(activity, "密码长度至少6位", Toast.LENGTH_SHORT).show()
                else -> {
                    resetPassword(email, code, newPassword)
                }
            }
        }

        return rootLayout
    }

    private fun resetPassword(email: String, code: String, newPassword: String) {
        RetrofitClient.api.resetPassword(email, code, newPassword)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse?.code == 0) {
                            Toast.makeText(activity, "密码重置成功，请登录", Toast.LENGTH_SHORT).show()
                            countdownTimer?.cancel()
                            onSwitchToLogin()
                        } else {
                            Toast.makeText(activity, "重置失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(activity, "重置失败，请检查信息", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(activity, "网络连接失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun startCountdown(button: Button) {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                button.isEnabled = false
                button.text = "${millisUntilFinished / 1000}s 后重发"
                button.background = createRoundedBackground(Color.parseColor("#9CA3AF"), dpToPx(20).toFloat())
            }

            override fun onFinish() {
                button.isEnabled = true
                button.text = "获取验证码"
                button.background = createRoundedBackground(Color.parseColor("#1976D2"), dpToPx(20).toFloat())
            }
        }.start()
    }

    private fun createInputField(hint: String, inputType: Int): EditText {
        return EditText(activity).apply {
            this.hint = hint
            this.inputType = inputType
            textSize = 16f
            setTextColor(Color.parseColor("#111827"))
            setHintTextColor(Color.parseColor("#9CA3AF"))
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

    private fun createUnderlineBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            setStroke(1, Color.parseColor("#E5E7EB"))
        }
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
