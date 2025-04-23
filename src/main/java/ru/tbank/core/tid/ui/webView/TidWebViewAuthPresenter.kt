package ru.tbank.core.tid.ui.webView

import ru.tbank.core.tid.api.TidApi

/**
 * @author k.voskrebentsev
 */
internal class TidWebViewAuthPresenter {

    fun buildWebViewAuthStartUrl(uiData: TidWebViewUiData): String {
        return TidApi.buildWebViewAuthStartUrl(
            host = uiData.host,
            clientId = uiData.clientId,
            codeChallenge = uiData.codeChallenge,
            codeChallengeMethod = uiData.codeChallengeMethod,
            redirectUri = uiData.redirectUri,
            phone = uiData.phone,
        )
    }

    fun parseCode(url: String): String {
        return TidApi.parseCode(url)
    }
}
