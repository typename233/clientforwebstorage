package com.example.clientforwebstorage.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.Share
import com.example.clientforwebstorage.network.models.ShareListData
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SharesFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvTotalShares: TextView
    private lateinit var tvActiveShares: TextView
    private lateinit var tvTotalAccess: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var containerList: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_shares, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar()
        loadSharesData()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar_shares)
        tvTotalShares = view.findViewById(R.id.tv_total_shares)
        tvActiveShares = view.findViewById(R.id.tv_active_shares)
        tvTotalAccess = view.findViewById(R.id.tv_total_access)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        progressLoading = view.findViewById(R.id.progress_loading)
        containerList = view.findViewById(R.id.container_share_list)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadSharesData() {
        progressLoading.visibility = View.VISIBLE
        containerList.visibility = View.GONE
        tvEmptyState.visibility = View.GONE

        RetrofitClient.api.getShareList(1, 100)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    progressLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseData<ShareListData>(response.body()?.data)
                        val shares = data?.items ?: emptyList()

                        updateStatistics(shares)
                        displayShareItems(shares)
                    } else {
                        showError("加载失败")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    progressLoading.visibility = View.GONE
                    showError("网络错误：${t.message}")
                }
            })
    }

    private fun updateStatistics(shares: List<Share>) {
        val totalShares = shares.size
        val activeShares = shares.count {
            it.revoked != true &&
            it.alreadyRevoked != true &&
            it.status != "revoked" &&
            it.status != "expired" &&
            !isExpired(it.expiredAt)
        }
        val totalAccess = shares.sumOf { it.currentAccessCount }

        tvTotalShares.text = totalShares.toString()
        tvActiveShares.text = activeShares.toString()
        tvTotalAccess.text = totalAccess.toString()
    }

    private fun displayShareItems(shares: List<Share>) {
        containerList.removeAllViews()

        if (shares.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            containerList.visibility = View.GONE
            return
        }

        tvEmptyState.visibility = View.GONE
        containerList.visibility = View.VISIBLE

        shares.forEach { share ->
            addShareItemView(share)
        }
    }

    private fun addShareItemView(share: Share) {
        val itemView = layoutInflater.inflate(R.layout.item_share, containerList, false)

        val tvTitle = itemView.findViewById<TextView>(R.id.tv_share_title)
        val tvCode = itemView.findViewById<TextView>(R.id.tv_share_code)
        val tvResourceCount = itemView.findViewById<TextView>(R.id.tv_resource_count)
        val tvAccessCount = itemView.findViewById<TextView>(R.id.tv_access_count)
        val tvStatus = itemView.findViewById<TextView>(R.id.tv_status)
        val tvCreatedAt = itemView.findViewById<TextView>(R.id.tv_created_at)
        val btnRevoke = itemView.findViewById<TextView>(R.id.btn_revoke)

        val badgePreview = itemView.findViewById<TextView>(R.id.badge_preview)
        val badgeDownload = itemView.findViewById<TextView>(R.id.badge_download)
        val badgeNeedCode = itemView.findViewById<TextView>(R.id.badge_need_code)
        val badgeExpired = itemView.findViewById<TextView>(R.id.badge_expired)
        val badgeRevoked = itemView.findViewById<TextView>(R.id.badge_revoked)

        tvTitle.text = share.title ?: "未命名分享"
        tvCode.text = "分享码：${share.shareCode}"

        val resourceCount = share.resourceCount ?: share.resourceIds?.size ?: 0
        tvResourceCount.text = "$resourceCount 个文件"
        tvAccessCount.text = "${share.currentAccessCount} 次"

        val createdAtFormatted = formatDate(share.createdAt)
        tvCreatedAt.text = createdAtFormatted

        when {
            share.revoked == true || share.alreadyRevoked == true || share.status == "revoked" -> {
                tvStatus.text = "已撤销"
                tvStatus.setTextColor(0xFF9E9E9E.toInt())
                badgeRevoked.visibility = View.VISIBLE
                btnRevoke.visibility = View.GONE
            }
            isExpired(share.expiredAt) || share.status == "expired" -> {
                tvStatus.text = "已过期"
                tvStatus.setTextColor(0xFFD32F2F.toInt())
                badgeExpired.visibility = View.VISIBLE
                btnRevoke.text = "删除记录"
            }
            else -> {
                tvStatus.text = "有效"
                tvStatus.setTextColor(0xFF4CAF50.toInt())
            }
        }

        if (share.allowPreview) {
            badgePreview.visibility = View.VISIBLE
        }
        if (share.allowDownload) {
            badgeDownload.visibility = View.VISIBLE
        }
        if (share.needCode) {
            badgeNeedCode.visibility = View.VISIBLE
        }

        btnRevoke.setOnClickListener {
            showRevokeConfirmDialog(share.id, share.title ?: "该分享", itemView)
        }

        containerList.addView(itemView)
    }

    private fun revokeShare(shareId: String, itemView: View) {
        updateItemViewToRevoked(itemView)

        RetrofitClient.api.revokeShare(shareId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "分享已撤销", Toast.LENGTH_SHORT).show()
                        loadSharesData()
                    } else {
                        Toast.makeText(requireContext(), "撤销失败", Toast.LENGTH_SHORT).show()
                        loadSharesData()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                    loadSharesData()
                }
            })
    }

    private fun updateItemViewToRevoked(itemView: View) {
        val tvStatus = itemView.findViewById<TextView>(R.id.tv_status)
        val badgeRevoked = itemView.findViewById<TextView>(R.id.badge_revoked)
        val btnRevoke = itemView.findViewById<TextView>(R.id.btn_revoke)

        tvStatus.text = "已撤销"
        tvStatus.setTextColor(0xFF9E9E9E.toInt())
        badgeRevoked.visibility = View.VISIBLE
        btnRevoke.visibility = View.GONE
    }

    private fun showRevokeConfirmDialog(shareId: String, shareTitle: String, itemView: View) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("撤销分享")
            .setMessage("确定要撤销「$shareTitle」的分享吗？\n撤销后链接将失效，他人无法访问。")
            .setPositiveButton("确定撤销") { _, _ ->
                revokeShare(shareId, itemView)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showError(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
        containerList.visibility = View.GONE
    }

    private fun isExpired(expiredAt: String?): Boolean {
        if (expiredAt.isNullOrBlank()) return false
        
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val expiredDate = format.parse(expiredAt)
            expiredDate?.before(Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateStr
        }
    }

    private inline fun <reified T> parseData(data: Any?): T? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<T>() {}.type)
        } catch (_: Exception) { null }
    }
}
