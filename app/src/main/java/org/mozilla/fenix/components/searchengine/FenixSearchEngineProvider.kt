package org.mozilla.fenix.components.searchengine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.provider.AssetsSearchEngineProvider
import mozilla.components.browser.search.provider.SearchEngineList
import mozilla.components.browser.search.provider.SearchEngineProvider
import mozilla.components.browser.search.provider.filter.SearchEngineFilter
import mozilla.components.browser.search.provider.localization.LocaleSearchLocalizationProvider

class FenixSearchEngineProvider(
    private val context: Context
): SearchEngineProvider, CoroutineScope by CoroutineScope(Job() + Dispatchers.IO) {

    private val defaultProvider = AssetsSearchEngineProvider(
        LocaleSearchLocalizationProvider()
    )

    private val bundledProvider = AssetsSearchEngineProvider(
        LocaleSearchLocalizationProvider(),
        filters = listOf(object : SearchEngineFilter {
            override fun filter(context: Context, searchEngine: SearchEngine): Boolean {
                return BUNDLED_SEARCH_ENGINES.contains(searchEngine.identifier)
            }
        }),
        additionalIdentifiers = BUNDLED_SEARCH_ENGINES
    )

    private var loadedSearchEngines: Deferred<SearchEngineList>

    init {
        val defaultEngines = async { defaultProvider.loadSearchEngines(context) }
        val bundledEngines = async { bundledProvider.loadSearchEngines(context) }

        loadedSearchEngines = async {
            val defaultEngineList = defaultEngines.await()
            defaultEngineList.copy(list = defaultEngineList.list + bundledEngines.await().list)
        }
    }

    fun installedSearchEngines(context: Context): SearchEngineList = runBlocking {
        val engineList = loadedSearchEngines.await()
        val installedIdentifiers = installedSearchEngineIdentifiers(context)

        engineList.copy(list = engineList.list.filter { installedIdentifiers.contains(it.identifier) })
    }

    fun uninstalledSearchEngines(context: Context): SearchEngineList = runBlocking {
        val engineList = loadedSearchEngines.await()
        val installedIdentifiers = installedSearchEngineIdentifiers(context)

        engineList.copy(list = engineList.list.filterNot { installedIdentifiers.contains(it.identifier) })
    }

    override suspend fun loadSearchEngines(context: Context): SearchEngineList {
        return installedSearchEngines(context)
    }

    fun installSearchEngine(context: Context, searchEngine: SearchEngine) = runBlocking {
        val installedIdentifiers = installedSearchEngineIdentifiers(context).toMutableSet()
        installedIdentifiers.add(searchEngine.identifier)
        prefs(context).edit().putStringSet(INSTALLED_ENGINES_KEY, installedIdentifiers).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(
        PREF_FILE,
        Context.MODE_PRIVATE
    )

    private suspend fun installedSearchEngineIdentifiers(context: Context): Set<String> {
        val prefs = prefs(context)

        if (!prefs.contains(INSTALLED_ENGINES_KEY)) {
            val defaultSet =  loadedSearchEngines.await()
                .list
                .filterNot { BUNDLED_SEARCH_ENGINES.contains(it.identifier) }
                .map { it.identifier }
                .toSet()

            prefs.edit().putStringSet(INSTALLED_ENGINES_KEY, defaultSet).apply()
            return defaultSet
        }

        return prefs(context).getStringSet(INSTALLED_ENGINES_KEY, setOf()) ?: setOf()
    }

    companion object {
        private val BUNDLED_SEARCH_ENGINES = listOf("ecosia", "reddit", "startpage", "yahoo", "youtube")
        private const val PREF_FILE = "fenix-search-engine-provider"
        private const val INSTALLED_ENGINES_KEY = "fenix-installed-search-engines"
    }
}