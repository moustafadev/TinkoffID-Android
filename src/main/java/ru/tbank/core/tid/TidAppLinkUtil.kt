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
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import ru.tbank.core.tid.ui.webView.TidWebViewAuthActivity
import ru.tbank.core.tid.ui.webView.TidWebViewUiData
import java.util.regex.Pattern

/**
 * @author Stanislav Mukhametshin
 */
internal class TidAppLinkUtil(private val context: Context) {

    private companion object {
        private const val PARTNER_HOST = "www.tinkoff.ru"
        private const val PARTNER_AUTH_CATEGORY = "ru.tinkoff.partner.TINKOFF_APP"

        private const val QUERY_PARAMETER_HOST = "host"
        private const val QUERY_PARAMETER_CLIENT_ID = "clientId"
        private const val QUERY_PARAMETER_CODE_CHALLENGE = "code_challenge"
        private const val QUERY_PARAMETER_CODE_CHALLENGE_METHOD = "code_challenge_method"
        private const val QUERY_PARAMETER_CALLBACK_URL = "callback_url"
        private const val QUERY_PARAMETER_PACKAGE = "package_name"
        private const val QUERY_PARAMETER_CODE = "code"
        private const val QUERY_PARAMETER_AUTH_STATUS_CODE = "auth_status_code"
        private const val QUERY_PARAMETER_REDIRECT_URI = "redirect_uri"
        private const val QUERY_PARAMETER_PHONE = "phone"
        private const val QUERY_PARTNER_SDK_VERSION = "partner_sdk_version"

        private const val AUTH_STATUS_CODE_SUCCESS = "success"
        private const val AUTH_STATUS_CODE_CANCELLED_BY_USER = "cancelled_by_user"
    }

    private val baseUri = Uri.Builder()
        .scheme("https")
        .authority(PARTNER_HOST)
        .appendPath("partner_auth")
        .build()

