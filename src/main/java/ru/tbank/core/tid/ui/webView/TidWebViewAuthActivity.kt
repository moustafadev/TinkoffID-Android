package ru.tbank.core.tid.ui.webView

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import ru.tbank.core.tid.TidAppLinkUtil
import ru.tbank.core.tid.error.TidLoggingConstants.LOG_TAG
import tbank.core.tid.R

/**
 * @author k.voskrebentsev
 */
internal class TidWebViewAuthActivity : AppCompatActivity() {

    private val presenter: TidWebViewAuthPresenter by lazy { TidWebViewAuthPresenter() }
    private val appLinkUtil by lazy { TidAppLinkUtil(this) }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tid_web_view_activity)

        val uiData = appLinkUtil.parseTidWebViewUiData(intent)

        initWebView(uiData)
        initToolbar(uiData)
        initBackPress(uiData)
    }

    private fun initToolbar(uiData: TidWebViewUiData) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        toolbar.inflateMenu(R.menu.tid_web_view_auth_menu)
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.reloadMenuItem) {
                webView.reload()
                true
            } else {
                false
            }
        }

        toolbar.setNavigationOnClickListener {
            finishWithCancellation(uiData.callbackUrl)
        }
    }

    private fun initBackPress(uiData: TidWebViewUiData) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithCancellation(uiData.callbackUrl)
            }
        }
        onBackPressedDispatcher.addCallback(callback)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(uiData: TidWebViewUiData) {
        webView = findViewById(R.id.webView)
        val url = presenter.buildWebViewAuthStartUrl(uiData)
        webView.run {
            webViewClient = TidWebViewClient(createTidWebViewCallback(uiData))
            with(settings) {
                javaScriptEnabled = true
                setGeolocationEnabled(false)
                cacheMode = LOAD_NO_CACHE
                allowFileAccess = false
                allowContentAccess = false
            }
            loadUrl(url)
        }
    }

    private fun createTidWebViewCallback(uiData: TidWebViewUiData): TidWebViewListener {
        return object : TidWebViewListener {

            override fun isUrlForAuthCompletion(url: String): Boolean {
                return url.startsWith(uiData.redirectUri)
            }

            @Suppress("TooGenericExceptionCaught")
            override fun completeAuthWithSuccess(url: String) {
                val code = try {
                    presenter.parseCode(url)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "error when trying to parse code from a redirect url = $url", e)
                    completeAuthWithCancellation()
                    return
                }

                finish(
                    intent = appLinkUtil.createBackAppCodeIntent(
                        callbackUrl = uiData.callbackUrl,
                        code = code,
                    )
                )
            }

            override fun completeAuthWithCancellation() {
                finishWithCancellation(uiData.callbackUrl)
            }
        }
    }

    private fun finishWithCancellation(callbackUrl: String) {
        finish(
            intent = appLinkUtil.createBackAppCancelIntent(callbackUrl)
        )
    }

    private fun finish(intent: Intent) {
        intent.setPackage(packageName)
        startActivity(intent)
        finish()
    }
}
