package com.example.clientforwebstorage.ui.agent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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

        toolbar.setOnMenuItemClickListener {
            Toast.makeText(requireContext(), it.title, Toast.LENGTH_SHORT).show()
            true
        }

        sendButton.setOnClickListener {
            val text = etInput.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                Toast.makeText(requireContext(), "发送: $text", Toast.LENGTH_SHORT).show()
                etInput.text?.clear()
            }
        }
    }
}
