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

class FavoritesFragment : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiService = RetrofitClient.api
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        loadFavorites()
    }

    private fun initViews(view: View) {
        recyclerFavorites = view.findViewById(R.id.recycler_favorites)
        tvEmpty = view.findViewById(R.id.tv_empty)
        
        recyclerFavorites.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupToolbar() {
        view?.findViewById<MaterialToolbar>(R.id.toolbar_favorites)?.apply {
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadFavorites() {
        apiService.getFavorites(1, 50).enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
            override fun onResponse(
                call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.code == 0) {
                        updateFavoritesList(apiResponse.data)
                    } else {
                        showError("加载收藏列表失败: ${apiResponse.message}")
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
    private fun updateFavoritesList(data: Any?) {
        try {
            val listData = data as? Map<String, Any?>
            val items = listData?.get("items") as? List<*>
            
            if (items.isNullOrEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                recyclerFavorites.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                recyclerFavorites.visibility = View.VISIBLE
                
                val adapter = FavoritesAdapter(items.filterNotNull())
                recyclerFavorites.adapter = adapter
            }
        } catch (e: Exception) {
            tvEmpty.visibility = View.VISIBLE
            recyclerFavorites.visibility = View.GONE
        }
    }

    inner class FavoritesAdapter(private val items: List<Any>) :
        RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

        inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(android.R.id.text1)
            val tvSize: TextView = itemView.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return FavoriteViewHolder(view)
        }

        @Suppress("UNCHECKED_CAST")
        override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
            val item = items[position] as? Map<String, Any?>
            
            holder.tvName.text = item?.get("title") as? String ?: "未知文件"
            
            val size = item?.get("size") as? Number ?: 0L
            holder.tvSize.text = formatFileSize(size.toLong())
        }

        override fun getItemCount(): Int = items.size

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}