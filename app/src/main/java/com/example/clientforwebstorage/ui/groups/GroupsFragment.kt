package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.CreateGroupRequest
import com.example.clientforwebstorage.network.models.GroupData
import com.example.clientforwebstorage.network.models.GroupListData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupsFragment : Fragment() {

    private lateinit var recycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var adapter: GroupListAdapter
    private var groups: List<GroupItem> = emptyList()

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
        recycler = view.findViewById(R.id.recycler_groups)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_group)

        toolbar.setOnMenuItemClickListener {
            Toast.makeText(requireContext(), it.title, Toast.LENGTH_SHORT).show()
            true
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = GroupListAdapter(groups) { item ->
            Toast.makeText(requireContext(), "点击群组: ${item.name}", Toast.LENGTH_SHORT).show()
        }
        recycler.adapter = adapter

        fab.setOnClickListener { showCreateGroupBottomSheet() }

        loadGroups(groupCount)
    }

    private fun showCreateGroupBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_create_group, null)

        sheetView.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            dialog.dismiss()
            val nameInput = sheetView.findViewById<EditText>(R.id.et_group_name)
            val name = nameInput.text?.toString()?.trim().orEmpty()
            if (name.isNotEmpty()) createGroup(name)
            else Toast.makeText(requireContext(), "请输入群组名称", Toast.LENGTH_SHORT).show()
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun createGroup(name: String) {
        RetrofitClient.api.createGroup(CreateGroupRequest(name))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "群组已创建: $name", Toast.LENGTH_SHORT).show()
                        loadGroups(view?.findViewById(R.id.tv_group_count) ?: return@onResponse)
                    } else {
                        Toast.makeText(requireContext(), "创建失败: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadGroups(countView: TextView) {
        RetrofitClient.api.getGroups(1, 50, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseGroupListData(response.body()?.data)
                        if (data != null) {
                            groups = data.items.map { group ->
                                GroupItem(group.id, group.name,
                                    role = group.role,
                                    memberCount = group.memberCount,
                                    storageUsed = formatSize(group.storageUsed),
                                    visibility = group.visibility)
                            }
                        } else {
                            groups = emptyList()
                        }
                        countView.text = "我的群组 (${groups.size})"
                        adapter = GroupListAdapter(groups) { item ->
                            Toast.makeText(requireContext(), "点击群组: ${item.name}", Toast.LENGTH_SHORT).show()
                        }
                        recycler.adapter = adapter
                    } else {
                        countView.text = "我的群组 (0)"
                        groups = emptyList()
                        adapter = GroupListAdapter(emptyList())
                        recycler.adapter = adapter
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun parseGroupListData(data: Any?): GroupListData? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<GroupListData>() {}.type)
        } catch (_: Exception) { null }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * requireContext().resources.displayMetrics.density).toInt()
}
