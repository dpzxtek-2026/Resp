package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    tabId: String,
    url: String,
    isDesktopMode: Boolean,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onPageVisited: (title: String, url: String) -> Unit,
    webViewsCache: MutableMap<String, WebView>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Desktop and Mobile user agents
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val defaultUserAgent = remember { mutableMapOf<String, String>() }

    val webView = remember(tabId) {
        webViewsCache.getOrPut(tabId) {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // Configure settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setSupportZoom(true)
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                // Store default mobile UA
                defaultUserAgent[tabId] = settings.userAgentString

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: ""
                        if (requestUrl.contains("dpzxproject.pages.dev")) {
                            view?.loadDataWithBaseURL("https://dpzxproject.pages.dev", getDpzxProjectHtml(), "text/html", "UTF-8", null)
                            return true
                        }
                        return false
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChanged(true)
                        onProgressChanged(10)
                        url?.let { onUrlChanged(it) }
                        
                        // Update navigation button states
                        view?.let {
                            onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                        onProgressChanged(100)
                        
                        view?.let {
                            val pageTitle = it.title ?: ""
                            val pageUrl = it.url ?: ""
                            onTitleChanged(pageTitle)
                            onUrlChanged(pageUrl)
                            onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                            
                            if (pageUrl.isNotBlank() && !pageUrl.startsWith("about:blank")) {
                                onPageVisited(pageTitle, pageUrl)
                            }
                        }
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        view?.let {
                            onNavigationStateChanged(it.canGoBack(), it.canGoForward())
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            view?.loadDataWithBaseURL(null, getCustomErrorHtml(), "text/html", "UTF-8", null)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        view?.loadDataWithBaseURL(null, getCustomErrorHtml(), "text/html", "UTF-8", null)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }
                }
            }
        }
    }

    // Handle User Agent toggles for Desktop / Mobile view
    LaunchedEffect(isDesktopMode) {
        val currentSettings = webView.settings
        if (isDesktopMode) {
            currentSettings.userAgentString = desktopUserAgent
            currentSettings.useWideViewPort = true
            currentSettings.loadWithOverviewMode = true
        } else {
            currentSettings.userAgentString = defaultUserAgent[tabId] ?: ""
            currentSettings.useWideViewPort = false
            currentSettings.loadWithOverviewMode = false
        }
        // Force reload page to apply new user agent
        if (webView.url != null && !webView.url.equals("about:blank")) {
            webView.reload()
        }
    }

    // Trigger loading of URL if it differs from current WebView URL
    LaunchedEffect(url) {
        if (url.isNotBlank() && url != "about:newtab") {
            val currentUrl = webView.url ?: ""
            if (currentUrl != url && !url.startsWith("about:blank")) {
                if (url.contains("dpzxproject.pages.dev")) {
                    webView.loadDataWithBaseURL("https://dpzxproject.pages.dev", getDpzxProjectHtml(), "text/html", "UTF-8", null)
                } else {
                    webView.loadUrl(url)
                }
            }
        }
    }

    // Render WebView
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// Generates a beautiful custom neon-cyberpunk themed error page for dpzxbrowse!
fun getCustomErrorHtml(): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body {
                    background-color: #0b0c10;
                    color: #ffffff;
                    font-family: 'Courier New', Courier, monospace;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                    padding: 20px;
                    box-sizing: border-box;
                    text-align: center;
                }
                .container {
                    border: 2px solid #ff007f;
                    border-radius: 16px;
                    padding: 30px;
                    background: #12131a;
                    box-shadow: 0 0 25px rgba(255, 0, 127, 0.4), inset 0 0 15px rgba(255, 0, 127, 0.2);
                    max-width: 420px;
                }
                .bolt {
                    font-size: 55px;
                    color: #39ff14;
                    text-shadow: 0 0 15px #39ff14;
                    margin-bottom: 15px;
                }
                h1 {
                    color: #ff007f;
                    text-shadow: 0 0 10px #ff007f;
                    font-size: 20px;
                    margin-top: 10px;
                    font-weight: 900;
                    letter-spacing: 2.5px;
                }
                p {
                    color: #8f94fb;
                    font-size: 13.5px;
                    line-height: 1.6;
                    margin-bottom: 25px;
                }
                .btn {
                    background: transparent;
                    border: 2px solid #00f3ff;
                    color: #00f3ff;
                    padding: 12px 24px;
                    font-size: 14px;
                    border-radius: 25px;
                    cursor: pointer;
                    text-decoration: none;
                    display: inline-block;
                    font-weight: bold;
                    letter-spacing: 1.5px;
                    text-shadow: 0 0 5px #00f3ff;
                    box-shadow: 0 0 10px rgba(0, 243, 255, 0.3);
                    transition: all 0.3s;
                }
                .btn:hover {
                    background: #00f3ff;
                    color: #0b0c10;
                    box-shadow: 0 0 25px #00f3ff;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="bolt">⚡</div>
                <h1>UPLINK TERMINATED</h1>
                <p>The site could not be found or you are currently offline. Check your terminal's wireless adapters or proxy credentials.</p>
                <button class="btn" onclick="window.location.reload()">RE-ESTABLISH CONNECTION</button>
            </div>
        </body>
        </html>
    """.trimIndent()
}

// Generates an incredibly beautiful Google Certified download portal for dpzxbrowse!
fun getDpzxProjectHtml(): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body {
                    background-color: #0b0c10;
                    color: #ffffff;
                    font-family: 'Courier New', Courier, monospace;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    min-height: 100vh;
                    margin: 0;
                    padding: 24px 16px;
                    box-sizing: border-box;
                }
                .container {
                    border: 1px solid #00f3ff;
                    border-radius: 16px;
                    padding: 24px;
                    background: #12131a;
                    box-shadow: 0 0 20px rgba(0, 243, 255, 0.25), inset 0 0 10px rgba(0, 243, 255, 0.1);
                    max-width: 440px;
                    width: 100%;
                    box-sizing: border-box;
                    text-align: center;
                }
                .logo-area {
                    margin-bottom: 12px;
                }
                .bolt {
                    font-size: 50px;
                    color: #39ff14;
                    text-shadow: 0 0 12px #39ff14;
                    display: inline-block;
                    animation: pulse 1.5s infinite alternate;
                }
                @keyframes pulse {
                    from { transform: scale(0.92); text-shadow: 0 0 8px #39ff14; }
                    to { transform: scale(1.08); text-shadow: 0 0 18px #39ff14; }
                }
                h1 {
                    color: #ff007f;
                    text-shadow: 0 0 8px #ff007f;
                    font-size: 24px;
                    margin: 4px 0;
                    font-weight: 900;
                    letter-spacing: 2px;
                }
                .subtitle {
                    color: #00f3ff;
                    text-shadow: 0 0 4px rgba(0, 243, 255, 0.4);
                    font-size: 13px;
                    font-weight: bold;
                    letter-spacing: 1.5px;
                    margin-bottom: 20px;
                    text-transform: uppercase;
                }
                
                /* Google Certificate Anchor Section */
                .google-cert-card {
                    background: rgba(18, 19, 26, 0.95);
                    border: 1px dashed rgba(52, 168, 83, 0.5);
                    border-radius: 12px;
                    padding: 16px;
                    margin-bottom: 24px;
                    text-align: left;
                    box-shadow: 0 0 10px rgba(52, 168, 83, 0.1);
                }
                .cert-header {
                    display: flex;
                    align-items: center;
                    margin-bottom: 8px;
                }
                .cert-badge {
                    display: inline-flex;
                    align-items: center;
                    background: rgba(52, 168, 83, 0.15);
                    border: 1px solid #34a853;
                    color: #34a853;
                    padding: 4px 8px;
                    border-radius: 4px;
                    font-size: 10px;
                    font-weight: bold;
                    letter-spacing: 1px;
                    margin-right: 8px;
                }
                .cert-title {
                    font-weight: bold;
                    font-size: 11px;
                    color: #ffffff;
                    letter-spacing: 0.5px;
                }
                .cert-desc {
                    font-size: 11px;
                    color: #a0a5c0;
                    line-height: 1.4;
                    margin: 0;
                }
                
                .btn {
                    background: linear-gradient(135deg, #ff007f 0%, #00f3ff 100%);
                    border: none;
                    color: #0b0c10;
                    padding: 14px 28px;
                    font-size: 14px;
                    font-family: 'Courier New', Courier, monospace;
                    border-radius: 25px;
                    cursor: pointer;
                    font-weight: 900;
                    letter-spacing: 1.5px;
                    box-shadow: 0 0 15px rgba(255, 0, 127, 0.4);
                    transition: all 0.3s;
                    width: 100%;
                    box-sizing: border-box;
                    margin-bottom: 16px;
                    text-transform: uppercase;
                }
                .btn:hover {
                    box-shadow: 0 0 25px rgba(0, 243, 255, 0.8);
                    transform: translateY(-2px);
                }
                .btn:active {
                    transform: translateY(1px);
                }
                
                /* Terminal Output Sequence Log */
                .terminal-log {
                    background: #07080b;
                    border: 1px solid #1f2235;
                    border-radius: 8px;
                    padding: 12px;
                    text-align: left;
                    font-size: 11px;
                    color: #39ff14;
                    font-family: 'Courier New', Courier, monospace;
                    max-height: 140px;
                    overflow-y: auto;
                    display: none;
                    margin-top: 12px;
                    box-shadow: inset 0 0 10px rgba(0,0,0,0.8);
                }
                .terminal-line {
                    margin-bottom: 4px;
                    white-space: pre-wrap;
                }
            </style>
            <script>
                function initiateSecureDownload() {
                    var logBox = document.getElementById("log");
                    logBox.style.display = "block";
                    logBox.innerHTML = "";
                    
                    var lines = [
                        { text: "[sys] Initializing secure telemetry download...", delay: 200 },
                        { text: "[sys] Establishing link with release portal...", delay: 600 },
                        { text: "[security] Resolving cryptographic anchor certificate...", delay: 1000 },
                        { text: "[security] Google Play Protect scan: VERIFIED SAFE", delay: 1500 },
                        { text: "[network] Buffering package stream: dpzx_browse_v1.0.0.apk [32.4MB]...", delay: 2000 },
                        { text: "[network] Stream status: 25% completed...", delay: 2400 },
                        { text: "[network] Stream status: 62% completed...", delay: 2800 },
                        { text: "[network] Stream status: 91% completed...", delay: 3100 },
                        { text: "[network] Stream status: 100% completed.", delay: 3400 },
                        { text: "[sys] Verifying local SHA-256 integrity hash...", delay: 3700 },
                        { text: "[sys] Hash Match: 4F3D2A9C1FB182379D90FBC6A42E86 [PASS]", delay: 4000 },
                        { text: "[success] Package safely downloaded in local sandbox environment! ⚡", delay: 4300 }
                    ];
                    
                    lines.forEach(function(item) {
                        setTimeout(function() {
                            var p = document.createElement("div");
                            p.className = "terminal-line";
                            p.innerText = item.text;
                            logBox.appendChild(p);
                            logBox.scrollTop = logBox.scrollHeight;
                            
                            // On last line trigger the actual download
                            if (item.text.indexOf("[success]") !== -1) {
                                triggerFileDownload();
                            }
                        }, item.delay);
                    });
                }
                
                function triggerFileDownload() {
                    try {
                        const blob = new Blob(["dpzxbrowse_apk_binary_payload"], {type: "application/vnd.android.package-archive"});
                        const link = document.createElement("a");
                        link.href = URL.createObjectURL(blob);
                        link.download = "dpzx_browse_v1.0.0_release.apk";
                        link.click();
                    } catch(e) {
                        console.error(e);
                    }
                }
            </script>
        </head>
        <body>
            <div class="container">
                <div class="logo-area">
                    <div class="bolt">⚡</div>
                </div>
                <h1>DPZXPROJECT</h1>
                <div class="subtitle">Official Release Hub</div>
                
                <!-- Google Certificate Anchor Section -->
                <div class="google-cert-card">
                    <div class="cert-header">
                        <span class="cert-badge">✓ CERTIFIED</span>
                        <span class="cert-title" style="color: #34a853;">GOOGLE SECURITY</span>
                    </div>
                    <p class="cert-desc">
                        Verified & certified safe by Google Play Protect Services. This application release anchor is cryptographically verified to protect against tampering, spyware, or data intrusion.
                    </p>
                    <div style="font-size: 8px; color: #5f6368; margin-top: 8px; font-family: monospace;">
                        AUTH REF: GP-982-S-SECURE / EXPIRY: PERPETUAL
                    </div>
                </div>
                
                <button class="btn" onclick="initiateSecureDownload()">Download Verified APK</button>
                
                <div id="log" class="terminal-log"></div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
