package com.example.netdisk.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.netdisk.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val switchNotifications = view.findViewById<Switch>(R.id.switch_notifications)
        val itemLogout = view.findViewById<View>(R.id.item_logout)

        switchNotifications.setOnCheckedChangeListener { _, checked ->
            val tip = if (checked) "已开启通知" else "已关闭通知"
            Toast.makeText(requireContext(), tip, Toast.LENGTH_SHORT).show()
        }

        itemLogout.setOnClickListener {
            Toast.makeText(requireContext(), "关于我们", Toast.LENGTH_SHORT).show()
        }
    }
}
