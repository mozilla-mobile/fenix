/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.searchengine

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.provider.AssetsSearchEngineProvider
import mozilla.components.browser.search.provider.SearchEngineList
import mozilla.components.browser.search.provider.SearchEngineProvider
import mozilla.components.browser.search.provider.filter.SearchEngineFilter
import mozilla.components.browser.search.provider.localization.LocaleSearchLocalizationProvider
import mozilla.components.browser.search.provider.localization.SearchLocalizationProvider
import mozilla.components.service.location.LocationService
import mozilla.components.service.location.MozillaLocationService
import mozilla.components.service.location.search.RegionSearchLocalizationProvider
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.Config
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import java.util.Locale

@SuppressWarnings("TooManyFunctions")
open class FenixSearchEngineProvider(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)
) : SearchEngineProvider {
    private val locationService = with(MozillaLocationService(
        context,
        context.components.core.client,
        BuildConfig.MLS_TOKEN
    )) {
        if (Config.channel.isDebug || !this.hasRegionCached()) {
            LocationService.dummy()
        } else {
            this
        }
    }

    // We have two search engine types: one based on MLS reported region, one based only on Locale.
    // There are multiple steps involved in returning the default search engine for example.
    // Simplest and most effective way to make sure the MLS engines do not mix with Locale based engines
    // is to use the same type of engines for the entire duration of the app's run.
    // See fenix/issues/11875
    private val isRegionCachedByLocationService = locationService.hasRegionCached()

    protected open val localizationProvider: SearchLocalizationProvider =
        RegionSearchLocalizationProvider(locationService)

    @VisibleForTesting
    internal open var baseSearchEngines = scope.async {
        AssetsSearchEngineProvider(localizationProvider).loadSearchEngines(context)
    }

    private val loadedRegion = scope.async { localizationProvider.determineRegion() }

    // https://github.com/mozilla-mobile/fenix/issues/9935
    // Adds a Locale search engine provider as a fallback in case the MLS lookup takes longer
    // than the time it takes for a user to try to search.
    private val fallbackLocationService: SearchLocalizationProvider = LocaleSearchLocalizationProvider()
    private val fallBackProvider =
        AssetsSearchEngineProvider(fallbackLocationService)

    protected open val fallbackEngines = scope.async { fallBackProvider.loadSearchEngines(context) }
    private val fallbackRegion = scope.async { fallbackLocationService.determineRegion() }

    protected open val bundledSearchEngines = scope.async {
        val defaultEngineIdentifiers = baseSearchEngines.await().list.map { it.identifier }.toSet()
        AssetsSearchEngineProvider(
            localizationProvider,
            filters = listOf(object : SearchEngineFilter {
                override fun filter(context: Context, searchEngine: SearchEngine): Boolean {
                    return BUNDLED_SEARCH_ENGINES.contains(searchEngine.identifier) &&
                            !defaultEngineIdentifiers.contains(searchEngine.identifier)
                }
            }),
            additionalIdentifiers = BUNDLED_SEARCH_ENGINES
        ).loadSearchEngines(context)
    }

    @VisibleForTesting
    internal open var customSearchEngines = scope.async {
        CustomSearchEngineProvider().loadSearchEngines(context)
    }

    private var loadedSearchEngines = refreshAsync()

    @VisibleForTesting
    internal val prefs = context.getSharedPreferences(PREF_FILE_SEARCH_ENGINES, Context.MODE_PRIVATE)

    fun getDefaultEngine(context: Context): SearchEngine {
        val engines = installedSearchEngines()
        val selectedName = context.settings().defaultSearchEngineName

        return engines.list.find { it.name == selectedName } ?: engines.default ?: engines.list.first()
    }

    /**
     * @return a list of all SearchEngines that are currently active. These are the engines that
     * are readily available throughout the app.
     */
    fun installedSearchEngines(): SearchEngineList = runBlocking {
        val installedIdentifiers = installedSearchEngineIdentifiers()
        val engineList = loadedSearchEngines.await()

        engineList.copy(
            list = engineList.list.filter {
                installedIdentifiers.contains(it.identifier)
            }.sortedBy { it.name.toLowerCase(Locale.getDefault()) },
            default = engineList.default?.let {
                if (installedIdentifiers.contains(it.identifier)) {
                    it
                } else {
                    null
                }
            }
        )
    }

    fun allSearchEngineIdentifiers() = runBlocking {
        loadedSearchEngines.await().list.map { it.identifier }
    }

    fun uninstalledSearchEngines(): SearchEngineList = runBlocking {
        val installedIdentifiers = installedSearchEngineIdentifiers()
        val engineList = loadedSearchEngines.await()

        engineList.copy(list = engineList.list.filterNot { installedIdentifiers.contains(it.identifier) })
    }

    override suspend fun loadSearchEngines(context: Context): SearchEngineList {
        return installedSearchEngines()
    }

    fun installSearchEngine(context: Context, searchEngine: SearchEngine, isCustom: Boolean = false) = runBlocking {
        if (isCustom) {
            val searchUrl = searchEngine.getSearchTemplate()
            CustomSearchEngineStore.addSearchEngine(context, searchEngine.name, searchUrl)
            reload()
        } else {
            val installedIdentifiers = installedSearchEngineIdentifiers().toMutableSet()
            installedIdentifiers.add(searchEngine.identifier)
            prefs.edit {
                putStringSet(localeAwareInstalledEnginesKey(), installedIdentifiers)
            }
        }
    }

    fun uninstallSearchEngine(context: Context, searchEngine: SearchEngine, isCustom: Boolean = false) = runBlocking {
        if (isCustom) {
            CustomSearchEngineStore.removeSearchEngine(context, searchEngine.identifier)
            reload()
        } else {
            val installedIdentifiers = installedSearchEngineIdentifiers().toMutableSet()
            installedIdentifiers.remove(searchEngine.identifier)
            prefs.edit {
                putStringSet(localeAwareInstalledEnginesKey(), installedIdentifiers)
            }
        }
    }

    fun reload() {
        scope.launch {
            customSearchEngines = scope.async { CustomSearchEngineProvider().loadSearchEngines(context) }
            loadedSearchEngines = refreshAsync()
        }
    }

    // When we change the locale we need to update the baseSearchEngines list
    protected open fun updateBaseSearchEngines() {
        baseSearchEngines = scope.async {
            AssetsSearchEngineProvider(localizationProvider).loadSearchEngines(context)
        }
    }

    private fun refreshAsync() = scope.async {
        val engineList = if (isRegionCachedByLocationService) {
            baseSearchEngines.await()
        } else {
            fallbackEngines.await()
        }
        val bundledList = bundledSearchEngines.await().list
        val customList = customSearchEngines.await().list

        engineList.copy(list = engineList.list + bundledList + customList)
    }

    /**
     * Returns the list of identifiers for installed search engines.
     * Will initialize the list with default engines if needed.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun installedSearchEngineIdentifiers(): Set<String> {
        val installedEnginesKey = localeAwareInstalledEnginesKey()

        if (!prefs.contains(installedEnginesKey)) {
            val searchEngines =
                if (isRegionCachedByLocationService) baseSearchEngines
                else fallbackEngines

            val defaultSet = searchEngines.await()
                .list
                .map { it.identifier }
                .toSet()

            prefs.edit {
                putStringSet(installedEnginesKey, defaultSet)
            }
        }

        val installedIdentifiers = prefs.getStringSet(installedEnginesKey, emptySet()).orEmpty()

        val customEngineIdentifiers = customSearchEngines.await().list.map { it.identifier }.toSet()
        return installedIdentifiers + customEngineIdentifiers
    }

    /**
     * Returns the locale aware key used to store installed search engine identifiers.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun localeAwareInstalledEnginesKey(): String {
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

        return "$INSTALLED_ENGINES_KEY-$tag"
    }

    companion object {
        private val BUNDLED_SEARCH_ENGINES = listOf("reddit", "youtube")
        private const val INSTALLED_ENGINES_KEY = "fenix-installed-search-engines"
        @VisibleForTesting
        internal const val PREF_FILE_SEARCH_ENGINES = "fenix-search-engine-provider"
    }
}
