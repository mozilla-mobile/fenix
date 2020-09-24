/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.awesomebar

import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.core.graphics.drawable.toBitmap
import mozilla.components.browser.awesomebar.BrowserAwesomeBar
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.session.Session
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.awesomebar.provider.BookmarksStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.HistoryStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchActionProvider
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.syncedtabs.DeviceIndicators
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.syncedtabs.SyncedTabsStorageSuggestionProvider
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.search.SearchEngineSource
import org.mozilla.fenix.search.SearchFragmentState

/**
 * View that contains and configures the BrowserAwesomeBar
 */
@Suppress("LargeClass")
class AwesomeBarView(
    private val activity: HomeActivity,
    val interactor: AwesomeBarInteractor,
    val view: BrowserAwesomeBar
) {
    private val sessionProvider: SessionSuggestionProvider
    private val historyStorageProvider: HistoryStorageSuggestionProvider
    private val shortcutsEnginePickerProvider: ShortcutsSuggestionProvider
    private val bookmarksStorageSuggestionProvider: BookmarksStorageSuggestionProvider
    private val syncedTabsStorageSuggestionProvider: SyncedTabsStorageSuggestionProvider
    private val defaultSearchSuggestionProvider: SearchSuggestionProvider
    private val defaultSearchActionProvider: SearchActionProvider
    private val searchSuggestionProviderMap: MutableMap<SearchEngine, List<AwesomeBar.SuggestionProvider>>
    private var providersInUse = mutableSetOf<AwesomeBar.SuggestionProvider>()

    private val loadUrlUseCase = object : SessionUseCases.LoadUrlUseCase {
        override fun invoke(
            url: String,
            flags: EngineSession.LoadUrlFlags,
            additionalHeaders: Map<String, String>?
        ) {
            interactor.onUrlTapped(url)
        }
    }

    private val searchUseCase = object : SearchUseCases.SearchUseCase {
        override fun invoke(
            searchTerms: String,
            searchEngine: SearchEngine?,
            parentSession: Session?
        ) {
            interactor.onSearchTermsTapped(searchTerms)
        }
    }

    private val shortcutSearchUseCase = object : SearchUseCases.SearchUseCase {
        override fun invoke(
            searchTerms: String,
            searchEngine: SearchEngine?,
            parentSession: Session?
        ) {
            interactor.onSearchTermsTapped(searchTerms)
        }
    }

    private val selectTabUseCase = object : TabsUseCases.SelectTabUseCase {
        override fun invoke(session: Session) {
            interactor.onExistingSessionSelected(session)
        }

        override fun invoke(tabId: String) {
            interactor.onExistingSessionSelected(tabId)
        }
    }

    init {
        view.itemAnimator = null

        val components = activity.components
        val primaryTextColor = activity.getColorFromAttr(R.attr.primaryText)

        val engineForSpeculativeConnects = when (activity.browsingModeManager.mode) {
            BrowsingMode.Normal -> components.core.engine
            BrowsingMode.Private -> null
        }
        sessionProvider =
            SessionSuggestionProvider(
                activity.resources,
                components.core.store,
                selectTabUseCase,
                components.core.icons,
                getDrawable(activity, R.drawable.ic_search_results_tab),
                excludeSelectedSession = true
            )

        historyStorageProvider =
            HistoryStorageSuggestionProvider(
                components.core.historyStorage,
                loadUrlUseCase,
                components.core.icons,
                engineForSpeculativeConnects
            )

        bookmarksStorageSuggestionProvider =
            BookmarksStorageSuggestionProvider(
                bookmarksStorage = components.core.bookmarksStorage,
                loadUrlUseCase = loadUrlUseCase,
                icons = components.core.icons,
                indicatorIcon = getDrawable(activity, R.drawable.ic_search_results_bookmarks),
                engine = engineForSpeculativeConnects
            )

        syncedTabsStorageSuggestionProvider =
            SyncedTabsStorageSuggestionProvider(
                components.backgroundServices.syncedTabsStorage,
                loadUrlUseCase,
                components.core.icons,
                DeviceIndicators(
                    getDrawable(activity, R.drawable.ic_search_results_device_desktop),
                    getDrawable(activity, R.drawable.ic_search_results_device_mobile),
                    getDrawable(activity, R.drawable.ic_search_results_device_tablet)
                )
            )

        val searchBitmap = getDrawable(activity, R.drawable.ic_search)!!.apply {
            colorFilter = createBlendModeColorFilterCompat(primaryTextColor, SRC_IN)
        }.toBitmap()

        defaultSearchSuggestionProvider =
            SearchSuggestionProvider(
                context = activity,
                searchEngineManager = components.search.searchEngineManager,
                searchUseCase = searchUseCase,
                fetchClient = components.core.client,
                mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                limit = 3,
                icon = searchBitmap,
                showDescription = false,
                engine = engineForSpeculativeConnects,
                filterExactMatch = true
            )

        defaultSearchActionProvider =
            SearchActionProvider(
                searchEngineGetter = suspend {
                    components.search.searchEngineManager.getDefaultSearchEngineAsync(activity)
                },
                searchUseCase = searchUseCase,
                icon = searchBitmap,
                showDescription = false
            )

        shortcutsEnginePickerProvider =
            ShortcutsSuggestionProvider(
                searchEngineProvider = components.search.provider,
                context = activity,
                selectShortcutEngine = interactor::onSearchShortcutEngineSelected,
                selectShortcutEngineSettings = interactor::onClickSearchEngineSettings
            )

        searchSuggestionProviderMap = HashMap()
    }

    fun update(state: SearchFragmentState) {
        updateSuggestionProvidersVisibility(state)

        // Do not make suggestions based on user's current URL unless it's a search shortcut
        if (state.query.isNotEmpty() && state.query == state.url && !state.showSearchShortcuts) {
            return
        }

        view.onInputChanged(state.query)
    }

    private fun updateSuggestionProvidersVisibility(state: SearchFragmentState) {
        if (state.showSearchShortcuts) {
            handleDisplayShortcutsProviders()
            return
        }

        val providersToAdd = getProvidersToAdd(state)
        val providersToRemove = getProvidersToRemove(state)

        performProviderListChanges(providersToAdd, providersToRemove)
    }

    private fun performProviderListChanges(
        providersToAdd: MutableSet<AwesomeBar.SuggestionProvider>,
        providersToRemove: MutableSet<AwesomeBar.SuggestionProvider>
    ) {
        for (provider in providersToAdd) {
            if (providersInUse.find { it.id == provider.id } == null) {
                providersInUse.add(provider)
                view.addProviders(provider)
            }
        }

        for (provider in providersToRemove) {
            if (providersInUse.find { it.id == provider.id } != null) {
                providersInUse.remove(provider)
                view.removeProviders(provider)
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun getProvidersToAdd(state: SearchFragmentState): MutableSet<AwesomeBar.SuggestionProvider> {
        val providersToAdd = mutableSetOf<AwesomeBar.SuggestionProvider>()

        if (state.showHistorySuggestions) {
            providersToAdd.add(historyStorageProvider)
        }

        if (state.showBookmarkSuggestions) {
            providersToAdd.add(bookmarksStorageSuggestionProvider)
        }

        if (state.showSearchSuggestions) {
            providersToAdd.addAll(getSelectedSearchSuggestionProvider(state))
        }

        if (state.showSyncedTabsSuggestions) {
            providersToAdd.add(syncedTabsStorageSuggestionProvider)
        }

        if (activity.browsingModeManager.mode == BrowsingMode.Normal) {
            providersToAdd.add(sessionProvider)
        }

        return providersToAdd
    }

    private fun getProvidersToRemove(state: SearchFragmentState): MutableSet<AwesomeBar.SuggestionProvider> {
        val providersToRemove = mutableSetOf<AwesomeBar.SuggestionProvider>()

        providersToRemove.add(shortcutsEnginePickerProvider)

        if (!state.showHistorySuggestions) {
            providersToRemove.add(historyStorageProvider)
        }

        if (!state.showBookmarkSuggestions) {
            providersToRemove.add(bookmarksStorageSuggestionProvider)
        }

        if (!state.showSearchSuggestions) {
            providersToRemove.addAll(getSelectedSearchSuggestionProvider(state))
        }

        if (!state.showSyncedTabsSuggestions) {
            providersToRemove.add(syncedTabsStorageSuggestionProvider)
        }

        if (activity.browsingModeManager.mode == BrowsingMode.Private) {
            providersToRemove.add(sessionProvider)
        }

        return providersToRemove
    }

    private fun getSelectedSearchSuggestionProvider(state: SearchFragmentState): List<AwesomeBar.SuggestionProvider> {
        return when (state.searchEngineSource) {
            is SearchEngineSource.Default -> listOf(
                defaultSearchActionProvider,
                defaultSearchSuggestionProvider
            )
            is SearchEngineSource.Shortcut -> getSuggestionProviderForEngine(
                state.searchEngineSource.searchEngine
            )
        }
    }

    private fun handleDisplayShortcutsProviders() {
        view.removeAllProviders()
        providersInUse.clear()
        providersInUse.add(shortcutsEnginePickerProvider)
        view.addProviders(shortcutsEnginePickerProvider)
    }

    private fun getSuggestionProviderForEngine(engine: SearchEngine): List<AwesomeBar.SuggestionProvider> {
        return searchSuggestionProviderMap.getOrPut(engine) {
            val components = activity.components
            val primaryTextColor = activity.getColorFromAttr(R.attr.primaryText)

            val searchBitmap = getDrawable(activity, R.drawable.ic_search)!!.apply {
                colorFilter = createBlendModeColorFilterCompat(primaryTextColor, SRC_IN)
            }.toBitmap()

            val engineForSpeculativeConnects = when (activity.browsingModeManager.mode) {
                BrowsingMode.Normal -> components.core.engine
                BrowsingMode.Private -> null
            }
            val searchEngine =
                components.search.provider.installedSearchEngines(activity).list.find { it.name == engine.name }
                    ?: components.search.provider.getDefaultEngine(activity)

            listOf(
                SearchActionProvider(
                    searchEngineGetter = suspend { searchEngine },
                    searchUseCase = shortcutSearchUseCase,
                    icon = searchBitmap
                ),
                SearchSuggestionProvider(
                    searchEngine,
                    shortcutSearchUseCase,
                    components.core.client,
                    limit = 3,
                    mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                    icon = searchBitmap,
                    engine = engineForSpeculativeConnects,
                    filterExactMatch = true
                )
            )
        }
    }
}
