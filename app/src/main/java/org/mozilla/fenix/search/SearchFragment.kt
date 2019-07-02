/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface.BOLD
import android.graphics.Typeface.ITALIC
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.style.StyleSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.toolbar_wrapper
import kotlinx.android.synthetic.main.fragment_search.view.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.browser.search.SearchEngine
import mozilla.components.feature.qr.QrFeature
import mozilla.components.lib.state.Store
import mozilla.components.lib.state.ext.observe
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.android.content.hasCamera
import mozilla.components.support.ktx.android.content.isPermissionGranted
import mozilla.components.support.ktx.kotlin.isUrl
import org.jetbrains.anko.backgroundDrawable
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ThemeManager
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.getSpannable
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.search.awesomebar.AwesomeBarView
import org.mozilla.fenix.search.toolbar.ToolbarView
import org.mozilla.fenix.utils.Settings

@Suppress("TooManyFunctions", "LargeClass")
class SearchFragment : Fragment(), BackHandler {
    private lateinit var toolbarView: ToolbarView
    private lateinit var awesomeBarView: AwesomeBarView
    private var sessionId: String? = null
    private var isPrivate = false
    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()
    private var permissionDidUpdate = false
    private lateinit var searchStore: SearchStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireComponents.analytics.metrics.track(Event.InteractWithSearchURLArea)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sessionId = SearchFragmentArgs.fromBundle(arguments!!).sessionId
        isPrivate = (activity as HomeActivity).browsingModeManager.isPrivate

        val session = sessionId?.let { requireComponents.core.sessionManager.findSessionById(it) }
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val url = session?.url ?: ""

        searchStore = Store(
            SearchState(
                query = url,
                searchEngineSource = SearchEngineSource.Default(
                    requireComponents.search.searchEngineManager.getDefaultSearchEngine(requireContext())
                ),
                showSuggestions = Settings.getInstance(requireContext()).showSearchSuggestions,
                showVisitedSitesBookmarks = Settings.getInstance(requireContext()).shouldShowVisitedSitesBookmarks
            ),
            ::searchStateReducer
        )

        toolbarView = ToolbarView(
            view.toolbar_component_wrapper,
            ::onUrlCommitted,
            { Navigation.findNavController(toolbar_wrapper).navigateUp() },
            { searchStore.dispatch(SearchAction.UpdateQuery(it)) },
            {
                if (Settings.getInstance(requireContext()).shouldShowVisitedSitesBookmarks) {
                    requireComponents.core.historyStorage
                } else null
            }
        )

        awesomeBarView = AwesomeBarView(
            view.search_layout,
            ::onURLTapped,
            ::onSearchTermsTapped,
            ::onSearchShortcutEngineSelected,
            ::onSearchEngineSettingsTapped
        )

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        search_scan_button.visibility = if (context?.hasCamera() == true) View.VISIBLE else View.GONE
        layoutComponents(view.search_layout)

