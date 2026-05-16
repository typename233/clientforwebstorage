package com.example.clientforwebstorage.ui.groups

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchGroupsFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etSearchInput: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var recyclerResults: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutNoResults: LinearLayout
    private lateinit var tvEmptyHint: TextView

    private lateinit var searchAdapter: GroupListAdapter
    private var searchResults: List<GroupItem> = emptyList()
    private var currentSearchJob: Call<ApiResponse>? = null

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L

        fun newInstance(): SearchGroupsFragment {
            return SearchGroupsFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        setupSearchInput()
        setupRecyclerView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelCurrentSearch()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_search)
        etSearchInput = view.findViewById(R.id.et_search_input)
        btnClearSearch = view.findViewById(R.id.btn_clear_search)
        recyclerResults = view.findViewById(R.id.recycler_search_results)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        layoutNoResults = view.findViewById(R.id.layout_no_results)
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupSearchInput() {
        etSearchInput.addTextChangedListener(object : TextWatcher {
            private var debounceRunnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s?.toString()?.trim().orEmpty()

                btnClearSearch.visibility = if (searchText.isNotEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                debounceRunnable?.let { etSearchInput.removeCallbacks(it) }

                if (searchText.isNotEmpty()) {
                    if (searchText.length < 2) {
                        showSearchingState()
                        return@onTextChanged
                    }

                    debounceRunnable = Runnable { performSearch() }
                    etSearchInput.postDelayed(debounceRunnable!!, SEARCH_DEBOUNCE_MS)
                } else {
                    cancelCurrentSearch()
                    showEmptyState()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        etSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        btnClearSearch.setOnClickListener {
            etSearchInput.text?.clear()
            etSearchInput.clearFocus()
            cancelCurrentSearch()
            showEmptyState()
        }
    }

    private fun setupRecyclerView() {
        recyclerResults.layoutManager = LinearLayoutManager(requireContext())
        searchAdapter = GroupListAdapter(
            searchResults,
            onItemClick = { item -> navigateToChat(item.id, item.name) },
            onItemLongClick = { _, _ -> },
            onMoreButtonClick = { _, _ -> }
        )
        recyclerResults.adapter = searchAdapter
    }

    private fun performSearch() {
        val query = etSearchInput.text?.toString()?.trim().orEmpty()

        if (query.isEmpty()) {
            showEmptyState()
            return
        }

        if (query.length < 2) {
            Toast.makeText(requireContext(), "请输入至少2个字符进行搜索", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("SearchGroupsFragment", "=== 开始搜索群组 ===")
        android.util.Log.d("SearchGroupsFragment", "搜索关键词: $query")

        cancelCurrentSearch()
        showSearchingState()

        currentSearchJob = RetrofitClient.api.listConversations(query, 1, DEFAULT_PAGE_SIZE)
        currentSearchJob?.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (!isAdded) return@onResponse

                android.util.Log.d("SearchGroupsFragment", "=== 搜索响应 ===")
                android.util.Log.d("SearchGroupsFragment", "isSuccessful: ${response.isSuccessful}")
                android.util.Log.d("SearchGroupsFragment", "responseCode: ${response.code()}")

                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = parseSearchResultData(response.body()?.data)
                    if (data != null) {
                        android.util.Log.d("SearchGroupsFragment",
                            "搜索成功 - 总数: ${data.total}, items数量: ${data.items.size}")

                        searchResults = data.items
                            .filter { it.conversationType == "group" }
                            .map { conv ->
                                val role = conv.role ?: "member"
                                val memberCount = conv.memberCount ?: 0

                                android.util.Log.d("SearchGroupsFragment",
                                    "搜索结果 - id: ${conv.id}, name: ${conv.name}, role: $role, memberCount: $memberCount")

                                GroupItem(
                                    id = conv.id,
                                    name = conv.name,
                                    role = role,
                                    memberCount = memberCount,
                                    storageUsed = "",
                                    visibility = if (conv.status == "active") "public" else "private",
                                    isPinned = false,
                                    isMuted = conv.isMuted
                                )
                            }

                        if (searchResults.isEmpty()) {
                            showNoResultsState()
                            android.util.Log.d("SearchGroupsFragment", "搜索无结果（已过滤非群组类型）")
                        } else {
                            showResultsState()
                            updateSearchAdapter()
                            android.util.Log.d("SearchGroupsFragment",
                                "搜索完成，显示 ${searchResults.size} 个群组结果")
                        }
                    } else {
                        android.util.Log.e("SearchGroupsFragment", "解析搜索结果数据失败")
                        showErrorState("数据解析失败")
                    }
                } else {
                    val errorCode = response.body()?.code
                    val errorMessage = response.body()?.message ?: "未知错误"
                    android.util.Log.e("SearchGroupsFragment",
                        "搜索请求失败 - code: $errorCode, message: $errorMessage")
                    showErrorState(errorMessage)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                if (!isAdded) return@onFailure
                android.util.Log.e("SearchGroupsFragment", "搜索网络错误", t)
                showErrorState("网络错误: ${t.message}")
            }
        })
    }

    private fun cancelCurrentSearch() {
        currentSearchJob?.cancel()
        currentSearchJob = null
    }

    private fun updateSearchAdapter() {
        searchAdapter = GroupListAdapter(
            searchResults,
            onItemClick = { item -> navigateToChat(item.id, item.name) },
            onItemLongClick = { _, _ -> },
            onMoreButtonClick = { _, _ -> }
        )
        recyclerResults.adapter = searchAdapter
    }

    private fun parseSearchResultData(data: Any?): com.example.clientforwebstorage.network.models.ConversationListResponse? {
        if (data == null) {
            android.util.Log.w("SearchGroupsFragment", "搜索返回数据为null")
            return null
        }
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json,
                object : TypeToken<com.example.clientforwebstorage.network.models.ConversationListResponse>() {}.type)
        } catch (e: Exception) {
            android.util.Log.e("SearchGroupsFragment", "解析搜索数据异常", e)
            null
        }
    }

    private fun navigateToChat(conversationId: String, conversationName: String) {
        val chatFragment = ChatFragment.newInstance(conversationId, conversationName)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chatFragment)
            .addToBackStack("chat_from_search")
            .commit()
    }

    private fun showEmptyState() {
        layoutEmptyState.visibility = View.VISIBLE
        layoutNoResults.visibility = View.GONE
        recyclerResults.visibility = View.GONE
        tvEmptyHint.text = "输入关键词搜索群组"
    }

    private fun showSearchingState() {
        layoutEmptyState.visibility = View.GONE
        layoutNoResults.visibility = View.GONE
        recyclerResults.visibility = View.GONE
        tvEmptyHint.text = "正在搜索..."
    }

    private fun showNoResultsState() {
        layoutEmptyState.visibility = View.GONE
        layoutNoResults.visibility = View.VISIBLE
        recyclerResults.visibility = View.GONE
    }

    private fun showResultsState() {
        layoutEmptyState.visibility = View.GONE
        layoutNoResults.visibility = View.GONE
        recyclerResults.visibility = View.VISIBLE
    }

    private fun showErrorState(message: String) {
        layoutEmptyState.visibility = View.VISIBLE
        layoutNoResults.visibility = View.GONE
        recyclerResults.visibility = View.GONE
        tvEmptyHint.text = message
    }
}
