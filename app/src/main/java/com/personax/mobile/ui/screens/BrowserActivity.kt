package com.personax.mobile.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.personax.mobile.data.MobileProfile
import com.personax.mobile.data.ProfileStore
import java.util.concurrent.Executor

class BrowserActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var proxySet = false
    private var urlBar: EditText? = null
    private var proxyUser: String = ""
    private var proxyPass: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profileId = intent.getStringExtra("profile_id") ?: run { finish(); return }
        val store = ProfileStore(this)
        val profile = store.getProfiles().find { it.id == profileId } ?: run { finish(); return }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0B10.toInt())
        }
        setContentView(root)

        root.addView(buildToolbar(profile))

        if (profile.proxy.isNotEmpty()) {
            try {
                setWebViewProxy(profile.proxy) {
                    runOnUiThread { setupWebView(root, profile) }
                }
            } catch (e: Exception) {
                setupWebView(root, profile)
            }
        } else {
            setupWebView(root, profile)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun buildToolbar(profile: MobileProfile): LinearLayout {
        val dp = resources.displayMetrics.density

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF12131A.toInt())
            setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (6 * dp).toInt())
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val btnStyle = { btn: ImageButton ->
            btn.setBackgroundColor(0xFF1A1B24.toInt())
            btn.setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
            btn.layoutParams = LinearLayout.LayoutParams((36 * dp).toInt(), (36 * dp).toInt()).apply {
                marginEnd = (4 * dp).toInt()
            }
            btn.setColorFilter(0xFFE1E4E8.toInt())
            btn.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val backBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            setOnClickListener { if (webView?.canGoBack() == true) webView?.goBack() }
            btnStyle(this)
        }

        val homeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            setOnClickListener { finish() }
            btnStyle(this)
        }

        val refreshBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_rotate)
            setOnClickListener { webView?.reload() }
            btnStyle(this)
        }

        urlBar = EditText(this).apply {
            setText("https://www.google.com")
            setTextColor(0xFFE1E4E8.toInt())
            setHintTextColor(0xFF71717A.toInt())
            textSize = 13f
            isSingleLine = true
            setBackgroundColor(0xFF1A1B24.toInt())
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt())
            imeOptions = EditorInfo.IME_ACTION_GO
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (4 * dp).toInt()
            }
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    var url = text.toString().trim()
                    if (!url.startsWith("http")) url = "https://$url"
                    webView?.loadUrl(url)
                    true
                } else false
            }
        }

        val badge = TextView(this).apply {
            text = profile.deviceModel.split(" ").first().take(3).uppercase()
            setTextColor(0xFF10B981.toInt())
            textSize = 10f
            setBackgroundColor(0x2610B981)
            setPadding((6 * dp).toInt(), (4 * dp).toInt(), (6 * dp).toInt(), (4 * dp).toInt())
            gravity = Gravity.CENTER
        }

        toolbar.addView(homeBtn)
        toolbar.addView(backBtn)
        toolbar.addView(refreshBtn)
        toolbar.addView(urlBar)
        toolbar.addView(badge)

        return toolbar
    }

    private fun setWebViewProxy(proxyStr: String, onComplete: () -> Unit) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onComplete()
            return
        }

        val parts = proxyStr.split(":")
        val proxyHost: String
        val proxyPort: String

        when {
            parts.size == 4 -> {
                proxyHost = parts[0]
                proxyPort = parts[1]
                proxyUser = parts[2]
                proxyPass = parts[3]
            }
            parts.size == 2 -> {
                proxyHost = parts[0]
                proxyPort = parts[1]
            }
            else -> {
                onComplete()
                return
            }
        }

        try {
            val proxyConfig = ProxyConfig.Builder()
                .addProxyRule("$proxyHost:$proxyPort")
                .build()

            ProxyController.getInstance().setProxyOverride(
                proxyConfig,
                Executor { it.run() }
            ) {
                proxySet = true
                onComplete()
            }
        } catch (e: Exception) {
            onComplete()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(container: LinearLayout, profile: MobileProfile) {
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f
            )

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.allowContentAccess = true
            settings.allowFileAccess = false
            settings.userAgentString = profile.userAgent
            settings.mediaPlaybackRequiresUserGesture = false
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            @Suppress("DEPRECATION")
            settings.databasePath = filesDir.resolve("webdata_${profile.id}").absolutePath

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    urlBar?.setText(url ?: "")
                    view?.evaluateJavascript(buildSpoofScript(profile), null)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(buildSpoofScript(profile), null)
                }

                override fun onReceivedHttpAuthRequest(
                    view: WebView?, handler: HttpAuthHandler?,
                    host: String?, realm: String?
                ) {
                    if (proxyUser.isNotEmpty()) {
                        handler?.proceed(proxyUser, proxyPass)
                    } else {
                        super.onReceivedHttpAuthRequest(view, handler, host, realm)
                    }
                }
            }

            webChromeClient = WebChromeClient()

            loadUrl("https://www.google.com")
        }

        container.addView(webView)
    }

    private fun buildSpoofScript(p: MobileProfile): String {
        val resParts = p.resolution.split("x")
        val w = resParts.getOrNull(0)?.toIntOrNull() ?: 1080
        val h = resParts.getOrNull(1)?.toIntOrNull() ?: 2400

        return """
        (function() {
            if (window.__pxSpoofed) return;
            window.__pxSpoofed = true;

            Object.defineProperty(screen, 'width', {get: function(){return $w}});
            Object.defineProperty(screen, 'height', {get: function(){return $h}});
            Object.defineProperty(screen, 'availWidth', {get: function(){return $w}});
            Object.defineProperty(screen, 'availHeight', {get: function(){return $h}});
            Object.defineProperty(screen, 'colorDepth', {get: function(){return 24}});
            Object.defineProperty(screen, 'pixelDepth', {get: function(){return 24}});
            Object.defineProperty(window, 'devicePixelRatio', {get: function(){return ${p.dpi / 160.0}}});

            Object.defineProperty(navigator, 'platform', {get: function(){return 'Linux armv8l'}});
            Object.defineProperty(navigator, 'hardwareConcurrency', {get: function(){return ${(2..8).random()}}});
            Object.defineProperty(navigator, 'maxTouchPoints', {get: function(){return 5}});
            Object.defineProperty(navigator, 'deviceMemory', {get: function(){return ${listOf(4, 6, 8).random()}}});
            Object.defineProperty(navigator, 'webdriver', {get: function(){return false}});

            var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
            HTMLCanvasElement.prototype.toDataURL = function(type) {
                var ctx = this.getContext('2d');
                if (ctx) {
                    var imgData = ctx.getImageData(0, 0, Math.min(this.width, 16), Math.min(this.height, 16));
                    for (var i = 0; i < imgData.data.length; i += 4) {
                        imgData.data[i] = imgData.data[i] ^ ${(1..3).random()};
                    }
                    ctx.putImageData(imgData, 0, 0);
                }
                return origToDataURL.apply(this, arguments);
            };

            var origGetParam = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = function(param) {
                if (param === 37445) return 'Qualcomm';
                if (param === 37446) return 'Adreno (TM) ${listOf("730", "740", "650", "660").random()}';
                return origGetParam.apply(this, arguments);
            };

            if (window.AudioContext) {
                var origCreateOsc = AudioContext.prototype.createOscillator;
                AudioContext.prototype.createOscillator = function() {
                    var osc = origCreateOsc.apply(this, arguments);
                    osc.__pxNoise = ${String.format("%.10f", Math.random() * 0.0001)};
                    return osc;
                };
            }

            if (navigator.getBattery) {
                navigator.getBattery = function() {
                    return Promise.resolve({
                        charging: ${listOf("true", "false").random()},
                        chargingTime: Infinity,
                        dischargingTime: ${(3600..18000).random()},
                        level: ${String.format("%.2f", 0.3 + Math.random() * 0.6)},
                        addEventListener: function(){},
                        removeEventListener: function(){}
                    });
                };
            }

            if (window.RTCPeerConnection) {
                var origRTC = window.RTCPeerConnection;
                window.RTCPeerConnection = function(config) {
                    if (config && config.iceServers) config.iceServers = [];
                    return new origRTC(config);
                };
                window.RTCPeerConnection.prototype = origRTC.prototype;
            }

            delete navigator.__proto__.webdriver;

            console.log('[PersonaX] Fingerprint active: ${p.deviceModel}');
        })();
        """.trimIndent()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        try {
            if (proxySet && WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride(
                    Executor { it.run() }
                ) {}
            }
        } catch (_: Exception) {}
        webView?.destroy()
        super.onDestroy()
    }
}