    fun createTBankAppAuthAppLink(
        clientId: String,
        codeChallenge: String,
        codeChallengeMethod: String,
        callbackUrl: Uri,
        packageName: String?,
        redirectUrl: String,
        partnerSdkVersion: String,
    ): Intent {
        val uri = baseUri.buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_CLIENT_ID, clientId)
            .appendQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE, codeChallenge)
            .appendQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE_METHOD, codeChallengeMethod)
            .appendQueryParameter(QUERY_PARAMETER_CALLBACK_URL, callbackUrl.toString())
            .appendQueryParameter(QUERY_PARAMETER_PACKAGE, packageName)
            .appendQueryParameter(QUERY_PARAMETER_REDIRECT_URI, redirectUrl)
            .appendQueryParameter(QUERY_PARTNER_SDK_VERSION, partnerSdkVersion)
            .build()
        return Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }
    }

    fun createWebViewAuthIntent(
        context: Context,
        uiData: TidWebViewUiData,
    ): Intent {
        return Intent(context, TidWebViewAuthActivity::class.java)
            .putExtra(QUERY_PARAMETER_HOST, uiData.host)
            .putExtra(QUERY_PARAMETER_CLIENT_ID, uiData.clientId)
            .putExtra(QUERY_PARAMETER_CODE_CHALLENGE, uiData.codeChallenge)
            .putExtra(QUERY_PARAMETER_CODE_CHALLENGE_METHOD, uiData.codeChallengeMethod)
            .putExtra(QUERY_PARAMETER_REDIRECT_URI, uiData.redirectUri)
            .putExtra(QUERY_PARAMETER_CALLBACK_URL, uiData.callbackUrl)
            .putExtra(QUERY_PARAMETER_PHONE, uiData.phone)
    }

    fun parseTidWebViewUiData(
        intent: Intent,
    ): TidWebViewUiData {
        return TidWebViewUiData(
            host = intent.requireStringExtra(QUERY_PARAMETER_HOST),
            clientId = intent.requireStringExtra(QUERY_PARAMETER_CLIENT_ID),
            codeChallenge = intent.requireStringExtra(QUERY_PARAMETER_CODE_CHALLENGE),
            codeChallengeMethod = intent.requireStringExtra(QUERY_PARAMETER_CODE_CHALLENGE_METHOD),
            redirectUri = intent.requireStringExtra(QUERY_PARAMETER_REDIRECT_URI),
            callbackUrl = intent.requireStringExtra(QUERY_PARAMETER_CALLBACK_URL),
            phone = intent.getStringExtra(QUERY_PARAMETER_PHONE),
        )
    }

    private fun Intent.requireStringExtra(key: String): String {
        return requireNotNull(getStringExtra(key)) { "the required web view parameter is missing" }
    }

    fun isPossibleToHandleAppLink(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            isPossibleToHandleAppLinkFor30AndBelow()
        } else {
            isPossibleToHandleAppLinkFor31AndAbove()
        }
    }

    fun isPhoneValidForWhiteLabel(phone: String): Boolean {
        return Pattern.compile("^\\+7\\d{10}").matcher(phone).matches()
    }

    private fun isPossibleToHandleAppLinkFor30AndBelow(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = baseUri
            addCategory(PARTNER_AUTH_CATEGORY)
        }
        val availablePackages = getAvailablePackages(intent)

        return availablePackages.isNotEmpty()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isPossibleToHandleAppLinkFor31AndAbove(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = baseUri
        }
        val availablePackages = getAvailablePackages(intent)

        return availablePackages.any {
            checkAppLinkVerify(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkAppLinkVerify(packageName: String): Boolean {
        val manager = context.getSystemService(DomainVerificationManager::class.java)
        val userState = manager.getDomainVerificationUserState(packageName)

        val verifiedHosts = userState?.hostToStateMap
            ?.filterValues { it == DomainVerificationUserState.DOMAIN_STATE_VERIFIED || it == DomainVerificationUserState.DOMAIN_STATE_SELECTED }
            ?.keys

        return verifiedHosts?.any { it == PARTNER_HOST } ?: false
    }

    private fun getAvailablePackages(intent: Intent): List<String> {
        val flag = PackageManager.MATCH_DEFAULT_ONLY
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(
                    flag.toLong()
                )
            )
        } else {
            context.packageManager.queryIntentActivities(intent, flag)
        }

        return resolveInfo.map { it.activityInfo.packageName }
    }

    fun createBackAppCodeIntent(
        callbackUrl: String,
        code: String,
    ): Intent {
        val data = buildBackAppLinkWithStatus(callbackUrl, AUTH_STATUS_CODE_SUCCESS)
            .appendQueryParameter(QUERY_PARAMETER_CODE, code)
            .build()
        return Intent(Intent.ACTION_VIEW)
            .setData(data)
    }

    fun createBackAppCancelIntent(
        callbackUrl: String,
    ): Intent {
        val data = buildBackAppLinkWithStatus(callbackUrl, AUTH_STATUS_CODE_CANCELLED_BY_USER)
            .build()
        return Intent(Intent.ACTION_VIEW)
            .setData(data)
    }

    private fun buildBackAppLinkWithStatus(callbackUrl: String, status: String): Uri.Builder {
        return Uri.parse(callbackUrl).buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE, status)
    }

    fun getAuthCode(uri: Uri): String? = uri.getQueryParameter(QUERY_PARAMETER_CODE)

    fun getAuthStatusCode(uri: Uri): TidStatusCode? {
        val statusCode = uri.getQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE)
        return authStatusCodesMap[statusCode]
    }

    private val authStatusCodesMap = mapOf(
        AUTH_STATUS_CODE_SUCCESS to TidStatusCode.SUCCESS,
        AUTH_STATUS_CODE_CANCELLED_BY_USER to TidStatusCode.CANCELLED_BY_USER
    )
}
