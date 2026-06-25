package com.personax.mobile.ui.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.personax.mobile.data.MobileProfile
import com.personax.mobile.data.ProfileStore
import java.util.concurrent.Executor

class BrowserActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var proxySet = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profileId = intent.getStringExtra("profile_id") ?: run { finish(); return }
        val store = ProfileStore(this)
        val profile = store.getProfiles().find { it.id == profileId } ?: run { finish(); return }

        val container = FrameLayout(this)
        setContentView(container)

        if (profile.proxy.isNotEmpty()) {
            setWebViewProxy(profile.proxy) {
                setupWebView(container, profile)
            }
        } else {
            setupWebView(container, profile)
        }
    }

    private fun setWebViewProxy(proxyStr: String, onComplete: () -> Unit) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onComplete()
            return
        }

        val proxyUrl = buildProxyUrl(proxyStr)

        val proxyConfig = ProxyConfig.Builder()
            .addProxyRule(proxyUrl)
            .build()

        val executor = Executor { it.run() }

        ProxyController.getInstance().setProxyOverride(
            proxyConfig,
            executor
        ) {
            proxySet = true
            onComplete()
        }
    }

    private fun buildProxyUrl(proxyStr: String): String {
        val parts = proxyStr.split(":")
        return when {
            parts.size == 4 -> {
                val host = parts[0]
                val port = parts[1]
                val user = parts[2]
                val pass = parts[3]
                "$user:$pass@$host:$port"
            }
            parts.size == 2 -> proxyStr
            else -> proxyStr
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(container: FrameLayout, profile: MobileProfile) {
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
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

            @Suppress("DEPRECATION")
            settings.databasePath = filesDir.resolve("webdata_${profile.id}").absolutePath

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
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
                    val parts = profile.proxy.split(":")
                    if (parts.size == 4) {
                        handler?.proceed(parts[2], parts[3])
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
        if (proxySet && WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyController.getInstance().clearProxyOverride(
                Executor { it.run() }
            ) {}
        }
        webView?.destroy()
        super.onDestroy()
    }
}
