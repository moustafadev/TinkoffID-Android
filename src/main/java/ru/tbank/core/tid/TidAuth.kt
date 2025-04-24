/*
 * Copyright © 2024 T-Bank
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.tbank.core.tid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ru.tbank.core.tid.api.TidApi
import ru.tbank.core.tid.codeVerifier.TidCodeVerifierStore
import ru.tbank.core.tid.codeVerifier.TidCodeVerifierUtil
import ru.tbank.core.tid.error.TidInvalidPhoneException
import ru.tbank.core.tid.error.TidRequestException
import ru.tbank.core.tid.ui.webView.TidWebViewUiData


/**
 * Main facade for T-ID authorization
 */
public class TidAuth(
    context: Context,
    private val clientId: String,
    private val redirectUri: String,
    private val tidApiHost: String = TID_API_DEFAULT_HOST,
) {

    private val applicationContext = context.applicationContext
    private val partnerService by lazy { TidPartnerApiService(tidApi = TidApi.createTidApi(context = context, host = tidApiHost)) }
    private val codeVerifierStore by lazy { TidCodeVerifierStore(applicationContext) }
    private val appLinkUtil by lazy { TidAppLinkUtil(context) }

    /**
     * Creates an intent to open T-Bank App or WebView Activity for authorization via T-Bank web
     * based on the results of the method [isTBankAppAuthAvailable()][isTBankAppAuthAvailable]
     * and later return authorization data. If [webMode] is [WhiteLabel][ru.tbank.core.tid.TidWebMode.WhiteLabel], then
     *  WebView Activity will open forcibly, regardless of the value [isTBankAppAuthAvailable()][isTBankAppAuthAvailable]
     *
     * @param callbackUrl AppLink/DeepLink that will be opened when authorization process finishes
     * @param webMode for choosing web mode ([Partner][ru.tbank.core.tid.TidWebMode.Partner] / [WhiteLabel][ru.tbank.core.tid.TidWebMode.WhiteLabel])
     *
     * @throws TidInvalidPhoneException if invoked web auth with [webMode] = [WhiteLabel][ru.tbank.core.tid.TidWebMode.WhiteLabel] mode with invalid phone
     *
     * @return intent for authorization via T-Bank
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(TidInvalidPhoneException::class)
    public fun createTidAuthIntent(callbackUrl: Uri, webMode: TidWebMode = TidWebMode.Partner): Intent {
        return when {
            webMode is TidWebMode.WhiteLabel -> createTidWebViewAuthIntent(callbackUrl, webMode)
            isTBankAppAuthAvailable() -> createTBankAppAuthIntent(callbackUrl)
            else -> createTidWebViewAuthIntent(callbackUrl, webMode)
        }
    }

    /**
     * Creates an intent to open T-Bank App and later returns authorization data.
     *
     * @param callbackUrl AppLink/DeepLink that will be opened when authorization process finishes
     * @return implicit Intent to open T-Bank App
     */
    @RequiresApi(Build.VERSION_CODES.M)
    public fun createTBankAppAuthIntent(callbackUrl: Uri): Intent {
        return buildIntentWithPCKEState { codeChallenge, codeChallengeMethod ->
            appLinkUtil.createTBankAppAuthAppLink(
                clientId = clientId,
                codeChallenge = codeChallenge,
                codeChallengeMethod = codeChallengeMethod,
                callbackUrl = callbackUrl,
                packageName = applicationContext.packageName,
                redirectUrl = redirectUri,
                partnerSdkVersion = "1.0.5",
            )
        }
    }

    /**
     * Creates an intent to open WebView Activity for authorization via T-Bank web and later returns authorization data.
     *
     * @param callbackUrl AppLink/DeepLink that will be opened when authorization process will be finished
     * @param webMode for choosing web mode ([Partner][ru.tbank.core.tid.TidWebMode.Partner] / [WhiteLabel][ru.tbank.core.tid.TidWebMode.WhiteLabel])
     *
     * @throws TidInvalidPhoneException if invoked web auth with [webMode] = [WhiteLabel][ru.tbank.core.tid.TidWebMode.WhiteLabel] mode with invalid phone
     *
     * @return explicit Intent to open [TidWebViewAuthActivity][ru.tbank.core.tid.ui.webView.TidWebViewAuthActivity]
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(TidInvalidPhoneException::class)
    public fun createTidWebViewAuthIntent(callbackUrl: Uri, webMode: TidWebMode = TidWebMode.Partner): Intent {
        return buildIntentWithPCKEState { codeChallenge, codeChallengeMethod ->
            appLinkUtil.createWebViewAuthIntent(
                context = applicationContext,
                uiData = TidWebViewUiData(
                    host = tidApiHost,
                    clientId = clientId,
                    codeChallenge = codeChallenge,
                    codeChallengeMethod = codeChallengeMethod,
                    redirectUri = redirectUri,
                    callbackUrl = callbackUrl.toString(),
                    phone = getPhoneFromWebMode(webMode),
                ),
            )
        }
    }

    @Throws(TidInvalidPhoneException::class)
    private fun getPhoneFromWebMode(webMode: TidWebMode): String? {
        return when (webMode) {
            is TidWebMode.WhiteLabel -> {
                if (appLinkUtil.isPhoneValidForWhiteLabel(webMode.phone)) {
                    webMode.phone
                } else {
                    throw TidInvalidPhoneException()
                }
            }

            else -> null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun buildIntentWithPCKEState(
        intentBuilder: (codeChallenge: String, codeChallengeMethod: String) -> Intent,
    ): Intent {
        val codeVerifier = TidCodeVerifierUtil.generateRandomCodeVerifier()
        val codeChallenge = TidCodeVerifierUtil.deriveCodeVerifierChallenge(codeVerifier)
        val codeChallengeMethod = TidCodeVerifierUtil.getCodeVerifierChallengeMethod()
        codeVerifierStore.codeVerifier = codeVerifier
        return intentBuilder(codeChallenge, codeChallengeMethod)
    }

    /**
     * Checks if authorization via T-Bank App is available on current device
     *
     * @return true if we can open T-Bank App
     */
    public fun isTBankAppAuthAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                appLinkUtil.isPossibleToHandleAppLink() &&
                TidCodeVerifierUtil.getCodeVerifierChallengeMethod() != TidCodeVerifierUtil.CODE_CHALLENGE_METHOD_PLAIN
    }

    /**
     * Function to get Application Token Call
     *
     * @param uri the uri returned after authorization process from T-Bank App
     * in callbackIntent based on callbackUrl from [createTBankAppAuthIntent]
     * @return [TidCall] object to get T-Bank Token by sending request
     *
     * @throws TidRequestException if request not executed
     *
     */
    @Throws(TidRequestException::class)
    public fun getTidTokenPayload(uri: Uri): TidCall<TidTokenPayload> {
        val code = requireNotNull(appLinkUtil.getAuthCode(uri)) { "invalid response format, auth status code required" }
        return partnerService.getToken(code, codeVerifierStore.codeVerifier, clientId, redirectUri)
    }

    /**
     * Function to get status code after authorization process
     *
     * @param uri the uri returned after authorization process from T-Bank App
     * in callbackIntent based on callbackUrl from [createTBankAppAuthIntent]
     * @return [TidStatusCode][ru.tbank.core.tid.TidStatusCode].
     * SUCCESS - we can perform getTidTokenPayload(), CANCELLED_BY_USER -  user
     * canceled authorization process
     *
     */
    public fun getStatusCode(uri: Uri): TidStatusCode? {
        return appLinkUtil.getAuthStatusCode(uri)
    }

    /**
     * Function to get Application Refresh Token Call
     *
     * @param refreshToken [refreshToken][TidTokenPayload.refreshToken] of current session
     * @return [TidCall] object to get T-Bank Token by sending request
     *
     * @throws TidRequestException if request not executed
     */
    @Throws(TidRequestException::class)
    public fun obtainTokenPayload(refreshToken: String): TidCall<TidTokenPayload> {
        return partnerService.refreshToken(refreshToken, clientId)
    }

    /**
     * Sign out call by using accessToken
     *
     * @param accessToken [accessToken][TidTokenPayload.accessToken] of current session
     * @return [TidCall] object that will return Unit if request successfully executed
     *
     * @throws TidRequestException if request not executed
     */
    @Throws(TidRequestException::class)
    public fun signOutByAccessToken(accessToken: String): TidCall<Unit> {
        return partnerService.revokeAccessToken(accessToken, clientId)
    }

    /**
     * Sign out call by using refreshToken
     *
     * @param refreshToken [refreshToken][TidTokenPayload.refreshToken] of current session
     * @return [TidCall] object that will return Unit if request successfully executed
     *
     * @throws TidRequestException if request not executed
     */
    @Throws(TidRequestException::class)
    public fun signOutByRefreshToken(refreshToken: String): TidCall<Unit> {
        return partnerService.revokeRefreshToken(refreshToken, clientId)
    }

    public companion object {
        public const val TID_API_DEFAULT_HOST: String = "https://id.tbank.ru"
    }
}
