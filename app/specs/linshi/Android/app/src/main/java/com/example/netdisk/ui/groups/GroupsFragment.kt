package com.example.netdisk.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.netdisk.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GroupsFragment : Fragment() {

    private val mockGroups = listOf(
        GroupItem("1", "设计团队", "owner", 8, "23.5 GB", "private"),
        GroupItem("2", "产品部门", "editor", 15, "67.2 GB", "private"),
        GroupItem("3", "公开资料库", "viewer", 45, "120.8 GB", "public")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val groupCount = view.findViewById<TextView>(R.id.tv_group_count)
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_groups)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_group)

        groupCount.text = "我的群组 (${mockGroups.size})"

        toolbar.setOnMenuItemClickListener {
            Toast.makeText(requireContext(), it.title, Toast.LENGTH_SHORT).show()
            true
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = GroupListAdapter(mockGroups)

        fab.setOnClickListener {
            Toast.makeText(requireContext(), "新建群组", Toast.LENGTH_SHORT).show()
        }
    }
}
