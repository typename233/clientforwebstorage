package com.example.clientforwebstorage.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.PurgeRecycleRequest
import com.example.clientforwebstorage.network.models.Resource
import com.example.clientforwebstorage.network.models.ResourceListData
import com.example.clientforwebstorage.network.models.Share
import com.example.clientforwebstorage.network.models.ShareListData
import com.example.clientforwebstorage.network.models.UserActivity
import com.example.clientforwebstorage.network.models.UserActivityListData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var onLogout: (() -> Unit)? = null

    fun setLogoutCallback(callback: () -> Unit) {
        onLogout = callback
    }

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
        val tvNickname = view.findViewById<TextView>(R.id.tv_nickname)
        val tvEmail = view.findViewById<TextView>(R.id.tv_email)

        tvNickname.text = TokenManager.getNickname() ?: "用户"
        tvEmail.text = "user@example.com"

        switchNotifications.setOnCheckedChangeListener { _, checked ->
            Toast.makeText(requireContext(), if (checked) "已开启通知" else "已关闭通知", Toast.LENGTH_SHORT).show()
        }

        itemLogout.setOnClickListener {
            TokenManager.clearTokens()
            onLogout?.invoke()
        }

        view.findViewById<View>(R.id.item_shares).setOnClickListener { showSharesDialog() }
        view.findViewById<View>(R.id.item_activities).setOnClickListener { showActivitiesDialog() }
        view.findViewById<View>(R.id.item_storage).setOnClickListener { showRecycleBinDialog() }

        view.findViewById<View>(R.id.item_favorites)?.setOnClickListener {
            Toast.makeText(requireContext(), "收藏夹", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.item_privacy)?.setOnClickListener {
            Toast.makeText(requireContext(), "隐私与安全", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.item_language)?.setOnClickListener {
            Toast.makeText(requireContext(), "语言设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSharesDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val root = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(400))
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        val titleView = TextView(requireContext()).apply {
            text = "我的分享"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val statusView = TextView(requireContext()).apply {
            text = "加载中..."
            textSize = 14f
            setPadding(0, dpToPx(8), 0, 0)
        }
        container.addView(titleView)
        container.addView(statusView)
        root.addView(container)
        dialog.setContentView(root)
        dialog.show()

        RetrofitClient.api.getShareList(1, 100)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseData<ShareListData>(response.body()?.data)
                        val shares = data?.items ?: emptyList()
                        statusView.text = "${shares.size} 个分享"
                        shares.forEach { share -> addShareItem(container, share, dialog) }
                    } else {
                        statusView.text = "加载失败"
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    statusView.text = "网络错误"
                }
            })
    }

    private fun addShareItem(container: LinearLayout, share: Share, dialog: Dialog) {
        val itemView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(10), 0, dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val infoView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        infoView.addView(TextView(requireContext()).apply {
            text = share.shareCode
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        infoView.addView(TextView(requireContext()).apply {
            text = share.createdAt
            textSize = 12f
            setTextColor(0xFF999999.toInt())
        })

        val revokeBtn = TextView(requireContext()).apply {
            text = if (share.revoked == true) "已撤销" else "撤销"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(if (share.revoked == true) 0xFF999999.toInt() else 0xFFD32F2F.toInt())
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            if (share.revoked != true) {
                setOnClickListener {
                    revokeShare(share.id, container, dialog)
                }
            }
        }
        itemView.addView(infoView)
        itemView.addView(revokeBtn)
        container.addView(itemView)
        container.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0xFFE5E7EB.toInt())
        })
    }

    private fun revokeShare(shareId: String, container: LinearLayout, dialog: Dialog) {
        RetrofitClient.api.revokeShare(shareId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已撤销分享", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        showSharesDialog()
                    } else {
                        Toast.makeText(requireContext(), "撤销失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showRecycleBinDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val root = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(400))
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        val titleView = TextView(requireContext()).apply {
            text = "回收站"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val statusView = TextView(requireContext()).apply {
            text = "加载中..."
            textSize = 14f
            setPadding(0, dpToPx(8), 0, 0)
        }

        val toolbarRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dpToPx(8))
            visibility = View.GONE
        }

        container.addView(titleView)
        container.addView(toolbarRow)
        container.addView(statusView)
        root.addView(container)
        dialog.setContentView(root)
        dialog.show()

        RetrofitClient.api.getRecycleResources(null, null, null)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseData<ResourceListData>(response.body()?.data)
                        val items = data?.items ?: emptyList()
                        statusView.text = "${items.size} 个已删除资源"

                        if (items.isNotEmpty()) {
                            val purgeAllBtn = TextView(requireContext()).apply {
                                text = "清空回收站"
                                textSize = 13f
                                setTextColor(0xFFD32F2F.toInt())
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                setPadding(0, 0, 0, 0)
                                setOnClickListener {
                                    purgeRecycle(container, dialog)
                                }
                            }
                            toolbarRow.addView(purgeAllBtn)
                            toolbarRow.visibility = View.VISIBLE
                        }

                        items.forEach { item -> addRecycleItem(container, item, dialog) }
                    } else {
                        statusView.text = "加载失败"
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    statusView.text = "网络错误"
                }
            })
    }

    private fun addRecycleItem(container: LinearLayout, resource: Resource, dialog: Dialog) {
        val itemView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(10), 0, dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val infoView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        infoView.addView(TextView(requireContext()).apply {
            text = resource.name
            textSize = 14f
        })
        infoView.addView(TextView(requireContext()).apply {
            text = resource.updatedAt
            textSize = 12f
            setTextColor(0xFF999999.toInt())
        })

        val restoreBtn = TextView(requireContext()).apply {
            text = "恢复"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1976D2.toInt())
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            setOnClickListener {
                restoreResource(resource.id, container, dialog)
            }
        }
        itemView.addView(infoView)
        itemView.addView(restoreBtn)
        container.addView(itemView)
        container.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0xFFE5E7EB.toInt())
        })
    }

    private fun restoreResource(resourceId: String, container: LinearLayout, dialog: Dialog) {
        RetrofitClient.api.restoreResource(resourceId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "已恢复", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        showRecycleBinDialog()
                    } else {
                        Toast.makeText(requireContext(), "恢复失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun purgeRecycle(container: LinearLayout, dialog: Dialog) {
        RetrofitClient.api.purgeRecycle(PurgeRecycleRequest(purgeAll = true, resourceIds = null))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(requireContext(), "回收站已清空", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        showRecycleBinDialog()
                    } else {
                        Toast.makeText(requireContext(), "清空失败", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showActivitiesDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val root = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(400))
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        val titleView = TextView(requireContext()).apply {
            text = "操作记录"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val statusView = TextView(requireContext()).apply {
            text = "加载中..."
            textSize = 14f
            setPadding(0, dpToPx(8), 0, 0)
        }
        container.addView(titleView)
        container.addView(statusView)
        root.addView(container)
        dialog.setContentView(root)
        dialog.show()

        RetrofitClient.api.getUserActivities(1, 50)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = parseData<UserActivityListData>(response.body()?.data)
                        val activities = data?.items ?: emptyList()
                        statusView.text = "${activities.size} 条操作记录"
                        activities.forEach { activity -> addActivityItem(container, activity) }
                    } else {
                        statusView.text = "加载失败"
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    statusView.text = "网络错误"
                }
            })
    }

    private fun addActivityItem(container: LinearLayout, activity: UserActivity) {
        container.addView(TextView(requireContext()).apply {
            text = "${activity.eventType} · ${activity.result}"
            textSize = 14f
            setPadding(0, dpToPx(10), 0, 0)
        })
        container.addView(TextView(requireContext()).apply {
            text = activity.createdAt
            textSize = 12f
            setTextColor(0xFF999999.toInt())
        })
        container.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0xFFE5E7EB.toInt())
            setPadding(0, 0, 0, dpToPx(8))
        })
    }

    private inline fun <reified T> parseData(data: Any?): T? {
        if (data == null) return null
        return try {
            val json = Gson().toJson(data)
            Gson().fromJson(json, object : TypeToken<T>() {}.type)
        } catch (_: Exception) { null }
    }

    private fun dpToPx(dp: Int): Int = (dp * requireContext().resources.displayMetrics.density).toInt()
}
