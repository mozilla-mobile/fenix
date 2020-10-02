/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.constraintlayout.widget.ConstraintProperties.BOTTOM
import androidx.constraintlayout.widget.ConstraintProperties.PARENT_ID
import androidx.constraintlayout.widget.ConstraintProperties.TOP
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_search_dialog.*
import kotlinx.android.synthetic.main.fragment_search_dialog.view.*
import kotlinx.android.synthetic.main.search_suggestions_hint.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.qr.QrFeature
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.content.hasCamera
import mozilla.components.support.ktx.android.content.isPermissionGranted
import mozilla.components.support.ktx.android.content.res.getSpanned
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.searchengine.CustomSearchEngineStore
import org.mozilla.fenix.components.searchengine.FenixSearchEngineProvider
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.isKeyboardVisible
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.search.awesomebar.AwesomeBarView
import org.mozilla.fenix.search.toolbar.ToolbarView
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.settings.registerOnSharedPreferenceChangeListener
import org.mozilla.fenix.widget.VoiceSearchActivity

typealias SearchDialogFragmentStore = SearchFragmentStore

@SuppressWarnings("LargeClass", "TooManyFunctions")
class SearchDialogFragment : AppCompatDialogFragment(), UserInteractionHandler {
    private lateinit var interactor: SearchDialogInteractor
    private lateinit var store: SearchDialogFragmentStore
    private lateinit var toolbarView: ToolbarView
    private lateinit var awesomeBarView: AwesomeBarView
    private var firstUpdate = true

    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()
    private val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

    private var keyboardVisible: Boolean = false

    override fun onStart() {
        super.onStart()
        // https://github.com/mozilla-mobile/fenix/issues/14279
        // To prevent GeckoView from resizing we're going to change the softInputMode to not adjust
        // the size of the window.
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        if (keyboardVisible) {
            toolbarView.view.edit.focus()
        }
    }

