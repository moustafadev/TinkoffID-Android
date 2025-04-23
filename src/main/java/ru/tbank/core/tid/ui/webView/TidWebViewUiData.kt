package ru.tbank.core.tid.ui.webView

/**
 * @author k.voskrebentsev
 */
internal class TidWebViewUiData(
    val host: String,
    val clientId: String,
    val codeChallenge: String,
    val codeChallengeMethod: String,
    val redirectUri: String,
    val callbackUrl: String,
    val phone: String?,
)
