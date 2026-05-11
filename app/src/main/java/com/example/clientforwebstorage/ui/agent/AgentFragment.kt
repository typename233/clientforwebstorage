package com.example.clientforwebstorage.ui.agent

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.clientforwebstorage.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText

class AgentFragment : Fragment() {

    private var onNavigateToFiles: (() -> Unit)? = null
    private var onNavigateToUpload: (() -> Unit)? = null

    fun setNavigationCallbacks(onFiles: () -> Unit, onUpload: () -> Unit) {
        onNavigateToFiles = onFiles
        onNavigateToUpload = onUpload
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_agent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val etInput = view.findViewById<TextInputEditText>(R.id.et_input)
        val sendButton = view.findViewById<ImageButton>(R.id.btn_send)
        val btnMore = view.findViewById<ImageButton>(R.id.btn_agent_more)

        btnMore.setOnClickListener { showMoreMenu(it) }

        sendButton.setOnClickListener {
            val text = etInput.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                Toast.makeText(requireContext(), "发送: $text", Toast.LENGTH_SHORT).show()
                etInput.text?.clear()
            }
        }
    }

    private fun showMoreMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext())
            .inflate(R.layout.popup_agent_menu, null)

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            setBackgroundDrawable(resources.getDrawable(android.R.color.white, null))
            isOutsideTouchable = true
            isFocusable = true
        }

        popupView.findViewById<View>(R.id.menu_clear_history)?.setOnClickListener {
            Toast.makeText(requireContext(), "清除对话记录", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupView.findViewById<View>(R.id.menu_settings)?.setOnClickListener {
            Toast.makeText(requireContext(), "设置", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        val offsetX = anchor.width - popupWidth
        popupWindow.showAsDropDown(anchor, offsetX, 0)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
