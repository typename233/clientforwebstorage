package com.example.clientforwebstorage.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.ApiService
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.ui.PreviewActivity
import com.example.clientforwebstorage.ui.files.FavoritesManager
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavoritesFragment : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var recyclerFavorites: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: FavoritesAdapter
    private lateinit var progressLoading: ProgressBar
    private lateinit var layoutMultiselect: LinearLayout
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnBatchUnfavorite: TextView
    private lateinit var btnCancelSelect: TextView
    private lateinit var toolbar: MaterialToolbar

    private var isMultiSelectMode = false
    private val selectedItems = mutableSetOf<Any>()
    private var currentItems = listOf<Any>()

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
        setupMultiselectToolbar()
        loadFavorites()
    }

    private fun initViews(view: View) {
        recyclerFavorites = view.findViewById(R.id.recycler_favorites)
        tvEmpty = view.findViewById(R.id.tv_empty)
        progressLoading = view.findViewById(R.id.progress_loading)
        layoutMultiselect = view.findViewById(R.id.layout_multiselect_toolbar)
        tvSelectedCount = view.findViewById(R.id.tv_selected_count)
        btnBatchUnfavorite = view.findViewById(R.id.btn_batch_unfavorite)
        btnCancelSelect = view.findViewById(R.id.btn_cancel_select)
        toolbar = view.findViewById(R.id.toolbar_favorites)

        recyclerFavorites.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupToolbar() {
        toolbar.apply {
            setNavigationOnClickListener {
                if (isMultiSelectMode) {
                    exitMultiSelectMode()
                } else {
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun setupMultiselectToolbar() {
        btnBatchUnfavorite.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                showBatchUnfavoriteConfirm()
            }
        }

        btnCancelSelect.setOnClickListener {
            exitMultiSelectMode()
        }
    }

    private fun loadFavorites() {
        showLoading(true)

        apiService.getFavorites(1, 50).enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
            override fun onResponse(
                call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
            ) {
                if (!isAdded) return@onResponse
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.code == 0) {
                        updateFavoritesList(apiResponse.data)
                    } else {
                        showError("加载收藏列表失败: ${apiResponse.message}")
                        showEmptyView()
                    }
                } else {
                    showError("网络请求失败")
                    showEmptyView()
                }
            }

            override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                if (!isAdded) return@onFailure
                showLoading(false)
                showError("网络错误: ${t.message}")
                showEmptyView()
            }
        })
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateFavoritesList(data: Any?) {
        try {
            val listData = data as? Map<String, Any?>
            val items = listData?.get("items") as? List<*>

            currentItems = items?.filterNotNull() ?: emptyList()

            Log.d("FavoritesFragment", "收藏列表数据解析完成, 共${currentItems.size}项")
            if (currentItems.isNotEmpty()) {
                val firstItem = currentItems[0]
                Log.d("FavoritesFragment", "首项数据结构: $firstItem")
                Log.d("FavoritesFragment", "首项类型: ${firstItem?.javaClass?.simpleName}")
                if (firstItem is Map<*, *>) {
                    Log.d("FavoritesFragment", "首项所有keys: ${firstItem.keys}")
                }
            }

            if (currentItems.isEmpty()) {
                showEmptyView()
            } else {
                hideEmptyView()
                setupAdapter()
            }
        } catch (e: Exception) {
            Log.e("FavoritesFragment", "解析收藏列表失败", e)
            showEmptyView()
        }
    }

    private fun setupAdapter() {
        adapter = FavoritesAdapter(
            currentItems,
            isMultiSelectMode = isMultiSelectMode,
            selectedItems = selectedItems,
            onUnfavoriteClick = { item -> unfavoriteItem(item) },
            onItemClick = { item -> handleItemClick(item) },
            onItemLongClick = { item -> handleItemLongClick(item) },
            onMoreClick = { item -> showFileDetail(item) },
            onSelectionChanged = { updateMultiselectUI() }
        )
        recyclerFavorites.adapter = adapter
    }

    private fun handleItemClick(item: Any) {
        if (isMultiSelectMode) {
            toggleItemSelection(item)
        } else {
            previewFile(item)
        }
    }

    private fun handleItemLongClick(item: Any): Boolean {
        if (!isMultiSelectMode) {
            enterMultiSelectMode(item)
            return true
        } else {
            showFileDetail(item)
            return true
        }
    }

    private fun enterMultiSelectMode(initialItem: Any) {
        isMultiSelectMode = true
        selectedItems.clear()
        selectedItems.add(initialItem)

        layoutMultiselect.visibility = View.VISIBLE
        toolbar.title = "已选择 ${selectedItems.size} 项"

        updateAdapterForMultiSelect()
        updateMultiselectUI()

        showToast("已进入多选模式")
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedItems.clear()

        layoutMultiselect.visibility = View.GONE
        toolbar.title = "我的收藏"

        updateAdapterForMultiSelect()
    }

    private fun toggleItemSelection(item: Any) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
        adapter.notifyDataSetChanged()
        updateMultiselectUI()
    }

    private fun updateMultiselectUI() {
        tvSelectedCount.text = "已选择 ${selectedItems.size} 项"
        btnBatchUnfavorite.text = "取消收藏 (${selectedItems.size})"
        toolbar.title = "已选择 ${selectedItems.size} 项"

        btnBatchUnfavorite.alpha = if (selectedItems.isNotEmpty()) 1.0f else 0.5f
        btnBatchUnfavorite.isEnabled = selectedItems.isNotEmpty()
    }

    private fun updateAdapterForMultiSelect() {
        if (::adapter.isInitialized) {
            adapter.setMultiSelectMode(isMultiSelectMode, selectedItems)
            adapter.notifyDataSetChanged()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveResourceId(itemMap: Map<String, Any?>): String? {
        val idCandidates = listOf("id", "resourceId", "_id", "resource_id", "resourceId")

        for (key in idCandidates) {
            val value = itemMap[key]
            if (value is String && value.isNotEmpty()) {
                Log.d("FavoritesFragment", "从字段 '$key' 获取到ID: $value")
                return value
            }
            if (value is Number) {
                val strValue = value.toString()
                Log.d("FavoritesFragment", "从字段 '$key' 获取到ID(数字类型): $strValue")
                return strValue
            }
        }

        val resourceObj = itemMap["resource"] as? Map<String, Any?>
        if (resourceObj != null) {
            Log.d("FavoritesFragment", "检测到嵌套的resource对象, 尝试从中获取id, keys=${resourceObj.keys}")
            for (key in idCandidates) {
                val value = resourceObj[key]
                if (value is String && value.isNotEmpty()) {
                    Log.d("FavoritesFragment", "从 resource.$key 获取到ID: $value")
                    return value
                }
                if (value is Number) {
                    val strValue = value.toString()
                    Log.d("FavoritesFragment", "从 resource.$key 获取到ID(数字类型): $strValue")
                    return strValue
                }
            }
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun unfavoriteItem(item: Any) {
        val itemMap = item as? Map<String, Any?>
        if (itemMap == null) {
            Log.e("FavoritesFragment", "unfavoriteItem: 无法解析item数据, item类型=${item.javaClass.simpleName}, item=$item")
            showToast("✗ 数据格式错误，无法取消收藏")
            return
        }

        val resourceId = resolveResourceId(itemMap)
        if (resourceId == null) {
            Log.e("FavoritesFragment", "unfavoriteItem: 无法从item中解析ID, 所有keys=${itemMap.keys}, 完整数据=$itemMap")
            showToast("✗ 文件ID缺失，无法取消收藏")
            return
        }
        val resourceName = itemMap["name"] as? String ?: itemMap["title"] as? String ?: "未知文件"

        Log.d("FavoritesFragment", "正在取消收藏: id=$resourceId, name=$resourceName")
        showToast("正在取消收藏...")

        RetrofitClient.api.unfavoriteResource(resourceId).enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
            override fun onResponse(
                call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
            ) {
                if (!isAdded) return@onResponse
                if (response.isSuccessful && response.body()?.code == 0) {
                    FavoritesManager.removeFavorite(resourceId)
                    showToast("✓ 已取消收藏「$resourceName」")

                    if (isMultiSelectMode) {
                        selectedItems.remove(item)
                        currentItems = currentItems.filter { it != item }
                        if (currentItems.isEmpty()) {
                            exitMultiSelectMode()
                            showEmptyView()
                        } else {
                            setupAdapter()
                            updateMultiselectUI()
                        }
                    } else {
                        loadFavorites()
                    }
                } else {
                    showToast("✗ 取消收藏失败")
                }
            }

            override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                if (!isAdded) return@onFailure
                showToast("✗ 网络错误")
            }
        })
    }

    private fun showBatchUnfavoriteConfirm() {
        AlertDialog.Builder(requireContext())
            .setTitle("确认取消收藏")
            .setMessage("确定要取消收藏选中的 ${selectedItems.size} 个文件吗？")
            .setPositiveButton("确认") { _, _ ->
                batchUnfavorite()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun batchUnfavorite() {
        showToast("正在批量取消收藏...")

        var completedCount = 0
        var successCount = 0
        val totalCount = selectedItems.size

        selectedItems.forEach { item ->
            val itemMap = item as? Map<String, Any?> ?: return@forEach
            val resourceId = resolveResourceId(itemMap) ?: return@forEach

            RetrofitClient.api.unfavoriteResource(resourceId).enqueue(object : Callback<com.example.clientforwebstorage.network.models.ApiResponse> {
                override fun onResponse(
                    call: Call<com.example.clientforwebstorage.network.models.ApiResponse>,
                    response: Response<com.example.clientforwebstorage.network.models.ApiResponse>
                ) {
                    if (!isAdded) return@onResponse
                    completedCount++

                    if (response.isSuccessful && response.body()?.code == 0) {
                        successCount++
                        FavoritesManager.removeFavorite(resourceId)
                    }

                    if (completedCount == totalCount) {
                        onBatchComplete(successCount, totalCount)
                    }
                }

                override fun onFailure(call: Call<com.example.clientforwebstorage.network.models.ApiResponse>, t: Throwable) {
                    if (!isAdded) return@onFailure
                    completedCount++
                    if (completedCount == totalCount) {
                        onBatchComplete(successCount, totalCount)
                    }
                }
            })
        }
    }

    private fun onBatchComplete(successCount: Int, totalCount: Int) {
        showToast("✓ 成功取消 $successCount/$totalCount 个文件的收藏")

        exitMultiSelectMode()
        loadFavorites()
    }

    @Suppress("UNCHECKED_CAST")
    private fun previewFile(item: Any) {
        val itemMap = item as? Map<String, Any?> ?: return
        val resourceId = resolveResourceId(itemMap) ?: return
        val fileName = itemMap["name"] as? String ?: itemMap["title"] as? String ?: "未知文件"

        val intent = Intent(requireContext(), PreviewActivity::class.java).apply {
            putExtra("resource_id", resourceId)
            putExtra("resource_name", fileName)
        }
        startActivity(intent)
    }

    @Suppress("UNCHECKED_CAST")
    private fun showFileDetail(item: Any) {
        val itemMap = item as? Map<String, Any?> ?: return

        FileDetailDialog.create(itemMap).show(parentFragmentManager, "FileDetailDialog")
    }

    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerFavorites.visibility = View.GONE
            tvEmpty.visibility = View.GONE
        }
    }

    private fun showEmptyView() {
        tvEmpty.visibility = View.VISIBLE
        recyclerFavorites.visibility = View.GONE
    }

    private fun hideEmptyView() {
        tvEmpty.visibility = View.GONE
        recyclerFavorites.visibility = View.VISIBLE
    }

    inner class FavoritesAdapter(
        private val items: List<Any>,
        private var isMultiSelectMode: Boolean = false,
        private val selectedItems: Set<Any> = emptySet(),
        private val onUnfavoriteClick: (Any) -> Unit = {},
        private val onItemClick: (Any) -> Unit = {},
        private val onItemLongClick: (Any) -> Boolean = { false },
        private val onMoreClick: (Any) -> Unit = {},
        private val onSelectionChanged: () -> Unit = {}
    ) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

        inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkboxSelect: CheckBox = itemView.findViewById(R.id.checkbox_select)
            val ivIcon: ImageView = itemView.findViewById(R.id.iv_favorite_icon)
            val tvName: TextView = itemView.findViewById(R.id.tv_favorite_name)
            val tvMeta: TextView = itemView.findViewById(R.id.tv_favorite_meta)
            val btnUnfavorite: View = itemView.findViewById(R.id.btn_unfavorite)
            val btnMore: View = itemView.findViewById(R.id.btn_more)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite, parent, false)
            return FavoriteViewHolder(view)
        }

        @Suppress("UNCHECKED_CAST")
        override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
            val item = items[position] as? Map<String, Any?>

            holder.tvName.text = item?.get("name") as? String ?: item?.get("title") as? String ?: "未知文件"

            val size = item?.get("size") as? Number ?: 0L
            val updatedAt = item?.get("updatedAt") as? String ?: ""
            holder.tvMeta.text = "${formatFileSize(size.toLong())} · ${updatedAt}"

            val extension = item?.get("extension") as? String
            val iconRes = when (extension?.lowercase()) {
                "png", "jpg", "jpeg" -> android.R.drawable.ic_menu_gallery
                "mp4", "avi" -> android.R.drawable.ic_menu_slideshow
                "pdf", "doc", "docx" -> android.R.drawable.ic_menu_edit
                else -> android.R.drawable.ic_menu_save
            }
            holder.ivIcon.setImageResource(iconRes)

            // 多选模式UI更新 - 确保视觉反馈清晰
            if (isMultiSelectMode) {
                holder.checkboxSelect.visibility = View.VISIBLE
                holder.btnUnfavorite.visibility = View.GONE
                holder.btnMore.visibility = View.GONE

                val isSelected = selectedItems.contains(items[position])
                holder.checkboxSelect.isChecked = isSelected

                // 移除之前的监听器，避免重复触发
                holder.checkboxSelect.setOnCheckedChangeListener(null)
                holder.checkboxSelect.isChecked = isSelected
                holder.checkboxSelect.setOnCheckedChangeListener { _, _ ->
                    onItemClick(items[position])
                }

                // 设置选中状态的视觉背景
                if (isSelected) {
                    holder.itemView.setBackgroundColor(0x1A2196F3.toInt()) // 浅蓝色背景
                } else {
                    holder.itemView.background = null
                }

                holder.itemView.isActivated = isSelected
            } else {
                holder.checkboxSelect.visibility = View.GONE
                holder.btnUnfavorite.visibility = View.VISIBLE
                holder.btnMore.visibility = View.VISIBLE
                holder.itemView.background = null
                holder.itemView.isActivated = false
            }

            holder.itemView.setOnClickListener { onItemClick(items[position]) }
            holder.itemView.setOnLongClickListener { onItemLongClick(items[position]) }

            holder.btnUnfavorite.setOnClickListener {
                Log.d("FavoritesFragment", "btnUnfavorite被点击, position=$position")
                onUnfavoriteClick(items[position])
            }
            holder.btnMore.setOnClickListener { onMoreClick(items[position]) }
        }

        override fun getItemCount(): Int = items.size

        fun setMultiSelectMode(multiSelect: Boolean, selected: Set<Any>) {
            isMultiSelectMode = multiSelect
            // 通过重新创建adapter来更新状态
        }

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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