        qrFeature.set(
            QrFeature(
                requireContext(),
                fragmentManager = requireFragmentManager(),
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_CAMERA_PERMISSIONS)
                },
                onScanResult = { result ->
                    search_scan_button.isChecked = false
                    activity?.let {
                        AlertDialog.Builder(
                            ContextThemeWrapper(
                                it,
                                R.style.DialogStyle
                            )
                        ).apply {
                            val spannable = resources.getSpannable(
                                R.string.qr_scanner_confirmation_dialog_message,
                                listOf(
                                    getString(R.string.app_name) to listOf(StyleSpan(BOLD)),
                                    result to listOf(StyleSpan(ITALIC))
                                )
                            )
                            setMessage(spannable)
                            setNegativeButton(R.string.qr_scanner_dialog_negative) { dialog: DialogInterface, _ ->
                                requireComponents.analytics.metrics.track(Event.QRScannerNavigationDenied)
                                dialog.cancel()
                            }
                            setPositiveButton(R.string.qr_scanner_dialog_positive) { dialog: DialogInterface, _ ->
                                requireComponents.analytics.metrics.track(Event.QRScannerNavigationAllowed)
                                (activity as HomeActivity)
                                    .openToBrowserAndLoad(
                                        searchTermOrURL = result,
                                        newTab = sessionId == null,
                                        from = BrowserDirection.FromSearch
                                    )
                                dialog.dismiss()
                            }
                            create()
                        }.show()
                        requireComponents.analytics.metrics.track(Event.QRScannerPromptDisplayed)
                    }
                }),
            owner = this,
            view = view
        )

        view.search_scan_button.setOnClickListener {
            toolbarView.view.clearFocus()
            requireComponents.analytics.metrics.track(Event.QRScannerOpened)
            qrFeature.get()?.scan(R.id.container)
        }

        view.toolbar_wrapper.clipToOutline = false

        search_shortcuts_button.setOnClickListener {
            val isOpen = searchStore.state.showShortcutEnginePicker
            searchStore.dispatch(SearchAction.SearchShortcutEnginePicker(!isOpen))

            if (isOpen) {
                requireComponents.analytics.metrics.track(Event.SearchShortcutMenuClosed)
            } else {
                requireComponents.analytics.metrics.track(Event.SearchShortcutMenuOpened)
            }
        }

        searchStore.observe(view) {
            MainScope().launch {
                awesomeBarView.update(it)
                updateSearchEngineIcon(it)
                updateSearchShortuctsIcon(it)
                updateSearchWithLabel(it)
            }
        }

        startPostponedEnterTransition()
    }

    override fun onResume() {
        super.onResume()

        if (!permissionDidUpdate) {
            toolbarView.view.requestFocus()
        }

        permissionDidUpdate = false
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        toolbarView.view.clearFocus()
    }

    override fun onBackPressed(): Boolean {
        return when {
            qrFeature.onBackPressed() -> {
                view?.search_scan_button?.isChecked = false
                toolbarView.view.requestFocus()
                true
            }
            else -> false
        }
    }

    private fun onUrlCommitted(url: String) {
        if (url.isNotBlank()) {
            (activity as HomeActivity).openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = sessionId == null,
                from = BrowserDirection.FromSearch,
                engine = searchStore.state.searchEngineSource.searchEngine
            )

            val event = if (url.isUrl()) {
                Event.EnteredUrl(false)
            } else {
                createSearchEvent(searchStore.state.searchEngineSource.searchEngine, false)
            }

            requireComponents.analytics.metrics.track(event)
        }
    }

    private fun onURLTapped(url: String) {
        (activity as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = sessionId == null,
            from = BrowserDirection.FromSearch
        )
        requireComponents.analytics.metrics.track(Event.EnteredUrl(false))
    }

    private fun onSearchTermsTapped(searchTerms: String) {
        (activity as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = searchTerms,
            newTab = sessionId == null,
            from = BrowserDirection.FromSearch,
            engine = searchStore.state.searchEngineSource.searchEngine,
            forceSearch = true
        )

        val event = createSearchEvent(searchStore.state.searchEngineSource.searchEngine, true)

        requireComponents.analytics.metrics.track(event)
    }

    private fun onSearchShortcutEngineSelected(engine: SearchEngine) {
        searchStore.dispatch(SearchAction.SearchShortcutEngineSelected(engine))
        requireComponents.analytics.metrics.track(Event.SearchShortcutSelected(engine.name))
    }

    private fun onSearchEngineSettingsTapped() {
        val directions = SearchFragmentDirections.actionSearchFragmentToSearchEngineFragment()
        findNavController().navigate(directions)
    }

    private fun updateSearchEngineIcon(searchState: SearchState) {
        val searchIcon = searchState.searchEngineSource.searchEngine.icon
        val draw = BitmapDrawable(resources, searchIcon)
        val iconSize = resources.getDimension(R.dimen.preference_icon_drawable_size).toInt()
        draw.setBounds(0, 0, iconSize, iconSize)
        search_engine_icon?.backgroundDrawable = draw
    }

    private fun updateSearchWithLabel(searchState: SearchState) {
        search_with_shortcuts.visibility = if (searchState.showShortcutEnginePicker) View.VISIBLE else View.GONE
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

    private fun updateSearchShortuctsIcon(searchState: SearchState) {
        with(requireContext()) {
            val showShortcuts = searchState.showShortcutEnginePicker
            search_shortcuts_button?.isChecked = showShortcuts

            val color = if (showShortcuts) R.attr.foundation else R.attr.primaryText

            search_shortcuts_button.compoundDrawables[0]?.setTint(
                ContextCompat.getColor(
                    this,
                    ThemeManager.resolveAttribute(color, this)
                )
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSIONS -> qrFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)

                context?.let { context: Context ->
                    if (context.isPermissionGranted(Manifest.permission.CAMERA)) {
                        permissionDidUpdate = true
                    } else {
                        view?.search_scan_button?.isChecked = false
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1
    }
}