    override fun onStop() {
        super.onStop()
        // https://github.com/mozilla-mobile/fenix/issues/14279
        // Let's reset back to the default behavior after we're done searching
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        keyboardVisible = toolbarView.view.isKeyboardVisible()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.SearchDialogStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), this.theme) {
            override fun onBackPressed() {
                this@SearchDialogFragment.onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args by navArgs<SearchDialogFragmentArgs>()
        val view = inflater.inflate(R.layout.fragment_search_dialog, container, false)
        val activity = requireActivity() as HomeActivity
        val isPrivate = activity.browsingModeManager.mode.isPrivate

        requireComponents.analytics.metrics.track(Event.InteractWithSearchURLArea)

        store = SearchDialogFragmentStore(
            createInitialSearchFragmentState(
                activity,
                requireComponents,
                tabId = args.sessionId,
                pastedText = args.pastedText,
                searchAccessPoint = args.searchAccessPoint
            )
        )

        interactor = SearchDialogInteractor(
            SearchDialogController(
                activity = activity,
                sessionManager = requireComponents.core.sessionManager,
                store = store,
                navController = findNavController(),
                settings = requireContext().settings(),
                metrics = requireComponents.analytics.metrics,
                dismissDialog = { dismissAllowingStateLoss() },
                clearToolbarFocus = {
                    toolbarView.view.hideKeyboardAndSave()
                    toolbarView.view.clearFocus()
                }
            )
        )

        toolbarView = ToolbarView(
            requireContext(),
            interactor,
            historyStorageProvider(),
            isPrivate,
            view.toolbar,
            requireComponents.core.engine
        ).also(::addSearchButton)

        awesomeBarView = AwesomeBarView(
            activity,
            interactor,
            view.awesome_bar
        )

        setShortcutsChangedListener(CustomSearchEngineStore.PREF_FILE_SEARCH_ENGINES)
        setShortcutsChangedListener(FenixSearchEngineProvider.PREF_FILE_SEARCH_ENGINES)

        view.awesome_bar.setOnTouchListener { _, _ ->
            view.hideKeyboardAndSave()
            false
        }

        awesomeBarView.view.setOnEditSuggestionListener(toolbarView.view::setSearchTerms)

        val urlView = toolbarView.view
            .findViewById<InlineAutocompleteEditText>(R.id.mozac_browser_toolbar_edit_url_view)
        urlView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        requireComponents.core.engine.speculativeCreateSession(isPrivate)

        return view
    }

    @ExperimentalCoroutinesApi
    @SuppressWarnings("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupConstraints(view)

        search_wrapper.setOnClickListener {
            it.hideKeyboardAndSave()
            dismissAllowingStateLoss()
        }

        view.search_engines_shortcut_button.setOnClickListener {
            interactor.onSearchShortcutsButtonClicked()
        }

        qrFeature.set(
            createQrFeature(),
            owner = this,
            view = view
        )

        qr_scan_button.visibility = if (context?.hasCamera() == true) View.VISIBLE else View.GONE

        qr_scan_button.setOnClickListener {
            if (!requireContext().hasCamera()) { return@setOnClickListener }
            view.hideKeyboard()
            toolbarView.view.clearFocus()

            if (requireContext().settings().shouldShowCameraPermissionPrompt) {
                requireComponents.analytics.metrics.track(Event.QRScannerOpened)
                qrFeature.get()?.scan(R.id.search_wrapper)
            } else {
                if (requireContext().isPermissionGranted(Manifest.permission.CAMERA)) {
                    requireComponents.analytics.metrics.track(Event.QRScannerOpened)
                    qrFeature.get()?.scan(R.id.search_wrapper)
                } else {
                    interactor.onCameraPermissionsNeeded()
                    resetFocus()
                    view.hideKeyboard()
                    toolbarView.view.requestFocus()
                }
            }
            requireContext().settings().setCameraPermissionNeededState = false
        }

        fill_link_from_clipboard.setOnClickListener {
            view.hideKeyboard()
            toolbarView.view.clearFocus()
            (activity as HomeActivity)
                .openToBrowserAndLoad(
                    searchTermOrURL = requireContext().components.clipboardHandler.url ?: "",
                    newTab = store.state.tabId == null,
                    from = BrowserDirection.FromSearchDialog
                )
        }

        val stubListener = ViewStub.OnInflateListener { _, inflated ->
            inflated.learn_more.setOnClickListener {
                (activity as HomeActivity)
                    .openToBrowserAndLoad(
                        searchTermOrURL = SupportUtils.getGenericSumoURLForTopic(
                            SupportUtils.SumoTopic.SEARCH_SUGGESTION
                        ),
                        newTab = store.state.tabId == null,
                        from = BrowserDirection.FromSearchDialog
                    )
            }

            inflated.allow.setOnClickListener {
                inflated.visibility = View.GONE
                requireContext().settings().also {
                    it.shouldShowSearchSuggestionsInPrivate = true
                    it.showSearchSuggestionsInPrivateOnboardingFinished = true
                }
                store.dispatch(SearchFragmentAction.SetShowSearchSuggestions(true))
                store.dispatch(SearchFragmentAction.AllowSearchSuggestionsInPrivateModePrompt(false))
                requireComponents.analytics.metrics.track(Event.PrivateBrowsingShowSearchSuggestions)
            }

            inflated.dismiss.setOnClickListener {
                inflated.visibility = View.GONE
                requireContext().settings().also {
                    it.shouldShowSearchSuggestionsInPrivate = false
                    it.showSearchSuggestionsInPrivateOnboardingFinished = true
                }
            }

            inflated.text.text =
                getString(R.string.search_suggestions_onboarding_text, getString(R.string.app_name))

            inflated.title.text =
                getString(R.string.search_suggestions_onboarding_title)
        }

        view.search_suggestions_hint.setOnInflateListener((stubListener))

        consumeFrom(store) {
            val shouldShowAwesomebar =
                !firstUpdate &&
                it.query.isNotBlank() ||
                it.showSearchShortcuts

            awesome_bar?.visibility = if (shouldShowAwesomebar) View.VISIBLE else View.INVISIBLE
            updateSearchSuggestionsHintVisibility(it)
            updateClipboardSuggestion(it, requireContext().components.clipboardHandler.url)
            updateToolbarContentDescription(it)
            updateSearchShortcutsIcon(it)
            toolbarView.update(it)
            awesomeBarView.update(it)
            firstUpdate = false
        }
    }

    override fun onResume() {
        super.onResume()
        resetFocus()
        toolbarView.view.edit.focus()
    }

    override fun onPause() {
        super.onPause()
        qr_scan_button.isChecked = false
        view?.hideKeyboard()
        toolbarView.view.requestFocus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == VoiceSearchActivity.SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.first()?.also {
                toolbarView.view.edit.updateUrl(url = it, shouldHighlight = true)
                interactor.onTextChanged(it)
                toolbarView.view.edit.focus()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return when {
            qrFeature.onBackPressed() -> {
                resetFocus()
                true
            }
            else -> {
                view?.hideKeyboardAndSave()
                dismissAllowingStateLoss()
                true
            }
        }
    }

    private fun historyStorageProvider(): HistoryStorage? {
        return if (requireContext().settings().shouldShowHistorySuggestions) {
            requireComponents.core.historyStorage
        } else null
    }

    private fun createQrFeature(): QrFeature {
        return QrFeature(
            requireContext(),
            fragmentManager = childFragmentManager,
            onNeedToRequestPermissions = { permissions ->
                requestPermissions(permissions, REQUEST_CODE_CAMERA_PERMISSIONS)
            },
            onScanResult = { result ->
                qr_scan_button.isChecked = false
                activity?.let {
                    AlertDialog.Builder(it).apply {
                        val spannable = resources.getSpanned(
                            R.string.qr_scanner_confirmation_dialog_message,
                            getString(R.string.app_name) to StyleSpan(Typeface.BOLD),
                            result to StyleSpan(Typeface.ITALIC)
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
                                    newTab = store.state.tabId == null,
                                    from = BrowserDirection.FromSearchDialog
                                )
                            dialog.dismiss()
                        }
                        create()
                    }.show()
                    requireComponents.analytics.metrics.track(Event.QRScannerPromptDisplayed)
                }
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSIONS -> qrFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
                resetFocus()
                requireContext().settings().setCameraPermissionNeededState = false
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun resetFocus() {
        qr_scan_button.isChecked = false
        toolbarView.view.edit.focus()
        toolbarView.view.requestFocus()
    }

    private fun setupConstraints(view: View) {
        if (view.context.settings().toolbarPosition == ToolbarPosition.BOTTOM) {
            ConstraintSet().apply {
                clone(search_wrapper)

                clear(toolbar.id, TOP)
                connect(toolbar.id, BOTTOM, PARENT_ID, BOTTOM)

                clear(pill_wrapper.id, BOTTOM)
                connect(pill_wrapper.id, BOTTOM, toolbar.id, TOP)

                clear(search_suggestions_hint.id, TOP)
                connect(search_suggestions_hint.id, TOP, PARENT_ID, TOP)

                clear(fill_link_from_clipboard.id, TOP)
                connect(fill_link_from_clipboard.id, BOTTOM, pill_wrapper.id, TOP)

                applyTo(search_wrapper)
            }
        }
    }

    private fun updateSearchSuggestionsHintVisibility(state: SearchFragmentState) {
        view?.apply {
            val showHint = state.showSearchSuggestionsHint && !state.showSearchShortcuts
            findViewById<View>(R.id.search_suggestions_hint)?.isVisible = showHint
            search_suggestions_hint_divider?.isVisible = showHint
        }
    }

    private fun addSearchButton(toolbarView: ToolbarView) {
        toolbarView.view.addEditAction(
            BrowserToolbar.Button(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_microphone)!!,
                requireContext().getString(R.string.voice_search_content_description),
                visible = {
                    store.state.searchEngineSource.searchEngine.identifier.contains("google") &&
                            isSpeechAvailable() &&
                            requireContext().settings().shouldShowVoiceSearch
                },
                listener = ::launchVoiceSearch
            )
        )
    }

    /**
     * Used to save keyboard status on stop/sleep, to be restored later.
     * See #14559
     * */
    private fun View.hideKeyboardAndSave() {
        keyboardVisible = false
        this.hideKeyboard()
    }

    private fun launchVoiceSearch() {
        // Note if a user disables speech while the app is on the search fragment
        // the voice button will still be available and *will* cause a crash if tapped,
        // since the `visible` call is only checked on create. In order to avoid extra complexity
        // around such a small edge case, we make the button have no functionality in this case.
        if (!isSpeechAvailable()) { return }

        requireComponents.analytics.metrics.track(Event.VoiceSearchTapped)
        speechIntent.apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, requireContext().getString(R.string.voice_search_explainer))
        }
        startActivityForResult(speechIntent, VoiceSearchActivity.SPEECH_REQUEST_CODE)
    }

    private fun isSpeechAvailable(): Boolean = speechIntent.resolveActivity(requireContext().packageManager) != null

    private fun setShortcutsChangedListener(preferenceFileName: String) {
        requireContext().getSharedPreferences(
            preferenceFileName,
            Context.MODE_PRIVATE
        ).registerOnSharedPreferenceChangeListener(viewLifecycleOwner) { _, _ ->
            awesomeBarView.update(store.state)
        }
    }

    private fun updateClipboardSuggestion(searchState: SearchFragmentState, clipboardUrl: String?) {
        val shouldShowView = searchState.showClipboardSuggestions &&
                searchState.query.isEmpty() &&
                !clipboardUrl.isNullOrEmpty() &&
                !searchState.showSearchShortcuts

        fill_link_from_clipboard.visibility = if (shouldShowView) View.VISIBLE else View.GONE
        clipboard_url.text = clipboardUrl

        if (clipboardUrl != null && !((activity as HomeActivity).browsingModeManager.mode.isPrivate)) {
            requireComponents.core.engine.speculativeConnect(clipboardUrl)
        }
    }

    private fun updateToolbarContentDescription(searchState: SearchFragmentState) {
        val urlView = toolbarView.view
            .findViewById<InlineAutocompleteEditText>(R.id.mozac_browser_toolbar_edit_url_view)
        toolbarView.view.contentDescription =
            searchState.searchEngineSource.searchEngine.name + ", " + urlView.hint
        urlView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun updateSearchShortcutsIcon(searchState: SearchFragmentState) {
        view?.apply {
            search_engines_shortcut_button.isVisible = searchState.areShortcutsAvailable

            val showShortcuts = searchState.showSearchShortcuts
            search_engines_shortcut_button.isChecked = showShortcuts

            val color = if (showShortcuts) R.attr.contrastText else R.attr.primaryText
            search_engines_shortcut_button.compoundDrawables[0]?.setTint(
                requireContext().getColorFromAttr(color)
            )
        }
    }

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1
    }
}
