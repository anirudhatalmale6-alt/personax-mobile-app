package com.personax.mobile.ui.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.personax.mobile.data.MobileProfile
import com.personax.mobile.data.ProfileStore
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class BrowserActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var currentProfile: MobileProfile? = null
    private val API_BASE = "https://personax.work/mobile"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profileId = intent.getStringExtra("profile_id") ?: run { finish(); return }
        val store = ProfileStore(this)
        val profile = store.getProfiles().find { it.id == profileId } ?: run { finish(); return }
        currentProfile = profile

        val container = FrameLayout(this)
        setContentView(container)

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

            webChromeClient = WebChromeClient()
        }

        container.addView(webView)

        if (profile.proxy.isNotEmpty()) {
            loadViaProxy("https://www.google.com", profile)
        } else {
            webView?.apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.evaluateJavascript(buildSpoofScript(profile), null)
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(buildSpoofScript(profile), null)
                    }
                }
                loadUrl("https://www.google.com")
            }
        }
    }

    private fun loadViaProxy(url: String, profile: MobileProfile) {
        thread {
            try {
                val conn = URL("$API_BASE/proxy-fetch").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 20000
                conn.readTimeout = 20000

                val escapedUrl = url.replace("\\", "\\\\").replace("\"", "\\\"")
                val escapedProxy = profile.proxy.replace("\\", "\\\\").replace("\"", "\\\"")
                val escapedUa = profile.userAgent.replace("\\", "\\\\").replace("\"", "\\\"")
                val json = """{"url":"$escapedUrl","proxy":"$escapedProxy","user_agent":"$escapedUa"}"""

                OutputStreamWriter(conn.outputStream).use { it.write(json); it.flush() }

                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    val error = try { conn.errorStream?.bufferedReader()?.readText() } catch (e: Exception) { "HTTP $responseCode" }
                    conn.disconnect()
                    throw Exception("Server returned $responseCode: $error")
                }

                val html = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                runOnUiThread {
                    webView?.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val newUrl = request?.url?.toString() ?: return false
                            loadViaProxy(newUrl, profile)
                            return true
                        }
                    }
                    webView?.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    webView?.loadDataWithBaseURL(null,
                        "<html><body style='background:#0A0B10;color:#EF4444;font-family:sans-serif;padding:40px;text-align:center'>" +
                        "<h2>Proxy Connection Error</h2><p>${e.message?.replace("<","&lt;")}</p>" +
                        "<p style='color:#888;font-size:12px'>Proxy: ${profile.proxy}</p>" +
                        "<p style='color:#10B981;font-size:14px;margin-top:20px' onclick='window.location.reload()'>Tap to retry</p>" +
                        "</body></html>",
                        "text/html", "UTF-8", null)
                }
            }
        }
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
            if (window.RTCPeerConnection) {
                var origRTC = window.RTCPeerConnection;
                window.RTCPeerConnection = function(config) {
                    if (config && config.iceServers) config.iceServers = [];
                    return new origRTC(config);
                };
                window.RTCPeerConnection.prototype = origRTC.prototype;
            }
            console.log('[PersonaX] Fingerprint spoofing active: ${p.deviceModel}');
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
        webView?.destroy()
        super.onDestroy()
    }
}
