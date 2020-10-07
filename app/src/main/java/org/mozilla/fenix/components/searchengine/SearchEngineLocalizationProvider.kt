/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.searchengine

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import mozilla.components.browser.search.provider.AssetsSearchEngineProvider
import mozilla.components.browser.search.provider.localization.LocaleSearchLocalizationProvider
import mozilla.components.browser.search.provider.localization.SearchLocalizationProvider
import mozilla.components.service.location.LocationService
import mozilla.components.service.location.MozillaLocationService
import mozilla.components.service.location.search.RegionSearchLocalizationProvider
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.Config
import org.mozilla.fenix.ext.components
import kotlin.coroutines.CoroutineContext


/**
 * Handles localization for search engines
 */
open class SearchEngineLocalizationProvider(
    private val context: Context,
    coroutineContext: CoroutineContext
) {
    private val shouldMockMLS = Config.channel.isDebug || BuildConfig.MLS_TOKEN.isEmpty()

    private val locationService: LocationService = if (shouldMockMLS) {
        LocationService.dummy()
    } else {
        MozillaLocationService(
            context,
            context.components.core.client,
            BuildConfig.MLS_TOKEN
        )
    }

    // We have two search engine types: one based on MLS reported region, one based only on Locale.
    // There are multiple steps involved in returning the default search engine for example.
    // Simplest and most effective way to make sure the MLS engines do not mix with Locale based engines
    // is to use the same type of engines for the entire duration of the app's run.
    // See fenix/issues/11875
    val isRegionCachedByLocationService = locationService.hasRegionCached()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    open val searchLocalizationProvider: SearchLocalizationProvider =
        RegionSearchLocalizationProvider(locationService)

    private val loadedRegion =
        CoroutineScope(coroutineContext).async { searchLocalizationProvider.determineRegion() }

    // https://github.com/mozilla-mobile/fenix/issues/9935
    // Adds a Locale search engine provider as a fallback in case the MLS lookup takes longer
    // than the time it takes for a user to try to search.
    private val fallbackLocationService: SearchLocalizationProvider = LocaleSearchLocalizationProvider()
    private val fallBackProvider =
        AssetsSearchEngineProvider(fallbackLocationService)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    open val fallbackEngines =
        CoroutineScope(coroutineContext).async { fallBackProvider.loadSearchEngines(context) }
    private val fallbackRegion =
        CoroutineScope(coroutineContext).async { fallbackLocationService.determineRegion() }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun localeAwareInstalledEnginesKey(): String {
        val tag = if (isRegionCachedByLocationService) {
            val localization = loadedRegion.await()
            val region = localization.region?.let {
                if (it.isEmpty()) "" else "-$it"
            }

            "${localization.languageTag}$region"
        } else {
            val localization = fallbackRegion.await()
            val region = localization.region?.let {
                if (it.isEmpty()) "" else "-$it"
            }

            "${localization.languageTag}$region-fallback"
        }

        return "${FenixSearchEngineProvider.INSTALLED_ENGINES_KEY}-$tag"
    }
}
