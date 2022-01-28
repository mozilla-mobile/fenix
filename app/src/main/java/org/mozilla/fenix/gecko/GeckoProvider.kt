/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gecko

import android.content.Context
import mozilla.components.browser.engine.gecko.autofill.GeckoAutocompleteStorageDelegate
import mozilla.components.browser.engine.gecko.ext.toContentBlockingSetting
import mozilla.components.browser.engine.gecko.glean.GeckoAdapter
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.storage.CreditCardsAddressesStorage
import mozilla.components.concept.storage.LoginsStorage
import mozilla.components.lib.crash.handler.CrashHandlerService
import mozilla.components.service.sync.autofill.GeckoCreditCardsAddressesStorageDelegate
import mozilla.components.service.sync.logins.GeckoLoginStorageDelegate
import org.mozilla.fenix.Config
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.ContentBlocking.SafeBrowsingProvider
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

object GeckoProvider {
    private var runtime: GeckoRuntime? = null
    private const val CN_UPDATE_URL =
        "https://sb.firefox.com.cn/downloads?client=SAFEBROWSING_ID&appver=%MAJOR_VERSION%&pver=2.2"
    private const val CN_GET_HASH_URL =
        "https://sb.firefox.com.cn/gethash?client=SAFEBROWSING_ID&appver=%MAJOR_VERSION%&pver=2.2"

    @Synchronized
    fun getOrCreateRuntime(
        context: Context,
        autofillStorage: Lazy<CreditCardsAddressesStorage>,
        loginStorage: Lazy<LoginsStorage>,
        trackingProtectionPolicy: TrackingProtectionPolicy
    ): GeckoRuntime {
        if (runtime == null) {
            runtime =
                createRuntime(context, autofillStorage, loginStorage, trackingProtectionPolicy)
        }

        return runtime!!
    }

    private fun createRuntime(
        context: Context,
        autofillStorage: Lazy<CreditCardsAddressesStorage>,
        loginStorage: Lazy<LoginsStorage>,
        policy: TrackingProtectionPolicy
    ): GeckoRuntime {
        val builder = GeckoRuntimeSettings.Builder()

        val runtimeSettings = builder
            .crashHandler(CrashHandlerService::class.java)
            .telemetryDelegate(GeckoAdapter())
            .contentBlocking(policy.toContentBlockingSetting())
            .debugLogging(Config.channel.isDebug)
            .aboutConfigEnabled(Config.channel.isBeta || Config.channel.isNightlyOrDebug)
            .build()

        val settings = context.components.settings
        if (!settings.shouldUseAutoSize) {
            runtimeSettings.automaticFontSizeAdjustment = false
            val fontSize = settings.fontSizeFactor
            runtimeSettings.fontSizeFactor = fontSize
        }

        // Add safebrowsing providers for China
        val o = SafeBrowsingProvider
            .from(ContentBlocking.GOOGLE_SAFE_BROWSING_PROVIDER)
            .getHashUrl("")
            .updateUrl("")
            .build()
        runtimeSettings.contentBlocking.setSafeBrowsingProviders(o)
        runtimeSettings.contentBlocking.setSafeBrowsingPhishingTable("goog-phish-proto")

        val geckoRuntime = GeckoRuntime.create(context, runtimeSettings)

        geckoRuntime.autocompleteStorageDelegate = GeckoAutocompleteStorageDelegate(
            GeckoCreditCardsAddressesStorageDelegate(autofillStorage) {
                context.settings().shouldAutofillCreditCardDetails
            },
            GeckoLoginStorageDelegate(loginStorage)
        )

        return geckoRuntime
    }
}
