/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import mozilla.components.support.ktx.android.content.appVersionName
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.Config
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getColorFromAttr
import org.mozilla.fenix.settings.account.AuthIntentReceiverActivity
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Locale

object SupportUtils {
    const val RATE_APP_URL = "market://details?id=" + BuildConfig.APPLICATION_ID
    const val MOZILLA_MANIFESTO_URL = "https://www.mozilla.org/en-GB/about/manifesto/"
    const val FENIX_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

    enum class SumoTopic(internal val topicStr: String) {
        HELP("faq-android"),
        PRIVATE_BROWSING_MYTHS("common-myths-about-private-browsing"),
        YOUR_RIGHTS("your-rights"),
        TRACKING_PROTECTION("tracking-protection-firefox-preview"),
        WHATS_NEW("whats-new-firefox-preview"),
        SEND_TABS("send-tab-preview"),
        SET_AS_DEFAULT_BROWSER("set-firefox-preview-default"),
        SEARCH_SUGGESTION("how-search-firefox-preview"),
        CUSTOM_SEARCH_ENGINES("custom-search-engines"),
        UPGRADE_FAQ("firefox-preview-upgrade-faqs")
    }

    /**
     * Gets a support page URL for the corresponding topic.
     */
    fun getSumoURLForTopic(
        context: Context,
        topic: SumoTopic,
        locale: Locale = Locale.getDefault()
    ): String {
        val escapedTopic = getEncodedTopicUTF8(topic.topicStr)
        // Remove the whitespace so a search is not triggered:
        val appVersion = context.appVersionName?.replace(" ", "")
        val osTarget = "Android"
        val langTag = getLanguageTag(locale)
        return "https://support.mozilla.org/1/mobile/$appVersion/$osTarget/$langTag/$escapedTopic"
    }

    /**
     * Gets a support page URL for the corresponding topic.
     * Used when the app version and os are not part of the URL.
     */
    fun getGenericSumoURLForTopic(topic: SumoTopic, locale: Locale = Locale.getDefault()): String {
        val escapedTopic = getEncodedTopicUTF8(topic.topicStr)
        val langTag = getLanguageTag(locale)
        return "https://support.mozilla.org/$langTag/kb/$escapedTopic"
    }

    fun getPrivacyNoticeUrl(locale: Locale = Locale.getDefault()) =
        "https://www.mozilla.org/${getLanguageTag(locale)}/privacy/firefox/"

    fun getWhatsNewUrl(context: Context) = if (Config.channel.isFennec) {
        getGenericSumoURLForTopic(SumoTopic.UPGRADE_FAQ)
    } else {
        getSumoURLForTopic(context, SumoTopic.WHATS_NEW)
    }

    fun createCustomTabIntent(context: Context, url: String): Intent = CustomTabsIntent.Builder()
        .setInstantAppsEnabled(false)
        .setToolbarColor(context.getColorFromAttr(R.attr.foundation))
        .build()
        .intent
        .setData(url.toUri())
        .setClassName(context, IntentReceiverActivity::class.java.name)
        .setPackage(context.packageName)

    fun createAuthCustomTabIntent(context: Context, url: String): Intent =
        createCustomTabIntent(context, url).setClassName(context, AuthIntentReceiverActivity::class.java.name)

    private fun getEncodedTopicUTF8(topic: String): String {
        try {
            return URLEncoder.encode(topic, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("utf-8 should always be available", e)
        }
    }

    private fun getLanguageTag(locale: Locale): String {
        val language = locale.language
        val country = locale.country // Can be an empty string.
        return if (country.isEmpty()) language else "$language-$country"
    }
}
