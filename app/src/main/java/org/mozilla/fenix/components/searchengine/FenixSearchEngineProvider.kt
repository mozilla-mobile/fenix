package org.mozilla.fenix.components.searchengine

import android.content.Context
import kotlinx.coroutines.*
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.provider.AssetsSearchEngineProvider
import mozilla.components.browser.search.provider.SearchEngineList
import mozilla.components.browser.search.provider.SearchEngineProvider
import mozilla.components.browser.search.provider.filter.SearchEngineFilter
import mozilla.components.browser.search.provider.localization.LocaleSearchLocalizationProvider
import org.mozilla.fenix.ext.settings

class FenixSearchEngineProvider(
    private val context: Context
): SearchEngineProvider, CoroutineScope by CoroutineScope(Job() + Dispatchers.IO) {
    private val defaultEngines = async {
        AssetsSearchEngineProvider(LocaleSearchLocalizationProvider()).loadSearchEngines(context)
    }

    private val bundledEngines = async {
        AssetsSearchEngineProvider(
            LocaleSearchLocalizationProvider(),
            filters = listOf(object : SearchEngineFilter {
                override fun filter(context: Context, searchEngine: SearchEngine): Boolean {
                    return BUNDLED_SEARCH_ENGINES.contains(searchEngine.identifier)
                }
            }),
            additionalIdentifiers = BUNDLED_SEARCH_ENGINES
        ).loadSearchEngines(context)
    }

    private val customEngines = async {
        CustomSearchEngineProvider().loadSearchEngines(context)
    }

    private var loadedSearchEngines = refreshAsync()

    fun getDefaultEngine(context: Context): SearchEngine {
        val engines = installedSearchEngines(context)
        val selectedName = context.settings().defaultSearchEngineName

        return engines.list.find { it.name == selectedName } ?: engines.list.first()
    }

    fun installedSearchEngines(context: Context): SearchEngineList = runBlocking {
        val engineList = loadedSearchEngines.await()
        val installedIdentifiers = installedSearchEngineIdentifiers(context)

        engineList.copy(
            list = engineList.list.filter {
                installedIdentifiers.contains(it.identifier)
            },
            default = engineList.default?.let {
                if (installedIdentifiers.contains(it.identifier)) {
                    it
                } else {
                    null
                }
            }
        )
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

    fun uninstallSearchEngine(context: Context, searchEngine: SearchEngine) = runBlocking {
        val installedIdentifiers = installedSearchEngineIdentifiers(context).toMutableSet()
        installedIdentifiers.remove(searchEngine.identifier)
        prefs(context).edit().putStringSet(INSTALLED_ENGINES_KEY, installedIdentifiers).commit()
    }

    fun reload() {
        launch {
            loadedSearchEngines = refreshAsync()
        }
    }

    private fun refreshAsync() = async {
        val engineList = defaultEngines.await()
        val bundledList = bundledEngines.await().list
        val customList = customEngines.await().list
        engineList.copy(list = engineList.list + bundledList + customList)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(
        PREF_FILE,
        Context.MODE_PRIVATE
    )

    private suspend fun installedSearchEngineIdentifiers(context: Context): Set<String> {
        val prefs = prefs(context)

        val identifiers = if (!prefs.contains(INSTALLED_ENGINES_KEY)) {
            val defaultSet =  defaultEngines.await()
                .list
                .map { it.identifier }
                .toSet()

            prefs.edit().putStringSet(INSTALLED_ENGINES_KEY, defaultSet).apply()
            defaultSet
        } else {
            prefs(context).getStringSet(INSTALLED_ENGINES_KEY, setOf()) ?: setOf()
        }

        val customEngineIdentifiers = customEngines.await().list.map { it.identifier }.toSet()
        return identifiers + customEngineIdentifiers
    }

    companion object {
        private val BUNDLED_SEARCH_ENGINES = listOf("ecosia", "reddit", "startpage", "yahoo", "youtube")
        private const val PREF_FILE = "fenix-search-engine-provider"
        private const val INSTALLED_ENGINES_KEY = "fenix-installed-search-engines"
        private const val EMPTY = ""
    }
}