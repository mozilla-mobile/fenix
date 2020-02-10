/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface.BOLD
import android.graphics.Typeface.ITALIC
import android.os.Bundle
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.android.synthetic.main.fragment_search.view.*
import kotlinx.android.synthetic.main.search_suggestions_onboarding.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.qr.QrFeature
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.android.content.hasCamera
import mozilla.components.support.ktx.android.content.isPermissionGranted
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getSpannable
import org.mozilla.fenix.ext.hideToolbar
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.search.awesomebar.AwesomeBarView
import org.mozilla.fenix.search.toolbar.ToolbarView
import org.mozilla.fenix.settings.SupportUtils

@Suppress("TooManyFunctions", "LargeClass")
class SearchFragment : Fragment(), UserInteractionHandler {
    private lateinit var toolbarView: ToolbarView
    private lateinit var awesomeBarView: AwesomeBarView
    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()
    private var permissionDidUpdate = false
    private lateinit var searchStore: SearchFragmentStore
    private lateinit var searchInteractor: SearchInteractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
                .setDuration(
                    SHARED_TRANSITION_MS
                )
        requireComponents.analytics.metrics.track(Event.InteractWithSearchURLArea)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args = arguments?.let { navArgs<SearchFragmentArgs>().value }
        val session = args?.sessionId
            ?.let(requireComponents.core.sessionManager::findSessionById)
        val pastedText = args?.pastedText
        val searchAccessPoint = args?.searchAccessPoint

        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val url = session?.url.orEmpty()
        val currentSearchEngine = SearchEngineSource.Default(
            requireComponents.search.provider.getDefaultEngine(requireContext())
        )

        val showSearchSuggestions =
            if (requireComponents.browsingModeManager.mode.isPrivate) {
                requireContext().settings().shouldShowSearchSuggestions &&
                        requireContext().settings().shouldShowSearchSuggestionsInPrivate
            } else {
                requireContext().settings().shouldShowSearchSuggestions
            }

        searchStore = StoreProvider.get(this) {
            SearchFragmentStore(
                SearchFragmentState(
                    query = url,
                    searchEngineSource = currentSearchEngine,
                    defaultEngineSource = currentSearchEngine,
                    showSearchSuggestions = showSearchSuggestions,
                    showSearchSuggestionsHint = false,
                    showSearchShortcuts = requireContext().settings().shouldShowSearchShortcuts && url.isEmpty(),
                    showClipboardSuggestions = requireContext().settings().shouldShowClipboardSuggestions,
                    showHistorySuggestions = requireContext().settings().shouldShowHistorySuggestions,
                    showBookmarkSuggestions = requireContext().settings().shouldShowBookmarkSuggestions,
                    session = session,
                    pastedText = pastedText,
                    searchAccessPoint = searchAccessPoint,
                    isAnimatingOut = false
                )
            )
        }

        val searchController = DefaultSearchController(
            activity as HomeActivity,
            searchStore,
            findNavController()
        )

        searchInteractor = SearchInteractor(
            searchController
        )

        awesomeBarView = AwesomeBarView(view.search_layout, searchInteractor)

        toolbarView = ToolbarView(
            view.toolbar_component_wrapper,
            searchInteractor,
            historyStorageProvider(),
            requireComponents.browsingModeManager.mode.isPrivate
        )

