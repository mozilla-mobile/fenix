/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import mozilla.components.browser.search.SearchEngine
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.ktx.kotlin.isUrl
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.utils.ItsNotBrokenSnack
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.toolbar.SearchAction
import org.mozilla.fenix.components.toolbar.SearchChange
import org.mozilla.fenix.components.toolbar.SearchState
import org.mozilla.fenix.components.toolbar.ToolbarComponent
import org.mozilla.fenix.components.toolbar.ToolbarUIView
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getAutoDisposeObservable
import org.mozilla.fenix.mvi.getManagedEmitter
import org.mozilla.fenix.search.awesomebar.AwesomeBarAction
import org.mozilla.fenix.search.awesomebar.AwesomeBarChange
import org.mozilla.fenix.search.awesomebar.AwesomeBarComponent
import org.mozilla.fenix.search.awesomebar.AwesomeBarUIView

class SearchFragment : Fragment() {
    private lateinit var toolbarComponent: ToolbarComponent
    private lateinit var awesomeBarComponent: AwesomeBarComponent
    private var sessionId: String? = null
    private var isPrivate = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sessionId = SearchFragmentArgs.fromBundle(arguments!!).sessionId
        isPrivate = (activity as HomeActivity).browsingModeManager.isPrivate
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val url = sessionId?.let {
            requireComponents.core.sessionManager.findSessionById(it)?.let { session ->
                session.url
            }
        } ?: ""

        view.search_scan_button.setOnClickListener {
            ItsNotBrokenSnack(context!!).showSnackbar(issueNumber = "113")
        }

        toolbarComponent = ToolbarComponent(
            view.toolbar_component_wrapper,
            ActionBusFactory.get(this),
            sessionId,
            isPrivate,
            SearchState(url, isEditing = true),
            view.search_engine_icon
        )

        awesomeBarComponent = AwesomeBarComponent(view.search_layout, ActionBusFactory.get(this))
        ActionBusFactory.get(this).logMergedObservables()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutComponents(view.search_layout)

        lifecycle.addObserver((toolbarComponent.uiView as ToolbarUIView).toolbarIntegration)

        view.toolbar_wrapper.clipToOutline = false

        search_shortcuts_button.setOnClickListener {
            val isOpen = (awesomeBarComponent.uiView as AwesomeBarUIView).state?.showShortcutEnginePicker ?: false

            getManagedEmitter<AwesomeBarChange>().onNext(AwesomeBarChange.SearchShortcutEnginePicker(!isOpen))

            if (isOpen) {
                requireComponents.analytics.metrics.track(Event.SearchShortcutMenuClosed)
            } else {
                requireComponents.analytics.metrics.track(Event.SearchShortcutMenuOpened)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onStart() {
        super.onStart()
        subscribeToSearchActions()
        subscribeToAwesomeBarActions()
    }

    private fun subscribeToSearchActions() {
        getAutoDisposeObservable<SearchAction>()
            .subscribe {
                when (it) {
                    is SearchAction.UrlCommitted -> {
                        if (it.url.isNotBlank()) {
                            (activity as HomeActivity).openToBrowserAndLoad(it.url, it.session, it.engine,
                                BrowserDirection.FromSearch)

                            val event = if (it.url.isUrl()) {
                                Event.EnteredUrl(false)
                            } else {
                                if (it.engine == null) { return@subscribe }

                                createSearchEvent(it.engine, false)
                            }

                            requireComponents.analytics.metrics.track(event)
                        }
                    }
                    is SearchAction.TextChanged -> {
                        getManagedEmitter<AwesomeBarChange>().onNext(AwesomeBarChange.UpdateQuery(it.query))
                    }
                    is SearchAction.EditingCanceled -> {
                        activity?.onBackPressed()
                    }
                }
            }
    }

    private fun subscribeToAwesomeBarActions() {
        getAutoDisposeObservable<AwesomeBarAction>()
            .subscribe {
                when (it) {
                    is AwesomeBarAction.URLTapped -> {
                        getSessionUseCase(requireContext(), sessionId == null).invoke(it.url)
                        (activity as HomeActivity).openToBrowser(sessionId, BrowserDirection.FromSearch)
                        requireComponents.analytics.metrics.track(Event.EnteredUrl(false))
                    }
                    is AwesomeBarAction.SearchTermsTapped -> {
                        getSearchUseCase(requireContext(), sessionId == null)
                            .invoke(it.searchTerms, it.engine)
                        (activity as HomeActivity).openToBrowser(sessionId, BrowserDirection.FromSearch)

                        if (it.engine == null) { return@subscribe }
                        val event = createSearchEvent(it.engine, true)

                        requireComponents.analytics.metrics.track(event)
                    }
                    is AwesomeBarAction.SearchShortcutEngineSelected -> {
                        getManagedEmitter<AwesomeBarChange>()
                            .onNext(AwesomeBarChange.SearchShortcutEngineSelected(it.engine))
                        getManagedEmitter<SearchChange>()
                            .onNext(SearchChange.SearchShortcutEngineSelected(it.engine))

                        requireComponents.analytics.metrics.track(Event.SearchShortcutSelected(it.engine.name))
                    }
                }
            }
    }

    private fun createSearchEvent(engine: SearchEngine, isSuggestion: Boolean): Event.PerformedSearch {
        val isShortcut = engine != requireComponents.search.searchEngineManager.defaultSearchEngine

        val engineSource =
            if (isShortcut) Event.PerformedSearch.EngineSource.Shortcut(engine)
            else Event.PerformedSearch.EngineSource.Default(engine)

        val source =
            if (isSuggestion) Event.PerformedSearch.EventSource.Suggestion(engineSource)
            else Event.PerformedSearch.EventSource.Action(engineSource)

        return Event.PerformedSearch(source)
    }

    private fun getSearchUseCase(context: Context, useNewTab: Boolean): SearchUseCases.SearchUseCase {
        if (!useNewTab) {
            return context.components.useCases.searchUseCases.defaultSearch
        }

        return when (isPrivate) {
            true -> context.components.useCases.searchUseCases.newPrivateTabSearch
            false -> context.components.useCases.searchUseCases.newTabSearch
        }
    }

    private fun getSessionUseCase(context: Context, useNewTab: Boolean): SessionUseCases.LoadUrlUseCase {
        if (!useNewTab) {
            return context.components.useCases.sessionUseCases.loadUrl
        }

        return when (isPrivate) {
            true -> context.components.useCases.tabsUseCases.addPrivateTab
            false -> context.components.useCases.tabsUseCases.addTab
        }
    }
}
