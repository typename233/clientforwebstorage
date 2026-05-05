package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.ApiService
import com.example.clientforwebstorage.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupDetailFragment : Fragment() {

    private var groupId: String? = null
    private var groupName: String = ""
    
    private lateinit var apiService: ApiService

    companion object {
        private const val ARG_GROUP_ID = "group_id"
        private const val ARG_GROUP_NAME = "group_name"

        fun newInstance(groupId: String, groupName: String): GroupDetailFragment {
            return GroupDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                    putString(ARG_GROUP_NAME, groupName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID)
            groupName = it.getString(ARG_GROUP_NAME, "群组详情")
        }
        apiService = RetrofitClient.api
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_group_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        loadGroupInfo()
        setupActionButtons(view)
    }

    private fun initViews(view: View) {
        
    }

    private fun setupToolbar() {
        view?.findViewById<MaterialToolbar>(R.id.toolbar_detail)?.apply {
            title = groupName
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadGroupInfo() {
        groupId?.let { id ->
            apiService.getGroupDetail(id).enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                override fun onResponse(
                    call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                    response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        if (apiResponse.code == 0) {
                            updateUIWithGroupData(apiResponse.data)
                        } else {
                            showError("加载群组详情失败: ${apiResponse.message}")
                        }
                    } else {
                        showError("网络请求失败")
                    }
                }

                override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                    showError("网络错误: ${t.message}")
                }
            })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateUIWithGroupData(data: Any?) {
        view?.let { v ->
            try {
                val groupData = data as? Map<String, Any?>
                
                v.findViewById<TextView>(R.id.tv_group_name)?.text = 
                    groupData?.get("name") as? String ?: groupName
                v.findViewById<TextView>(R.id.tv_group_id)?.text = 
                    "ID: ${groupData?.get("id") ?: groupId}"
                v.findViewById<TextView>(R.id.tv_created_at)?.text = 
                    "创建于 ${groupData?.get("createdAt") ?: "未知"}"
                    
                val memberCount = (groupData?.get("memberCount") as? Number)?.toInt() ?: 0
                v.findViewById<TextView>(R.id.tv_member_count)?.text = 
                    memberCount.toString()
                    
                val storageUsed = groupData?.get("storageUsed") as? Number
                v.findViewById<TextView>(R.id.tv_storage_used)?.text = 
                    formatStorageSize(storageUsed?.toLong() ?: 0L)
                    
                v.findViewById<TextView>(R.id.tv_visibility)?.text = 
                    when (groupData?.get("visibility") as? String) {
                        "public" -> "公开"
                        "private" -> "私有"
                        else -> "未知"
                    }
                    
                v.findViewById<TextView>(R.id.tv_role)?.text = 
                    when (groupData?.get("myRole") as? String) {
                        "owner" -> "所有者"
                        "editor" -> "编辑者"
                        "viewer" -> "查看者"
                        else -> "成员"
                    }
            } catch (e: Exception) {
                
            }
        }
    }

    private fun formatStorageSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    private fun setupActionButtons(view: View) {
        view.findViewById<View>(R.id.btn_invite)?.setOnClickListener {
            
            Toast.makeText(requireContext(), "邀请成员功能开发中", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btn_leave)?.setOnClickListener {
            
            showLeaveGroupDialog()
        }
    }

    private fun showLeaveGroupDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("退出群组")
            .setMessage("确定要退出该群组吗？")
            .setPositiveButton("确定") { _, _ ->
                leaveGroup()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun leaveGroup() {
        groupId?.let { id ->
            
            Toast.makeText(requireContext(), "退出群组功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}