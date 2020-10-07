/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.searchengine

import android.content.Context
import android.content.SharedPreferences
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.SearchEngineParser
import mozilla.components.browser.search.provider.SearchEngineList
import mozilla.components.browser.search.provider.SearchEngineProvider
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.stringSetPreference
import org.mozilla.fenix.R
import org.mozilla.fenix.collections.CollectionCreationAction
import org.mozilla.fenix.collections.CollectionCreationState
import org.mozilla.fenix.collections.SaveCollectionStep
import org.mozilla.fenix.collections.getTabs
import org.mozilla.fenix.collections.toTab
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.home.Tab

/**
 * SearchEngineProvider implementation to load user entered custom search engines.
 */
class CustomSearchEngineProvider : SearchEngineProvider {
    override suspend fun loadSearchEngines(context: Context): SearchEngineList {
        return SearchEngineList(CustomSearchEngineStore.loadCustomSearchEngines(context), null)
    }
}


/**
 * Object to handle storing custom search engines
 */
object CustomSearchEngineStore {
    class EngineNameAlreadyExists : Exception()



    /**
     * Add a search engine to the store.
     * @param context [Context] used for various Android interactions.
     * @param engineName The name of the search engine
     * @param searchQuery The templated search string for the search engine
     * @throws EngineNameAlreadyExists if you try to add a search engine that already exists
     */
    suspend fun addSearchEngine(context: Context, engineName: String, searchQuery: String) {
        if (CustomSearchEngineState.customSearchEngineIds.contains(engineName)) {
            throw EngineNameAlreadyExists()
        }

        val icon = context.components.core.icons.loadIcon(IconRequest(searchQuery)).await()
        val searchEngineXml = SearchEngineWriter.buildSearchEngineXML(engineName, searchQuery, icon.bitmap)
        val engines = storage.customSearchEngineIds.toMutableSet()

        val newSearchEngine = Set<String>()

        engines.add(engineName)
        storage.customSearchEngineIds = engines
        storage[engineName] = searchEngineXml
    }

    /**
     * Updates an existing search engine.
     * To prevent duplicate search engines we want to remove the old engine before adding the new one
     * @param context [Context] used for various Android interactions.
     * @param oldEngineName the name of the engine you want to replace
     * @param newEngineName the name of the engine you want to save
     * @param searchQuery The templated search string for the search engine
     */
    suspend fun updateSearchEngine(
        context: Context,
        oldEngineName: String,
        newEngineName: String,
        searchQuery: String
    ) {

        removeSearchEngine(context, oldEngineName)
        addSearchEngine(context, newEngineName, searchQuery)
    }

    /**
     * Removes a search engine from the store
     * @param context [Context] used for various Android interactions.
     * @param engineId the id of the engine you want to remove
     */
    fun removeSearchEngine(context: Context, engineId: String) {
        val storage = engineStorage(context)
        val customEngines = storage.customSearchEngineIds
        storage.customSearchEngineIds = customEngines.filterNot { it == engineId }.toSet()
        storage[engineId] = null
    }

    /**
     * Checks the store to see if it contains a search engine
     * @param context [Context] used for various Android interactions.
     * @param engineId The name of the engine to check
     */
    fun isCustomSearchEngine(context: Context, engineId: String): Boolean {
        val storage = engineStorage(context)
        return storage.customSearchEngineIds.contains(engineId)
    }

    /**
     * Creates a list of [SearchEngine] from the store
     * @param context [Context] used for various Android interactions.
     */
    fun loadCustomSearchEngines(context: Context): List<SearchEngine> {
        val storage = engineStorage(context)
        val parser = SearchEngineParser()
        val engines = storage.customSearchEngineIds

        return engines.mapNotNull {
            val engineXml = storage[it] ?: return@mapNotNull null
            val engineInputStream = engineXml.byteInputStream().buffered()
            parser.load(it, engineInputStream)
        }
    }
//
//    /**
//     * Creates a helper object to help interact with [SharedPreferences]
//     * @param context [Context] used for various Android interactions.
//     */
//    private fun engineStorage(context: Context) = object : PreferencesHolder {
//        override val preferences: SharedPreferences
//            get() = context.getSharedPreferences(
//                context.getPreferenceKey(
//                    R.string.pref_key_file_search_engines
//                ), Context.MODE_PRIVATE
//            )
//
//        var customSearchEngineIds by stringSetPreference(
//            context.getPreferenceKey(
//                R.string.pref_key_custom_search_engines
//            ), emptySet()
//        )
//
//        operator fun get(engineId: String): String? {
//            return preferences.getString(engineId, null)
//        }
//
//        operator fun set(engineId: String, value: String?) {
//            preferences.edit().putString(engineId, value).apply()
//        }
//    }

}

data class CustomSearchEngineState(
    val searchEngineList: SearchEngineList = SearchEngineList(emptyList(), default = null)
) : State


sealed class CustomSearchEngineAction : Action {
    data class AddCustomSearchEngine(val newSearchEngine: SearchEngine) : CustomSearchEngineAction()
    data class DeleteCustomSearchEngine(val id: String) : CustomSearchEngineAction()
    data class EditSearchEngine(val oldSearchEngine: SearchEngine,
                                val newSearchEngine: SearchEngine) : CustomSearchEngineAction()
    data class SetDefaultSearchEngine(val id: String) : CustomSearchEngineAction()
}

internal fun CustomSearchEngineState.get(id: String): SearchEngine? {
    return searchEngineList.list.firstOrNull { it.identifier == id }
}

private fun customSearchEngineReducer(
    prevState: CustomSearchEngineState,
    action: CustomSearchEngineAction
): CustomSearchEngineState = when (action) {
    is CustomSearchEngineAction.AddCustomSearchEngine ->
        prevState.copy(
            searchEngineList = SearchEngineList(
                list = prevState.searchEngineList.list + action.newSearchEngine,
                default = prevState.searchEngineList.default
            )
        )
    is CustomSearchEngineAction.DeleteCustomSearchEngine -> {
        val default =
            if (prevState.searchEngineList.default?.identifier == action.id) {
                // how to get original default from the provider if we delete the curr?
                prevState.searchEngineList.list[0]
            } else {
                prevState.searchEngineList.default
            }

        prevState.copy(
            searchEngineList = SearchEngineList(
                list = prevState.searchEngineList.list.filter { it.identifier != action.id },
                default = default
            )
        )
    }

    is CustomSearchEngineAction.EditSearchEngine -> {
        val default =
            if (prevState.searchEngineList.default?.identifier == action.oldSearchEngine.identifier) {
                // how to get original default from the provider if we delete the curr?
                prevState.searchEngineList.list[0]
            } else {
                prevState.searchEngineList.default
            }
        // remove old version of engine
        prevState.copy(
            searchEngineList = SearchEngineList(
                list = prevState.searchEngineList.list
                    .filter { it.identifier != action.oldSearchEngine.identifier },
                default = default
            )
        )
        // add edited new version, maintaining defaults
        prevState.copy(
            searchEngineList = SearchEngineList(
                list = prevState.searchEngineList.list + action.newSearchEngine,
                default = default
            )
        )
    }
    is CustomSearchEngineAction.SetDefaultSearchEngine -> {
        val newDefault =
            prevState.searchEngineList.list.first { it.identifier != action.id }
        prevState.copy(
            searchEngineList = SearchEngineList(
                list = prevState.searchEngineList.list,
                default = newDefault
            )
        )
    }
}
