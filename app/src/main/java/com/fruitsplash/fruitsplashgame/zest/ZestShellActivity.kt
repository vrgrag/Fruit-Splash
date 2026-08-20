package com.fruitsplash.fruitsplashgame.zest

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.R
import com.fruitsplash.fruitsplashgame.zest.core.ZestIds
import com.fruitsplash.fruitsplashgame.zest.lock.ZestHref
import com.fruitsplash.fruitsplashgame.zest.vine.ZestChrome
import com.fruitsplash.fruitsplashgame.zest.vine.ZestKeep
import com.fruitsplash.fruitsplashgame.zest.vine.ZestPass
import com.fruitsplash.fruitsplashgame.zest.vine.ZestReach
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class ZestShellActivity : ComponentActivity() {

    private lateinit var container: FrameLayout
    private lateinit var web: WebView
    private lateinit var vault: ZestKeep
    private lateinit var link: ZestReach

    private var settledUrl: String? = null
    private var deepestHop: String? = null
    private var landingUrl: String? = null
    private var redirectRetries = 0
    private var entryPointRetried = false
    private var rendererRecoveries = 0
    private var loadFailed = false
    private var chainSettled = false

    private var cover: View? = null
    private var coverTimer: Job? = null

    @Volatile private var resumed = false
    @Volatile private var leftForOffline = false
    @Volatile private var offlineDeferred = false

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = filePathCallback ?: return@registerForActivityResult
        filePathCallback = null
        callback.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
                ?: emptyArray(),
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        allowCutoutArea()
        hideSystemBars()

        vault = ZestKeep(this)
        link = ZestReach(this)
        ZestPass.shellAlive = true

        container = FrameLayout(this)
        container.setBackgroundColor(Color.BLACK)
        container.fitsSystemWindows = false
        setContentView(container)
        installInsetRule()

        buildWebView()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = retreatTowardLanding()
            },
        )

        val initial = resolveInitialUrl()
        if (initial == null) {
            finish()
            return
        }
        raiseCover()
        web.loadUrl(initial)
        watchConnectivity()
        startHeartbeat()
        lifecycleScope.launch {
            delay(ZestIds.SAFE_AREA_DELAY_MS)
            injectFitSheet()
        }
    }

    private fun resolveInitialUrl(): String? {
        val fromIntent = intent.getStringExtra(EXTRA_TARGET_URL)
        val pushed = vault.takePushLink()
        return ZestHref.pick(pushed)
            ?: ZestHref.pick(fromIntent)
            ?: ZestHref.pick(vault.readCachedLink())
    }

    override fun onStart() {
        super.onStart()
        leftForOffline = false
        ZestPass.attach { url -> runOnUiThread { runCatching { web.loadUrl(url) } } }
        ZestPass.drain()?.let { url ->
            info("parked push URL delivered")
            runCatching { web.loadUrl(url) }
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        hideSystemBars()
        val deferred = offlineDeferred
        offlineDeferred = false
        lifecycleScope.launch {
            val alive = link.isReachable()
            when {
                !alive -> goOffline("offline on resume")
                deferred -> restoreAfterLoss()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    override fun onStop() {
        ZestPass.detach()
        super.onStop()
    }

    override fun onDestroy() {
        ZestPass.detach()
        ZestPass.shellAlive = false
        runCatching {
            web.stopLoading()
            container.removeView(web)
            web.destroy()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leftForOffline = false
        val pushed = ZestHref.pick(vault.takePushLink())
            ?: ZestHref.pick(intent.getStringExtra(EXTRA_TARGET_URL))
            ?: return
        val current = web.url
        if (current.isNullOrEmpty() || current == BLANK || current != pushed) {
            landingUrl = null
            web.loadUrl(pushed)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView() {
        val view = WebView(this)
        val s: WebSettings = view.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = false
        s.allowContentAccess = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.mediaPlaybackRequiresUserGesture = false
        s.textZoom = 100
        s.loadsImagesAutomatically = true
        s.blockNetworkImage = false
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.userAgentString = ZestChrome.value
        s.setSupportMultipleWindows(false)
        s.javaScriptCanOpenWindowsAutomatically = true

        view.setBackgroundColor(Color.BLACK)
        view.isHorizontalScrollBarEnabled = false
        view.isVerticalScrollBarEnabled = false
        view.webViewClient = pageClient
        view.webChromeClient = chromeClient
        web = view
        container.addView(
            view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(view, true)
    }

    private fun replaceWebView() {
        val resumeAt = settledUrl ?: deepestHop ?: vault.readCachedLink() ?: return
        val dead = web
        container.removeView(dead)
        runCatching { dead.destroy() }
        buildWebView()
        web.loadUrl(resumeAt)
    }

    private fun raiseCover() {
        coverTimer?.cancel()
        coverTimer = null
        cover?.let { existing ->
            existing.animate().cancel()
            existing.alpha = 1f
            return
        }
        val frame = FrameLayout(this)
        frame.setBackgroundColor(Color.BLACK)
        frame.isClickable = true
        val art = ImageView(this)
        art.setImageResource(coverArt())
        art.scaleType = ImageView.ScaleType.CENTER_CROP
        frame.addView(
            art,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        val spinner = ProgressBar(this)
        spinner.isIndeterminate = true
        spinner.indeterminateTintList = ColorStateList.valueOf(COVER_ACCENT)
        val spinnerParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        )
        spinnerParams.bottomMargin = (56f * resources.displayMetrics.density).toInt()
        frame.addView(spinner, spinnerParams)
        cover = frame
        coverHost().addView(
            frame,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        coverTimer = lifecycleScope.launch {
            delay(ZestIds.COVER_MAX_MS)
            if (cover === frame) dropCover(0L)
        }
    }

    private fun dropCover(after: Long = ZestIds.CHAIN_SETTLE_MS) {
        val current = cover ?: return
        coverTimer?.cancel()
        coverTimer = lifecycleScope.launch {
            delay(after)
            if (cover !== current) return@launch
            chainSettled = true
            pinLandingPane()
            cover = null
            current.animate().alpha(0f).setDuration(160L).withEndAction {
                coverHost().removeView(current)
            }.start()
        }
    }

    private fun coverHost(): FrameLayout = findViewById(android.R.id.content)

    private fun coverArt(): Int =
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            R.drawable.horizontal_loading_screen
        } else {
            R.drawable.vertical_loading_screen
        }

    private val pageClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
            val target = req.url?.toString() ?: return false
            val scheme = target.substringBefore(':').lowercase()
            if (req.isForMainFrame && req.hasGesture()) chainSettled = true
            return when {
                scheme in WEB_SCHEMES -> {
                    if (req.isForMainFrame) deepestHop = target
                    false
                }
                scheme == "intent" -> {
                    openIntentUri(target)
                    true
                }
                else -> {
                    openExternally(target)
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            loadFailed = false
            if (url != BLANK) deepestHop = url
            if (url != BLANK && !chainSettled) raiseCover()
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (loadFailed || url == BLANK) return
            redirectRetries = 0
            entryPointRetried = false
            settledUrl = url
            deepestHop = url
            CookieManager.getInstance().flush()
            injectFitSheet()
            dropCover()
        }

        override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
            if (!req.isForMainFrame) return
            loadFailed = true
            val code = err.errorCode
            val description = runCatching { err.description?.toString() }.getOrNull().orEmpty()
            info("main-frame error $code on ${req.url?.host}")
            if (code == ERROR_UNSUPPORTED_SCHEME) {
                dropCover(0L)
                return
            }
            val looping = code == ERROR_REDIRECT_LOOP ||
                code == ERROR_TOO_MANY_REQUESTS ||
                code == -1007 ||
                description.contains("too_many", ignoreCase = true)
            if (looping) {
                resumeChain(view, req.url?.toString().orEmpty())
                return
            }
            if (code in NETWORK_ERRORS || !link.hasAnyAdapter()) {
                view.stopLoading()
                view.loadUrl(BLANK)
                goOffline("main-frame network error $code")
                return
            }
            dropCover(0L)
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            if (isFinishing || view !== web) {
                runCatching { view.destroy() }
                return true
            }
            if (rendererRecoveries >= ZestIds.RENDERER_RECOVERY_MAX) {
                goOffline("renderer recovery budget exhausted")
                return true
            }
            rendererRecoveries++
            replaceWebView()
            return true
        }
    }

    private val chromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            if (newProgress < 100 || view.url == BLANK) return
            dropCover()
        }

        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams,
        ): Boolean {
            filePathCallback?.onReceiveValue(emptyArray())
            filePathCallback = callback
            return try {
                filePicker.launch(params.createIntent())
                true
            } catch (_: Throwable) {
                filePathCallback = null
                false
            }
        }
    }

    private fun resumeChain(view: WebView, failedUrl: String) {
        if (redirectRetries < ZestIds.REDIRECT_RETRY_MAX) {
            redirectRetries++
            info("redirect loop, resume $redirectRetries")
            queueLoad(view, deepestHop ?: failedUrl)
            return
        }
        val entryPoint = vault.readCachedLink()
        if (!entryPointRetried && !entryPoint.isNullOrEmpty() && entryPoint != deepestHop) {
            entryPointRetried = true
            queueLoad(view, entryPoint)
            return
        }
        dropCover(0L)
    }

    private fun queueLoad(view: WebView, url: String) {
        view.postDelayed({
            if (!isFinishing && !isDestroyed) view.loadUrl(url)
        }, 80L)
    }

    private fun openExternally(url: String) {
        val intent = runCatching {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.getOrNull() ?: return
        launchOrIgnore(intent)
    }

    private fun openIntentUri(url: String) {
        val parsed = runCatching {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        }.getOrNull() ?: return
        val fallback = parsed.getStringExtra("browser_fallback_url")
        parsed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        parsed.addCategory(Intent.CATEGORY_BROWSABLE)
        parsed.component = null
        parsed.selector = null
        if (launchOrIgnore(parsed)) return
        parsed.`package` = null
        if (launchOrIgnore(parsed)) return
        ZestHref.pick(fallback)?.let { web.loadUrl(it) }
    }

    private fun launchOrIgnore(intent: Intent): Boolean =
        runCatching { startActivity(intent) }.isSuccess

    private fun watchConnectivity() {
        lifecycleScope.launch {
            link.statusStream().collect { status ->
                if (status == ZestReach.Status.Offline) goOffline("default network lost")
            }
        }
    }

    private fun startHeartbeat() {
        lifecycleScope.launch {
            while (true) {
                delay(ZestIds.HEARTBEAT_MS)
                if (!resumed || leftForOffline) continue
                if (!link.isReachable()) goOffline("network unreachable")
            }
        }
    }

    private fun goOffline(why: String) {
        if (leftForOffline) return
        if (!resumed) {
            offlineDeferred = true
            info("offline while backgrounded ($why)")
            return
        }
        leftForOffline = true
        info("offline ($why)")
        val resumeAt = settledUrl ?: web.url
        runCatching {
            web.stopLoading()
            web.loadUrl(BLANK)
        }
        startActivity(
            Intent(this, ZestStillActivity::class.java).apply {
                if (!resumeAt.isNullOrEmpty() && resumeAt != BLANK) {
                    putExtra(ZestStillActivity.EXTRA_RESUME_URL, resumeAt)
                }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }

    private fun restoreAfterLoss() {
        val current = web.url
        if (!current.isNullOrEmpty() && current != BLANK) return
        val resumeAt = settledUrl ?: deepestHop ?: vault.readCachedLink() ?: return
        web.loadUrl(resumeAt)
    }

    private fun allowCutoutArea() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun hideSystemBars() {
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
        if (::container.isInitialized) container.requestApplyInsets()
    }

    private fun retreatTowardLanding() {
        if (!::web.isInitialized) return
        pinLandingPane()
        val home = landingUrl
        val current = web.url
        if (current.isNullOrEmpty() || current == BLANK) return
        if (home != null && samePane(current, home)) return
        val list = web.copyBackForwardList()
        val idx = list.currentIndex
        var homeIdx = -1
        if (home != null) {
            for (i in 0 until list.size) {
                val item = list.getItemAtIndex(i)?.url ?: continue
                if (samePane(item, home)) {
                    homeIdx = i
                    break
                }
            }
        }
        if (homeIdx >= 0 && idx > 0 && idx - 1 < homeIdx) {
            web.goBackOrForward(homeIdx - idx)
            return
        }
        if (web.canGoBack()) {
            web.goBack()
            return
        }
        if (home != null && !samePane(current, home)) web.loadUrl(home)
    }

    private fun pinLandingPane() {
        if (landingUrl != null) return
        if (!::web.isInitialized) return
        val now = settledUrl ?: web.url ?: return
        if (now.isEmpty() || now == BLANK) return
        val site = siteKey(now)
        val list = web.copyBackForwardList()
        var firstOnSite: String? = null
        for (i in 0 until list.size) {
            val item = list.getItemAtIndex(i)?.url ?: continue
            if (item == BLANK) continue
            if (siteKey(item) == site) {
                firstOnSite = item
                break
            }
        }
        landingUrl = firstOnSite ?: now
    }

    private fun siteKey(url: String): String {
        val host = runCatching { Uri.parse(url).host }.getOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?: return url
        val parts = host.split('.')
        return if (parts.size >= 2) parts.takeLast(2).joinToString(".") else host
    }

    private fun samePane(a: String, b: String): Boolean {
        fun strip(raw: String): String {
            var s = raw
            val hash = s.indexOf('#')
            if (hash >= 0) s = s.substring(0, hash)
            if (s.endsWith('/') && s.count { it == '/' } > 2) s = s.dropLast(1)
            return s
        }
        return strip(a) == strip(b)
    }

    private fun installInsetRule() {
        container.setOnApplyWindowInsetsListener { view, insets ->
            val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) insets.displayCutout else null
            view.setPadding(
                cutout?.safeInsetLeft ?: 0,
                cutout?.safeInsetTop ?: 0,
                cutout?.safeInsetRight ?: 0,
                cutout?.safeInsetBottom ?: 0,
            )
            val ime = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.ime()).bottom
            } else {
                0
            }
            if (ime > 0) panTowardFocus(ime) else if (::web.isInitialized) web.translationY = 0f
            insets
        }
        container.requestApplyInsets()
    }

    /** One-shot focus query, then pan. No scrollIntoView listener. */
    private fun panTowardFocus(imePx: Int) {
        if (!::web.isInitialized) return
        val js = """(function(){
          var el=document.activeElement;
          try{
            while(el&&el.tagName==='IFRAME'){
              var d=el.contentDocument; if(!d) break; el=d.activeElement;
            }
          }catch(e){}
          if(!el) return '{"t":0,"h":0}';
          var r=el.getBoundingClientRect();
          return JSON.stringify({t:r.top,h:r.height});
        })();"""
        runCatching {
            web.evaluateJavascript(js) { raw ->
                val json = runCatching {
                    JSONObject(raw?.trim()?.trim('"')?.replace("\\\"", "\"") ?: "{}")
                }.getOrNull()
                val top = json?.optDouble("t", 0.0) ?: 0.0
                val h = json?.optDouble("h", 0.0) ?: 0.0
                val fieldBottom = (top + h).toFloat()
                val visBottom = web.height - imePx
                val need = fieldBottom - visBottom + (18f * resources.displayMetrics.density)
                web.translationY = if (need > 0f) -need else 0f
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideSystemBars()
        container.requestApplyInsets()
        ((cover as? FrameLayout)?.getChildAt(0) as? ImageView)?.setImageResource(coverArt())
    }

    /**
     * Unique fit sheet: CSS env() vars only. Never touches
     * padding/margin on html, body or #app.
     */
    private fun injectFitSheet() {
        val js = """(function(){
          if(window.__orchFitBound) return; window.__orchFitBound=1;
          var TAG='orch-fit-sheet';
          var RULE=':root{--sat:0px;--sar:0px;--sab:0px;--sal:0px;}';
          function paint(){
            var h=document.head||document.documentElement; if(!h) return;
            var m=document.querySelector('meta[name="viewport"]');
            if(!m){
              m=document.createElement('meta');
              m.setAttribute('name','viewport');
              m.setAttribute('content','width=device-width, initial-scale=1, viewport-fit=contain');
              h.appendChild(m);
            }
            var s=document.getElementById(TAG);
            if(!s){ s=document.createElement('style'); s.id=TAG; h.appendChild(s); }
            if(s.textContent!==RULE) s.textContent=RULE;
          }
          paint();
          document.addEventListener('DOMContentLoaded', paint);
        })();"""
        runCatching { web.evaluateJavascript(js, null) }
    }

    private fun info(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    companion object {
        const val EXTRA_TARGET_URL = "zest_target_url"
        private const val TAG = "ZestShell"
        private const val BLANK = "about:blank"
        private const val COVER_ACCENT = 0xFFFFC107.toInt()
        private val WEB_SCHEMES = setOf("http", "https", "about", "data", "blob", "file", "javascript")
        private val NETWORK_ERRORS = setOf(-2, -6, -7, -8, -11)
    }
}