        val urlView = toolbarView.view
            .findViewById<InlineAutocompleteEditText>(R.id.mozac_browser_toolbar_edit_url_view)
        urlView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        startPostponedEnterTransition()
        return view
    }

    @ExperimentalCoroutinesApi
    @SuppressWarnings("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        search_scan_button.visibility = if (context?.hasCamera() == true) View.VISIBLE else View.GONE

        qrFeature.set(
            QrFeature(
                requireContext(),
                fragmentManager = parentFragmentManager,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_CAMERA_PERMISSIONS)
                },
                onScanResult = { result ->
                    search_scan_button.isChecked = false
                    activity?.let {
                        AlertDialog.Builder(it).apply {
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
                                        newTab = searchStore.state.session == null,
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

        view.back_button.setOnClickListener {
            searchInteractor.onEditingCanceled()
        }

        val stubListener = ViewStub.OnInflateListener { _, inflated ->
            inflated.learn_more.setOnClickListener {
                (activity as HomeActivity)
                    .openToBrowserAndLoad(
                        searchTermOrURL = SupportUtils.getGenericSumoURLForTopic(
                            SupportUtils.SumoTopic.SEARCH_SUGGESTION
                        ),
                        newTab = searchStore.state.session == null,
                        from = BrowserDirection.FromSearch
                    )
            }

            inflated.allow.setOnClickListener {
                inflated.visibility = View.GONE
                context?.settings()?.shouldShowSearchSuggestionsInPrivate = true
                context?.settings()?.showSearchSuggestionsInPrivateOnboardingFinished = true
                requireComponents.analytics.metrics.track(Event.PrivateBrowsingShowSearchSuggestions)
            }

            inflated.dismiss.setOnClickListener {
                inflated.visibility = View.GONE
                context?.settings()?.shouldShowSearchSuggestionsInPrivate = false
                context?.settings()?.showSearchSuggestionsInPrivateOnboardingFinished = true
            }

            inflated.text.text =
                getString(R.string.search_suggestions_onboarding_text, getString(R.string.app_name))

            inflated.title.text =
                getString(R.string.search_suggestions_onboarding_title)
        }

        view.search_suggestions_onboarding.setOnInflateListener((stubListener))

        view.toolbar_wrapper.clipToOutline = false

        fill_link_from_clipboard.setOnClickListener {
            (activity as HomeActivity)
                .openToBrowserAndLoad(
                    searchTermOrURL = requireContext().components.clipboardHandler.url ?: "",
                    newTab = searchStore.state.session == null,
                    from = BrowserDirection.FromSearch
                )
        }

        consumeFrom(searchStore) {
            awesomeBarView.update(it)
            toolbarView.update(it)
            updateSearchWithLabel(it)
            updateClipboardSuggestion(it, requireContext().components.clipboardHandler.url)
            updateSearchSuggestionsHintVisibility(it)
            updateBackButton(it)
        }

        startPostponedEnterTransition()
    }

    override fun onResume() {
        super.onResume()

        // The user has the option to go to 'Shortcuts' -> 'Search engine settings' to modify the default search engine.
        // When returning from that settings screen we need to update it to account for any changes.
        val currentDefaultEngine =
            requireComponents.search.provider.getDefaultEngine(requireContext())

        if (searchStore.state.defaultEngineSource.searchEngine != currentDefaultEngine) {
            searchStore.dispatch(
                SearchFragmentAction.SelectNewDefaultSearchEngine
                    (currentDefaultEngine)
            )
        }

        if (!permissionDidUpdate) {
            toolbarView.view.requestFocus()
        }

        updateClipboardSuggestion(
            searchStore.state,
            requireContext().components.clipboardHandler.url
        )

        permissionDidUpdate = false
        hideToolbar()
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
            }
            else -> awesomeBarView.isKeyboardDismissedProgrammatically
        }
    }

    private fun updateSearchWithLabel(searchState: SearchFragmentState) {
        search_with_shortcuts.visibility =
            if (searchState.showSearchShortcuts) View.VISIBLE else View.GONE
    }

    private fun updateClipboardSuggestion(searchState: SearchFragmentState, clipboardUrl: String?) {
        val visibility =
            if (searchState.showClipboardSuggestions && searchState.query.isEmpty() && !clipboardUrl.isNullOrEmpty())
                View.VISIBLE else View.GONE

        fill_link_from_clipboard.visibility = visibility
        divider_line.visibility = visibility
        clipboard_url.text = clipboardUrl
    }

    private fun updateBackButton(searchState: SearchFragmentState) {
        if (searchState.isAnimatingOut) {
            searchStore.dispatch(SearchFragmentAction.ConsumeEditingCancelled)
            animateBackButtonAway()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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

    private fun historyStorageProvider(): HistoryStorage? {
        return if (requireContext().settings().shouldShowHistorySuggestions) {
            requireComponents.core.historyStorage
        } else null
    }

    private fun updateSearchSuggestionsHintVisibility(state: SearchFragmentState) {
        view?.apply {
            findViewById<View>(R.id.search_suggestions_onboarding)?.isVisible = state.showSearchSuggestionsHint

            search_suggestions_onboarding_divider?.isVisible =
                search_with_shortcuts.isVisible && state.showSearchSuggestionsHint
        }
    }

    private fun animateBackButtonAway() {
        val backButton = requireView().back_button
        val xTranslation = with(backButton) {
            -(width + marginStart + paddingStart).toFloat()
        }

        backButton
            .animate()
            .translationX(xTranslation)
            .interpolator = FastOutSlowInInterpolator()
    }

    companion object {
        private const val SHARED_TRANSITION_MS = 200L
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1
    }
}
