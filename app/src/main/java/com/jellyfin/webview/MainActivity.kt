package com.jellyfin.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var setupLayout: LinearLayout
    private lateinit var urlInput: EditText
    private lateinit var connectButton: Button
    private lateinit var statusMessage: TextView

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    companion object {
        private const val PREFS_NAME = "JellyfinWebViewPrefs"
        private const val KEY_SERVER_URL = "server_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        setupLayout = findViewById(R.id.setupLayout)
        urlInput = findViewById(R.id.urlInput)
        connectButton = findViewById(R.id.connectButton)
        statusMessage = findViewById(R.id.statusMessage)

        configureWebView()
        setupBackNavigation()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(KEY_SERVER_URL, null)

        connectButton.setOnClickListener {
            connectToInputUrl()
        }

        urlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                connectToInputUrl()
                true
            } else {
                false
            }
        }

        if (!savedUrl.isNullOrBlank()) {
            urlInput.setText(savedUrl)
            loadJellyfinUrl(savedUrl)
        } else {
            showSetupScreen()
        }
    }

    private fun connectToInputUrl() {
        var rawUrl = urlInput.text.toString().trim()
        if (rawUrl.isEmpty()) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            return
        }

        if (!rawUrl.startsWith("http://", ignoreCase = true) &&
            !rawUrl.startsWith("https://", ignoreCase = true)
        ) {
            rawUrl = "http://$rawUrl"
        }

        if (!Patterns.WEB_URL.matcher(rawUrl).matches()) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_URL, rawUrl)
            .apply()

        loadJellyfinUrl(rawUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.setBackgroundColor(Color.parseColor("#101010"))

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    statusMessage.text = "Could not connect to ${request.url}\n(${error?.description ?: "Connection error"})"
                    showSetupScreen()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback

                fullscreenContainer.addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE

                setImmersiveFullscreen(true)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }

            override fun onHideCustomView() {
                if (customView == null) return

                setImmersiveFullscreen(false)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                fullscreenContainer.removeView(customView)
                fullscreenContainer.visibility = View.GONE
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null

                webView.visibility = View.VISIBLE
            }
        }
    }

    private fun loadJellyfinUrl(url: String) {
        setupLayout.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun showSetupScreen() {
        webView.visibility = View.GONE
        setupLayout.visibility = View.VISIBLE
        urlInput.requestFocus()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    customView != null -> {
                        webView.webChromeClient?.onHideCustomView()
                    }
                    setupLayout.visibility == View.VISIBLE -> {
                        finish()
                    }
                    webView.canGoBack() -> {
                        webView.goBack()
                    }
                    else -> {
                        // When at the root of the Jellyfin web app, pressing back allows changing server or exiting
                        finish()
                    }
                }
            }
        })
    }

    private fun setImmersiveFullscreen(enable: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
