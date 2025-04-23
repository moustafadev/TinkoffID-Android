package ru.tbank.core.tid.ui.webView

/**
 * @author k.voskrebentsev
 */
internal interface TidWebViewListener {

    fun isUrlForAuthCompletion(url: String): Boolean

    fun completeAuthWithSuccess(url: String)

    fun completeAuthWithCancellation()
}
