package com.fruitsplash.fruitsplashgame

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.webkit.*
import android.widget.*
import java.net.URI

class WebPageActivity : Activity() {
    private var web: WebView? = null
    private lateinit var root: LinearLayout
    private lateinit var progress: ProgressBar
    private val kind by lazy { intent.getStringExtra("kind") ?: "privacy" }
    private val url get() = if (kind == "support") "https://fruitsplassh.com/support.html" else "https://fruitsplassh.com/privacy-policy.html"
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(18,52,29)) }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(18,18,18,18) }
        bar.addView(Button(this).apply { text="Close"; setOnClickListener { finish() } })
        bar.addView(TextView(this).apply { text=if(kind=="support") "Support" else "Privacy Policy"; textSize=20f; setTextColor(Color.WHITE); gravity=17 }, LinearLayout.LayoutParams(0,-1,1f))
        root.addView(bar, LinearLayout.LayoutParams(-1,-2))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        root.addView(progress, LinearLayout.LayoutParams(-1,8))
        setContentView(root)
        load()
    }
    private fun online(): Boolean {
        val cm=getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n=cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(n)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true
    }
    private fun load() {
        web?.destroy(); web=null
        if (!online()) { fallback("You are offline. The game remains fully playable without a connection."); return }
        web=WebView(this).also { w ->
            w.setBackgroundColor(Color.WHITE)
            w.settings.apply {
                javaScriptEnabled = kind=="support"
                allowFileAccess=false; allowContentAccess=false; mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
                domStorageEnabled=kind=="support"; setSupportMultipleWindows(false)
            }
            w.webViewClient=object:WebViewClient() {
                override fun shouldOverrideUrlLoading(v:WebView, r:WebResourceRequest):Boolean {
                    val u=r.url
                    return u.scheme!="https" || u.host!="fruitsplassh.com"
                }
                override fun onReceivedError(v:WebView, r:WebResourceRequest, e:WebResourceError) {
                    if (r.isForMainFrame) fallback("The page could not be loaded. Please check your connection.")
                }
            }
            w.webChromeClient=object:WebChromeClient() {
                override fun onProgressChanged(v:WebView,p:Int){ progress.progress=p; progress.visibility=if(p<100) android.view.View.VISIBLE else android.view.View.GONE }
            }
            root.addView(w,LinearLayout.LayoutParams(-1,0,1f)); w.loadUrl(url)
        }
    }
    private fun fallback(message:String) {
        web?.let { root.removeView(it); it.destroy() }; web=null
        root.removeViews(2, root.childCount-2)
        root.addView(TextView(this).apply { text=message; textSize=19f; setTextColor(Color.WHITE); gravity=17; setPadding(32,32,32,32) },LinearLayout.LayoutParams(-1,0,1f))
        root.addView(Button(this).apply { text="Retry"; setOnClickListener { load() } })
    }
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if(web?.canGoBack()==true) web?.goBack() else super.onBackPressed() }
    override fun onDestroy(){ web?.apply{ stopLoading(); clearHistory(); removeAllViews(); destroy() }; web=null; super.onDestroy() }
}
