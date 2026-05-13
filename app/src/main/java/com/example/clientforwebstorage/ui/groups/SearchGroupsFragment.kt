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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clientforwebstorage.R
import com.google.android.material.appbar.MaterialToolbar

class SearchGroupsFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etSearchInput: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var recyclerResults: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutNoResults: LinearLayout
    private lateinit var tvEmptyHint: TextView

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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s?.toString()?.trim().orEmpty()

                btnClearSearch.visibility = if (searchText.isNotEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                if (searchText.isNotEmpty()) {
                    showSearchingState()
                } else {
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
            showEmptyState()
        }
    }

    private fun setupRecyclerView() {
        recyclerResults.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun performSearch() {
        val query = etSearchInput.text?.toString()?.trim().orEmpty()

        if (query.isEmpty()) {
            showEmptyState()
            return
        }

        showSearchingState()
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

    companion object {
        fun newInstance(): SearchGroupsFragment {
            return SearchGroupsFragment()
        }
    }
}
