package com.example.clientforwebstorage.ui.profile

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

class NotificationsFragment : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var tvEmpty: TextView
    private var unreadOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiService = RetrofitClient.api
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        setupActionButtons()
        loadNotifications()
    }

    private fun initViews(view: View) {
        recyclerNotifications = view.findViewById(R.id.recycler_notifications)
        tvEmpty = view.findViewById(R.id.tv_empty)
        
        recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupToolbar() {
        view?.findViewById<MaterialToolbar>(R.id.toolbar_notifications)?.apply {
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun setupActionButtons() {
        view?.findViewById<TextView>(R.id.btn_unread_only)?.setOnClickListener {
            unreadOnly = !unreadOnly
            
            if (unreadOnly) {
                it.setBackgroundResource(R.drawable.bg_tag_selected)
                (it as TextView).setTextColor(resources.getColor(R.color.primary_blue, null))
            } else {
                it.background = null
                (it as TextView).setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            
            loadNotifications()
        }

        view?.findViewById<View>(R.id.btn_mark_all_read)?.setOnClickListener {
            markAllAsRead()
        }
    }

    private fun loadNotifications() {
        apiService.getNotifications(1, 50, unreadOnly).enqueue(object : 
            Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
            
            override fun onResponse(
                call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.code == 0) {
                        updateNotificationsList(apiResponse.data)
                    } else {
                        showError("加载通知失败: ${apiResponse.message}")
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

    @Suppress("UNCHECKED_CAST")
    private fun updateNotificationsList(data: Any?) {
        try {
            val listData = data as? Map<String, Any?>
            val items = listData?.get("items") as? List<*>
            
            if (items.isNullOrEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                recyclerNotifications.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                recyclerNotifications.visibility = View.VISIBLE
                
                val adapter = NotificationsAdapter(items.filterNotNull()) { notificationId ->
                    markAsRead(notificationId)
                }
                recyclerNotifications.adapter = adapter
            }
        } catch (e: Exception) {
            tvEmpty.visibility = View.VISIBLE
            recyclerNotifications.visibility = View.GONE
        }
    }

    private fun markAsRead(notificationId: String) {
        apiService.markNotificationRead(notificationId).enqueue(object : 
            Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
            
            override fun onResponse(
                call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
            ) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    Toast.makeText(requireContext(), "已标记为已读", Toast.LENGTH_SHORT).show()
                    loadNotifications()
                }
            }

            override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                
            }
        })
    }

    private fun markAllAsRead() {
        apiService.markAllNotificationsRead().enqueue(object : 
            Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
            
            override fun onResponse(
                call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
            ) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    Toast.makeText(requireContext(), "已全部标记为已读", Toast.LENGTH_SHORT).show()
                    loadNotifications()
                } else {
                    showError("操作失败")
                }
            }

            override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                showError("网络错误")
            }
        })
    }

    inner class NotificationsAdapter(
        private val items: List<Any>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

        inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: TextView = itemView.findViewById(android.R.id.text1)
            val tvContent: TextView = itemView.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return NotificationViewHolder(view)
        }

        @Suppress("UNCHECKED_CAST")
        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val item = items[position] as? Map<String, Any?>
            
            holder.tvTitle.text = item?.get("title") as? String ?: "通知"
            holder.tvContent.text = item?.get("content") as? String ?: ""
            
            val isRead = item?.get("isRead") as? Boolean ?: true
            if (!isRead) {
                holder.tvTitle.alpha = 1.0f
                holder.tvContent.alpha = 1.0f
            } else {
                holder.tvTitle.alpha = 0.6f
                holder.tvContent.alpha = 0.6f
            }
            
            holder.itemView.setOnClickListener {
                val notificationId = item?.get("notificationId") as? String ?: return@setOnClickListener
                onItemClick(notificationId)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}