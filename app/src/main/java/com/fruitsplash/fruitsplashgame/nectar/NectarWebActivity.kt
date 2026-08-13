package com.fruitsplash.fruitsplashgame.nectar

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.fruitsplash.fruitsplashgame.BuildConfig
import com.fruitsplash.fruitsplashgame.nectar.box.NectarBox
import com.fruitsplash.fruitsplashgame.nectar.box.NectarWarm
import com.fruitsplash.fruitsplashgame.nectar.face.NectarFill
import com.fruitsplash.fruitsplashgame.nectar.gate.NectarUrl
import com.fruitsplash.fruitsplashgame.nectar.id.NectarBrand
import com.fruitsplash.fruitsplashgame.nectar.net.NectarChrome
import com.fruitsplash.fruitsplashgame.nectar.net.NectarReach
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NectarWebActivity : ComponentActivity() {

    private lateinit var holder: FrameLayout
    private lateinit var web: WebView
    private lateinit var vault: NectarBox
    private lateinit var link: NectarReach

    private var settledUrl: String? = null
    private var deepestHop: String? = null
    private var landingUrl: String? = null
    private var redirectRetries = 0
    private var entryPointRetried = false
    private var rendererRecoveries = 0
    private var loadFailed = false
    private var retryPending = false
    private var chainSettled = false
    private var chainHops = 0
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        allowCutoutArea()

        vault = NectarBox(this)
        link = NectarReach(this)
        NectarWarm.shellAlive = true

        holder = FrameLayout(this)
        holder.setBackgroundColor(Color.BLACK)
        holder.fitsSystemWindows = false
        setContentView(holder)
        hideStatusChrome()
        installInsetRule()
        buildWebView()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    retreatTowardLanding()
                }
            },
        )

        val initial = resolveInitialUrl()
        if (initial == null) {
            info("no URL to load, finishing")
            finish()
            return
        }
        raiseCover()
        web.loadUrl(initial)
        watchConnectivity()
        startHeartbeat()
        lifecycleScope.launch {
            delay(NectarBrand.SAFE_AREA_DELAY_MS)
            injectSafeAreaKill()
        }
    }

    private fun resolveInitialUrl(): String? {
        val fromIntent = intent.getStringExtra(EXTRA_TARGET_URL)
        val pushed = vault.takePushLink()
        return NectarUrl.clean(pushed)
            ?: NectarUrl.clean(fromIntent)
            ?: NectarUrl.clean(vault.readCachedLink())
    }

    override fun onStart() {
        super.onStart()
        leftForOffline = false
        NectarWarm.attach { url ->
            runOnUiThread { runCatching { web.loadUrl(url) } }
        }
        NectarWarm.drain()?.let { url ->
            info("parked push URL delivered")
            runCatching { web.loadUrl(url) }
        }
        maybeOfferInvite()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        hideStatusChrome()
        val deferred = offlineDeferred
        offlineDeferred = false
        lifecycleScope.launch {
            val alive = link.canTalk()
            when {
                !alive -> goOffline("offline on resume")
                deferred -> restoreAfterLoss()
            }
        }
    }

    override fun onPause() {
        resumed = false
        super.onPause()
    }

    override fun onStop() {
        NectarWarm.detach()
        super.onStop()
    }

    override fun onDestroy() {
        NectarWarm.detach()
        NectarWarm.shellAlive = false
        runCatching {
            web.stopLoading()
            holder.removeView(web)
            web.destroy()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        leftForOffline = false
        val pushed = NectarUrl.clean(vault.takePushLink())
            ?: NectarUrl.clean(intent.getStringExtra(EXTRA_TARGET_URL))
            ?: return
        val current = web.url
        if (current.isNullOrEmpty() || current == BLANK || current != pushed) {
            info("new intent, loading target")
            landingUrl = null
            chainSettled = false
            web.loadUrl(pushed)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView() {
        val view = WebView(this)
        val s: WebSettings = view.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = true
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
        s.userAgentString = NectarChrome.value
        s.setSupportMultipleWindows(false)
        s.javaScriptCanOpenWindowsAutomatically = true
        view.setBackgroundColor(Color.BLACK)
        view.isHorizontalScrollBarEnabled = false
        view.isVerticalScrollBarEnabled = false
        view.webViewClient = pageClient
        view.webChromeClient = chromeClient
        web = view
        holder.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val cookies = CookieManager.getInstance()
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(view, true)
    }

    private fun replaceWebView() {
        val resumeAt = settledUrl ?: deepestHop ?: vault.readCachedLink() ?: return
        val dead = web
        holder.removeView(dead)
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
        cover = frame
        coverHost().addView(
            frame,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        coverTimer = lifecycleScope.launch {
            delay(NectarBrand.COVER_MAX_MS)
            if (cover !== frame) return@launch
            if (retryPending || loadFailed) {
                info("loading cover timed out during recovery")
                return@launch
            }
            info("loading cover timed out")
            dropCover(0L)
        }
    }

    private fun dropCover(after: Long = NectarBrand.CHAIN_SETTLE_MS) {
        if (loadFailed || retryPending) return
        val current = cover ?: return
        coverTimer?.cancel()
        coverTimer = lifecycleScope.launch {
            delay(after)
            if (cover !== current) return@launch
            if (!chainSettled && !loadFailed) pinLandingPane(web)
            chainSettled = true
            cover = null
            current.animate().alpha(0f).setDuration(160L).withEndAction {
                coverHost().removeView(current)
            }.start()
        }
    }

    private fun coverHost(): FrameLayout = findViewById(android.R.id.content)

    private val pageClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
            val target = req.url?.toString() ?: return false
            val scheme = target.substringBefore(':').lowercase()
            if (req.isForMainFrame && req.hasGesture()) chainSettled = true
            return when {
                scheme in WEB_SCHEMES -> {
                    val upgraded = NectarUrl.clean(target)
                    if (req.isForMainFrame && upgraded != null && upgraded != target) {
                        info("cleartext hop upgraded to https")
                        deepestHop = upgraded
                        view.post { if (!isFinishing && !isDestroyed) view.loadUrl(upgraded) }
                        true
                    } else {
                        if (req.isForMainFrame) deepestHop = target
                        false
                    }
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
            if (url == BLANK) return
            if (isErrorDocument(url)) {
                loadFailed = true
                if (!chainSettled) raiseCover()
                return
            }
            loadFailed = false
            retryPending = false
            deepestHop = url
            if (!chainSettled) {
                chainHops++
                raiseCover()
                if (chainHops >= NectarBrand.CHAIN_HOP_SOFT_CAP) {
                    info("hop cap $chainHops, resume before chromium loop")
                    retryPending = true
                    loadFailed = true
                    view.stopLoading()
                    resumeChain(view, url)
                }
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (loadFailed || retryPending || url == BLANK) return
            if (isErrorDocument(url)) {
                loadFailed = true
                resumeChain(view, url)
                return
            }
            redirectRetries = 0
            entryPointRetried = false
            chainHops = 0
            settledUrl = url
            deepestHop = url
            if (!chainSettled) pinLandingPane(view)
            CookieManager.getInstance().flush()
            injectSafeAreaKill()
            injectKeyboardScrollFix()
            dropCover()
        }

        override fun onReceivedError(
            view: WebView,
            req: WebResourceRequest,
            err: WebResourceError,
        ) {
            if (!req.isForMainFrame) return
            if (retryPending) return
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
                description.contains("too_many", ignoreCase = true) ||
                description.contains("too many redirects", ignoreCase = true)
            if (looping) {
                resumeChain(view, req.url?.toString().orEmpty())
                return
            }
            if (code in NETWORK_ERRORS || !link.hasRadio()) {
                view.stopLoading()
                view.loadUrl(BLANK)
                goOffline("main-frame network error $code")
                return
            }
            dropCover(0L)
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            info("render process gone, crashed=${detail.didCrash()}")
            if (isFinishing || view !== web) {
                runCatching { view.destroy() }
                return true
            }
            if (rendererRecoveries >= NectarBrand.RENDERER_RECOVERY_MAX) {
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
            if (loadFailed || retryPending || !chainSettled) return
            dropCover()
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            request.grant(request.resources)
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
        if (redirectRetries < NectarBrand.REDIRECT_RETRY_MAX) {
            redirectRetries++
            info("redirect loop, resume attempt $redirectRetries")
            queueLoad(view, deepestHop ?: failedUrl)
            return
        }
        val entryPoint = vault.readCachedLink()
        if (!entryPointRetried && !entryPoint.isNullOrEmpty() && entryPoint != deepestHop) {
            entryPointRetried = true
            info("redirect budget spent, retrying entry")
            queueLoad(view, entryPoint)
            return
        }
        info("redirect chain unresolvable, keeping cover off the error page")
        retryPending = false
        view.stopLoading()
        raiseCover()
    }

    private fun queueLoad(view: WebView, url: String) {
        retryPending = true
        chainHops = 0
        raiseCover()
        view.stopLoading()
        view.postDelayed({
            if (!isFinishing && !isDestroyed) view.loadUrl(url)
        }, RETRY_PAUSE_MS)
    }

    private fun pinLandingPane(view: WebView) {
        val current = view.url ?: return
        val siteDomain = registeredDomain(current) ?: return
        val list = view.copyBackForwardList()
        for (i in 0 until list.size) {
            val candidate = list.getItemAtIndex(i)?.url ?: continue
            if (registeredDomain(candidate) == siteDomain) {
                landingUrl = candidate
                info("landing pinned: $candidate")
                return
            }
        }
        landingUrl = current
        info("landing pinned (current): $current")
    }

    private fun retreatTowardLanding() {
        if (!::web.isInitialized) return
        val home = landingUrl
        val current = web.url
        if (current.isNullOrEmpty() || current == BLANK) return
        if (home != null && samePage(current, home)) return

        val list = web.copyBackForwardList()
        val idx = list.currentIndex
        var homeIdx = -1
        if (home != null) {
            for (i in 0 until list.size) {
                val item = list.getItemAtIndex(i)?.url ?: continue
                if (samePage(item, home)) {
                    homeIdx = i
                    break
                }
            }
        }
        when {
            homeIdx >= 0 -> web.goBackOrForward(homeIdx - idx)
            home != null -> web.loadUrl(home)
            web.canGoBack() -> web.goBack()
        }
    }

    private fun registeredDomain(url: String): String? {
        val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return null
        val stripped = host.trimEnd('.').lowercase().removePrefix("www.")
        val parts = stripped.split('.')
        return if (parts.size >= 2) parts.takeLast(2).joinToString(".") else stripped
    }

    private fun samePage(a: String, b: String): Boolean {
        val cutA = a.indexOf('#').let { if (it >= 0) a.substring(0, it) else a }
        val cutB = b.indexOf('#').let { if (it >= 0) b.substring(0, it) else b }
        return cutA == cutB
    }

    private fun isErrorDocument(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("chrome-error:") ||
            lower.startsWith("about:neterror") ||
            lower.contains("chromewebdata") ||
            lower.contains("err_too_many_redirects")
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
        NectarUrl.clean(fallback)?.let { web.loadUrl(it) }
    }

    private fun launchOrIgnore(intent: Intent): Boolean =
        runCatching { startActivity(intent) }.isSuccess

    private fun watchConnectivity() {
        lifecycleScope.launch {
            link.live().collect { status ->
                if (status == NectarReach.Kind.Down) goOffline("default network lost")
            }
        }
    }

    private fun startHeartbeat() {
        lifecycleScope.launch {
            while (true) {
                delay(NectarBrand.HEARTBEAT_MS)
                if (!resumed || leftForOffline) continue
                if (!link.canTalk()) goOffline("network unreachable")
            }
        }
    }

    private fun goOffline(why: String) {
        if (leftForOffline) return
        if (!resumed) {
            offlineDeferred = true
            info("offline while backgrounded ($why), deferred")
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
            Intent(this, NectarOfflineActivity::class.java).apply {
                if (!resumeAt.isNullOrEmpty() && resumeAt != BLANK) {
                    putExtra(NectarOfflineActivity.EXTRA_RESUME_URL, resumeAt)
                }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
    }

    private fun maybeOfferInvite() {
        if (isFinishing || isDestroyed) return
        if (!vault.shouldOfferInvite(this)) return
        val target = NectarUrl.clean(settledUrl)
            ?: NectarUrl.clean(web.url)
            ?: NectarUrl.clean(vault.readCachedLink())
            ?: return
        info("invite cooldown elapsed, showing prompt")
        startActivity(
            Intent(this, NectarInviteActivity::class.java)
                .putExtra(NectarInviteActivity.EXTRA_TARGET_URL, target),
        )
    }

    private fun restoreAfterLoss() {
        val current = web.url
        if (!current.isNullOrEmpty() && current != BLANK) return
        val resumeAt = settledUrl ?: deepestHop ?: vault.readCachedLink() ?: return
        info("link back after a loss, reloading")
        web.loadUrl(resumeAt)
    }

    private fun hideStatusChrome() {
        NectarFill.apply(this)
    }

    private fun allowCutoutArea() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun installInsetRule() {
        holder.setOnApplyWindowInsetsListener { view, insets ->
            val safe = safeArea(insets)
            view.setPadding(safe[0], safe[1], safe[2], safe[3])
            insets
        }
        holder.requestApplyInsets()
    }

    /**
     * Hidden bars report 0 via [WindowInsets.getInsets]. Use
     * [WindowInsets.getInsetsIgnoringVisibility] so the WebView still
     * clears the camera hole and the gesture/nav strip.
     */
    private fun safeArea(insets: WindowInsets): IntArray {
        val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            insets.displayCutout
        } else {
            null
        }
        val cutLeft = cutout?.safeInsetLeft ?: 0
        val cutTop = cutout?.safeInsetTop ?: 0
        val cutRight = cutout?.safeInsetRight ?: 0
        val cutBottom = cutout?.safeInsetBottom ?: 0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            return intArrayOf(
                maxOf(cutLeft, insets.systemWindowInsetLeft),
                maxOf(cutTop, insets.systemWindowInsetTop),
                maxOf(cutRight, insets.systemWindowInsetRight),
                maxOf(cutBottom, insets.systemWindowInsetBottom),
            )
        }
        val chrome = insets.getInsetsIgnoringVisibility(
            WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout(),
        )
        val nav = insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
        return intArrayOf(
            maxOf(cutLeft, chrome.left, nav.left),
            maxOf(cutTop, chrome.top),
            maxOf(cutRight, chrome.right, nav.right),
            maxOf(cutBottom, nav.bottom),
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideStatusChrome()
        holder.requestApplyInsets()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusChrome()
    }

    private fun injectSafeAreaKill() {
        val js = """(function () {
          if (window.__fsNectarSa) return; window.__fsNectarSa = true;
          var ID = '__fs_nectar_sa';
          var CSS = ':root{' +
              '--safe-area-inset-top:0px!important;' +
              '--safe-area-inset-right:0px!important;' +
              '--safe-area-inset-bottom:0px!important;' +
              '--safe-area-inset-left:0px!important;' +
              '--sat:0px!important;--sar:0px!important;' +
              '--sab:0px!important;--sal:0px!important;' +
              '--safe-top:0px!important;--safe-bottom:0px!important;' +
              '--safe-left:0px!important;--safe-right:0px!important;' +
            '}' +
            '.gameview-mobile-header,.app-header,.js-safe-top{' +
              'padding-top:0!important;margin-top:0!important;' +
            '}';
          function kbOpen(){
            if (!window.visualViewport) return false;
            return window.visualViewport.height < window.innerHeight * 0.75;
          }
          function apply(){
            if (kbOpen()) return;
            var head = document.head || document.documentElement; if (!head) return;
            var m = document.querySelector('meta[name="viewport"]');
            if (!m) {
              m = document.createElement('meta');
              m.setAttribute('name','viewport');
              m.setAttribute('content','width=device-width, initial-scale=1, viewport-fit=contain');
              head.appendChild(m);
            } else if (!/viewport-fit\s*=\s*contain/i.test(m.getAttribute('content')||'')) {
              var c = (m.getAttribute('content')||'').replace(/,?\s*viewport-fit\s*=\s*\w+/ig,'').trim();
              m.setAttribute('content', c + (c ? ', ' : '') + 'viewport-fit=contain');
            }
            var s = document.getElementById(ID);
            if (!s) { s = document.createElement('style'); s.id = ID; head.appendChild(s); }
            if (s.textContent !== CSS) s.textContent = CSS;
            if (head.lastElementChild !== s) head.appendChild(s);
          }
          apply();
          ['pushState','replaceState'].forEach(function (fn) {
            var o = history[fn]; history[fn] = function () {
              var r = o.apply(this, arguments);
              setTimeout(apply, 80); setTimeout(apply, 400); return r;
            };
          });
          window.addEventListener('popstate', function(){ setTimeout(apply, 80); });
          setInterval(apply, 2500);
        })();""".trimIndent()
        runCatching { web.evaluateJavascript(js, null) }
    }

    private fun injectKeyboardScrollFix() {
        val js = """(function(){
          if (window.__fsNectarKb) return; window.__fsNectarKb = true;
          document.addEventListener('focusin', function(e){
            var el = e.target;
            if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA')) return;
            setTimeout(function(){
              try { el.scrollIntoView({behavior:'auto',block:'center',inline:'nearest'}); } catch(_){}
            }, 350);
          });
        })();""".trimIndent()
        runCatching { web.evaluateJavascript(js, null) }
    }

    private fun info(message: String) {
        if (BuildConfig.DEBUG) Log.i(TAG, message)
    }

    companion object {
        const val EXTRA_TARGET_URL = "nectar_web_dest"
        private const val TAG = "NectarWeb"
        private const val BLANK = "about:blank"
        private const val RETRY_PAUSE_MS = 70L
        private val WEB_SCHEMES =
            setOf("http", "https", "about", "data", "blob", "file", "javascript")
        private val NETWORK_ERRORS = setOf(-2, -6, -7, -8, -11)
    }
}
