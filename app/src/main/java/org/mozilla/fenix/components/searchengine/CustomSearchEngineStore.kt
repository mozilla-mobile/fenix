/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.searchengine

import android.content.Context
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.SearchEngineParser
import mozilla.components.browser.search.provider.SearchEngineList
import mozilla.components.browser.search.provider.SearchEngineProvider
import org.mozilla.fenix.ext.components
import java.lang.Exception

/**
 * SearchEngineProvider implementation to load user entered custom search engines.
 */
class CustomSearchEngineProvider : SearchEngineProvider {
    // Our version of ktlint enforces the wrong modifier order. We need to update the plugin: #2488
    /* ktlint-disable modifier-order */
    override suspend fun loadSearchEngines(context: Context): SearchEngineList {
        return SearchEngineList(CustomSearchEngineStore.loadCustomSearchEngines(context), null)
    }
}

object CustomSearchEngineStore {
    class EngineNameAlreadyExists : Exception()

    suspend fun addSearchEngine(context: Context, engineName: String, searchQuery: String) {
        val prefs = pref(context)
        if (prefs.contains(engineName)) { throw EngineNameAlreadyExists() }

        val icon = context.components.core.icons.loadIcon(IconRequest(searchQuery)).await()
        val searchEngineXml = SearchEngineWriter.buildSearchEngineXML(engineName, searchQuery, icon.bitmap)
        val engines = (pref(context)
            .getStringSet(PREF_KEY_CUSTOM_SEARCH_ENGINES, emptySet()) ?: emptySet()).toMutableSet()
        engines.add(engineName)

        pref(context)
            .edit()
            .putStringSet(PREF_KEY_CUSTOM_SEARCH_ENGINES, engines)
            .putString(engineName, searchEngineXml)
            .apply()
    }

    fun removeSearchEngine(context: Context, engineId: String) {
        val customEngines = pref(context).getStringSet(PREF_KEY_CUSTOM_SEARCH_ENGINES, emptySet())
        val enginesEditor = pref(context).edit()
        enginesEditor.remove(engineId)

        enginesEditor.putStringSet(
            PREF_KEY_CUSTOM_SEARCH_ENGINES,
            customEngines?.filterNot { it == engineId }?.toSet() ?: emptySet()
        )

        enginesEditor.apply()
    }

    fun isCustomSearchEngine(context: Context, engineId: String): Boolean {
        return loadCustomSearchEngines(context).firstOrNull { it.identifier == engineId } != null
    }

    fun loadCustomSearchEngines(context: Context): List<SearchEngine> {
        val parser = SearchEngineParser()
        val prefs = pref(context)
        val engines = prefs.getStringSet(PREF_KEY_CUSTOM_SEARCH_ENGINES, emptySet()) ?: emptySet()

        return engines.map {
            val engineInputStream = prefs.getString(it, "")!!.byteInputStream().buffered()
            parser.load(it, engineInputStream)
        }
    }

    private fun pref(context: Context) =
        context.getSharedPreferences(PREF_FILE_SEARCH_ENGINES, Context.MODE_PRIVATE)

    const val ENGINE_TYPE_CUSTOM = "custom"
    const val ENGINE_TYPE_BUNDLED = "bundled"

    private const val REMOVED_ENGINES = "removed_engine_ids"
    private const val PREF_KEY_CUSTOM_SEARCH_ENGINES = "pref_custom_search_engines"
    private const val PREF_FILE_SEARCH_ENGINES = "custom-search-engines"
}
