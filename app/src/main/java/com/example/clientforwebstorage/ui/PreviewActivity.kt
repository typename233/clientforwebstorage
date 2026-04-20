package com.example.clientforwebstorage.ui

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.clientforwebstorage.R
import com.example.clientforwebstorage.network.RetrofitClient
import com.example.clientforwebstorage.network.models.ApiResponse
import com.example.clientforwebstorage.network.models.PreviewUrlResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PreviewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var titleText: TextView
    private var resourceId: String = ""
    private var resourceName: String = ""
    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        resourceId = intent.getStringExtra("resourceId") ?: ""
        resourceName = intent.getStringExtra("resourceName") ?: ""

        webView = findViewById(R.id.webView)
        titleText = findViewById(R.id.titleText)

        titleText.text = resourceName

        val closeBtn = findViewById<TextView>(R.id.closeBtn)
        closeBtn.setOnClickListener { finish() }

        setupWebView()
        setupBackPressedCallback()
        loadPreviewUrl()
    }

    private fun setupBackPressedCallback() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback!!)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }
        webView.setBackgroundColor(android.graphics.Color.WHITE)
        webView.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.WHITE)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                android.util.Log.d("PreviewActivity", "page started loading: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                android.util.Log.d("PreviewActivity", "page finished loading: $url")
                android.util.Log.d("PreviewActivity", "content height: ${view?.contentHeight}, scale: ${view?.scale}, webview height: ${view?.height}")
                view?.evaluateJavascript("document.body.innerHTML") { html ->
                    android.util.Log.d("PreviewActivity", "page HTML length: ${html?.length}")
                    android.util.Log.d("PreviewActivity", "page HTML preview: ${html?.take(500)}")
                }
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                android.util.Log.d("PreviewActivity", "load error: $errorCode, $description, $failingUrl")
                Toast.makeText(this@PreviewActivity, "加载失败：$description", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPreviewUrl() {
        RetrofitClient.api.getPreviewUrl(resourceId)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        android.util.Log.d("PreviewActivity", "response body: $apiResponse")
                        if (apiResponse?.code == 0) {
                            val previewResponse = parsePreviewUrlResponse(apiResponse.data)
                            android.util.Log.d("PreviewActivity", "preview response: $previewResponse")
                            if (previewResponse != null) {
                                android.util.Log.d("PreviewActivity", "loading url: ${previewResponse.url}")
                                android.util.Log.d("PreviewActivity", "mimeType: ${previewResponse.mimeType}")
                                
                                val mimeType = previewResponse.mimeType ?: ""
                                if (mimeType.startsWith("image/")) {
                                    val html = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head>
                                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
                                            <style>
                                                body { margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f5f5; }
                                                img { max-width: 100%; height: auto; }
                                            </style>
                                        </head>
                                        <body>
                                            <img src="${previewResponse.url}" alt="${previewResponse.filename ?: ""}">
                                        </body>
                                        </html>
                                    """.trimIndent()
                                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                } else {
                                    webView.loadUrl(previewResponse.url)
                                }
                            } else {
                                Toast.makeText(this@PreviewActivity, "获取预览链接失败", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@PreviewActivity, "获取预览链接失败：${apiResponse?.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.d("PreviewActivity", "error: code=${response.code()}, body=$errorBody")
                        Toast.makeText(this@PreviewActivity, "获取预览链接失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    android.util.Log.d("PreviewActivity", "failure: ${t.message}", t)
                    Toast.makeText(this@PreviewActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun parsePreviewUrlResponse(data: Any?): PreviewUrlResponse? {
        if (data == null) return null
        return try {
            val gson = Gson()
            val json = gson.toJson(data)
            android.util.Log.d("PreviewActivity", "data json: $json")
            val type = object : TypeToken<PreviewUrlResponse>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            android.util.Log.d("PreviewActivity", "parse error: ${e.message}", e)
            null
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
