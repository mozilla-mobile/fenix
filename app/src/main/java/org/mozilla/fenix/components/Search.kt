/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.search.SearchEngineManager
import mozilla.components.browser.search.provider.AssetsSearchEngineProvider
import mozilla.components.browser.search.provider.localization.LocaleSearchLocalizationProvider
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.test.Mockable
import java.util.Locale

/**
 * Component group for all search engine integration related functionality.
 */
@Mockable
class Search(private val context: Context) {

    /**
     * This component provides access to a centralized registry of search engines.
     *
     * NOTE: only the async API of this manager should be used.
     * See https://github.com/mozilla-mobile/fenix/issues/3869#issuecomment-524951003
     */
    val searchEngineManager by lazy {
        SearchEngineManager(
            coroutineContext = IO, providers = listOf(
                AssetsSearchEngineProvider(LocaleSearchLocalizationProvider())
//                TODO use RegionSearchLocalizationProvider instead once API key is available
//                AssetsSearchEngineProvider(RegionSearchLocalizationProvider(
//                    MozillaLocationService(
//                        context = context,
//                        client = context.components.core.client,
//                        apiKey = ???
//                    )
//                ))
            )
        ).apply {
            registerForLocaleUpdates(context)
            GlobalScope.launch {
                defaultSearchEngine = getDefaultSearchEngineAsync(
                    context,
                    context.settings().defaultSearchEngineName
                )
            }
        }
    }

    /**
     * Provides access to a centralized registry of search engines.
     *
     * This instance will lookup search engines using [Locale], which may be less accurate. Prefer
     * using [searchEngineManager] where possible.
     */
    val localeSearchEngineManager = SearchEngineManager(
        coroutineContext = IO, providers = listOf(
            AssetsSearchEngineProvider(LocaleSearchLocalizationProvider())
        )
    ).apply {
        registerForLocaleUpdates(context)
        GlobalScope.launch {
            defaultSearchEngine = getDefaultSearchEngineAsync(
                context,
                context.settings().defaultSearchEngineName
            )
        }
    }
}
