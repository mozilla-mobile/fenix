/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.component_search.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.session.Session
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.feature.session.ThumbnailsFeature
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.readermode.DefaultReaderModeController
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.toolbar.BrowserInteractor
import org.mozilla.fenix.components.toolbar.BrowserToolbarController
import org.mozilla.fenix.components.toolbar.QuickActionSheetAction
import org.mozilla.fenix.customtabs.CustomTabsIntegration
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.home.sessioncontrol.SessionControlChange
import org.mozilla.fenix.home.sessioncontrol.TabCollection
import org.mozilla.fenix.mvi.getManagedEmitter
import org.mozilla.fenix.quickactionsheet.DefaultQuickActionSheetController
import org.mozilla.fenix.quickactionsheet.QuickActionSheetSessionObserver
import org.mozilla.fenix.quickactionsheet.QuickActionSheetView

/**
 * Fragment used for browsing the web within the main app and external apps.
 */
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@Suppress("TooManyFunctions", "LargeClass")
class BrowserFragment : BaseBrowserFragment(), BackHandler {
    private lateinit var quickActionSheetView: QuickActionSheetView
    private var quickActionSheetSessionObserver: QuickActionSheetSessionObserver? = null

    private val readerViewFeature = ViewBoundFeatureWrapper<ReaderViewFeature>()
    private val thumbnailsFeature = ViewBoundFeatureWrapper<ThumbnailsFeature>()
    private val customTabsIntegration = ViewBoundFeatureWrapper<CustomTabsIntegration>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(android.R.transition.move)
                .setDuration(
                    SHARED_TRANSITION_MS
                )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.browserLayout.transitionName = "$TAB_ITEM_TRANSITION_NAME${getSessionById()?.id}"

        startPostponedEnterTransition()

        return view
    }

    override fun initializeUI(view: View): Session? {
        val sessionManager = requireComponents.core.sessionManager

        return super.initializeUI(view)?.also {

            quickActionSheetView =
                QuickActionSheetView(view.nestedScrollQuickAction, browserInteractor)

            customTabSessionId?.let { customTabSessionId ->
                customTabsIntegration.set(
                    feature = CustomTabsIntegration(
                        requireContext(),
                        requireComponents.core.sessionManager,
                        toolbar,
                        customTabSessionId,
                        activity,
                        view.nestedScrollQuickAction,
                        view.swipeRefresh,
                        onItemTapped = { browserInteractor.onBrowserToolbarMenuItemTapped(it) }
                    ),
                    owner = this,
                    view = view)
            }

            thumbnailsFeature.set(
                feature = ThumbnailsFeature(
                    requireContext(),
                    view.engineView,
                    requireComponents.core.sessionManager
                ),
                owner = this,
                view = view
            )

            readerViewFeature.set(
                feature = ReaderViewFeature(
                    requireContext(),
                    requireComponents.core.engine,
                    requireComponents.core.sessionManager,
                    view.readerViewControlsBar
                ) { available ->
                    if (available) {
                        requireComponents.analytics.metrics.track(Event.ReaderModeAvailable)
                    }

                    browserStore.apply {
                        dispatch(QuickActionSheetAction.ReadableStateChange(available))
                        dispatch(
                            QuickActionSheetAction.ReaderActiveStateChange(
                                sessionManager.selectedSession?.readerMode ?: false
                            )
                        )
                    }
                },
                owner = this,
                view = view
            )

            if ((activity as HomeActivity).browsingModeManager.mode.isPrivate) {
                // We need to update styles for private mode programmatically for now:
                // https://github.com/mozilla-mobile/android-components/issues/3400
                themeReaderViewControlsForPrivateMode(view.readerViewControlsBar)
            }

            consumeFrom(browserStore) {
                quickActionSheetView.update(it)
                browserToolbarView.update(it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        subscribeToTabCollections()
        quickActionSheetSessionObserver = QuickActionSheetSessionObserver(
            lifecycleScope,
            requireComponents,
            dispatch = { action -> browserStore.dispatch(action) }
        ).also { observer ->
            getSessionById()?.register(observer, this, autoPause = true)
        }
    }

    override fun onResume() {
        super.onResume()
        getSessionById()?.let { quickActionSheetSessionObserver?.updateBookmarkState(it) }
        requireComponents.core.tabCollectionStorage.register(collectionStorageObserver, this)
    }

    override fun onBackPressed(): Boolean {
        return readerViewFeature.onBackPressed() || super.onBackPressed()
    }

    override fun removeSessionIfNeeded(): Boolean {
        if (customTabsIntegration.onBackPressed()) return true

        getSessionById()?.let { session ->
            if (session.source == Session.Source.ACTION_VIEW) requireComponents.core.sessionManager.remove(
                session
            )
        }
        return false
    }

    override fun createBrowserToolbarViewInteractor(
        browserToolbarController: BrowserToolbarController,
        session: Session?
    ) = BrowserInteractor(
        context = context!!,
        store = browserStore,
        browserToolbarController = browserToolbarController,
        quickActionSheetController = DefaultQuickActionSheetController(
            context = context!!,
            navController = findNavController(),
            currentSession = getSessionById()
                ?: requireComponents.core.sessionManager.selectedSessionOrThrow,
            appLinksUseCases = requireComponents.useCases.appLinksUseCases,
            bookmarkTapped = {
                lifecycleScope.launch { bookmarkTapped(it) }
            }
        ),
        readerModeController = DefaultReaderModeController(readerViewFeature),
        customTabSession = customTabSessionId?.let { requireComponents.core.sessionManager.findSessionById(it) }
    )

    override fun getEngineMargins(): Pair<Int, Int> {
        val toolbarAndQASSize = resources.getDimensionPixelSize(R.dimen.toolbar_and_qab_height)
        val toolbarSize = resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)
        return if (customTabSessionId != null) Pair(toolbarSize, 0) else Pair(0, toolbarAndQASSize)
    }

    override fun getAppropriateLayoutGravity() =
        if (customTabSessionId != null) Gravity.TOP else Gravity.BOTTOM

    private fun themeReaderViewControlsForPrivateMode(view: View) = with(view) {
        listOf(
            R.id.mozac_feature_readerview_font_size_decrease,
            R.id.mozac_feature_readerview_font_size_increase
        ).map {
            findViewById<Button>(it)
        }.forEach {
            it.setTextColor(
                ContextCompat.getColorStateList(
                    context,
                    R.color.readerview_private_button_color
                )
            )
        }

        listOf(
            R.id.mozac_feature_readerview_font_serif,
            R.id.mozac_feature_readerview_font_sans_serif
        ).map {
            findViewById<RadioButton>(it)
        }.forEach {
            it.setTextColor(
                ContextCompat.getColorStateList(
                    context,
                    R.color.readerview_private_radio_color
                )
            )
        }
    }

    private suspend fun bookmarkTapped(session: Session) = withContext(IO) {
        val bookmarksStorage = requireComponents.core.bookmarksStorage
        val existing =
            bookmarksStorage.getBookmarksWithUrl(session.url).firstOrNull { it.url == session.url }
        if (existing != null) {
            // Bookmark exists, go to edit fragment
            withContext(Main) {
                nav(
                    R.id.browserFragment,
                    BrowserFragmentDirections.actionBrowserFragmentToBookmarkEditFragment(existing.guid)
                )
            }
        } else {
            // Save bookmark, then go to edit fragment
            val guid = bookmarksStorage.addItem(
                BookmarkRoot.Mobile.id,
                url = session.url,
                title = session.title,
                position = null
            )

            withContext(Main) {
                browserStore.dispatch(
                    QuickActionSheetAction.BookmarkedStateChange(bookmarked = true)
                )
                requireComponents.analytics.metrics.track(Event.AddBookmark)

                view?.let {
                    FenixSnackbar.make(it.rootView, Snackbar.LENGTH_LONG)
                        .setAnchorView(browserToolbarView.view)
                        .setAction(getString(R.string.edit_bookmark_snackbar_action)) {
                            nav(
                                R.id.browserFragment,
                                BrowserFragmentDirections.actionBrowserFragmentToBookmarkEditFragment(
                                    guid
                                )
                            )
                        }
                        .setText(getString(R.string.bookmark_saved_snackbar))
                        .show()
                }
            }
        }
    }

    private fun subscribeToTabCollections() {
        requireComponents.core.tabCollectionStorage.getCollections().observe(this, Observer {
            requireComponents.core.tabCollectionStorage.cachedTabCollections = it
            getManagedEmitter<SessionControlChange>().onNext(
                SessionControlChange.CollectionsChange(
                    it
                )
            )
        })
    }

    override fun onSessionSelected(session: Session) {
        super.onSessionSelected(session)
        quickActionSheetSessionObserver?.updateBookmarkState(session)
    }

    private val collectionStorageObserver = object : TabCollectionStorage.Observer {
        override fun onCollectionCreated(title: String, sessions: List<Session>) {
            showTabSavedToCollectionSnackbar()
        }

        override fun onTabsAdded(tabCollection: TabCollection, sessions: List<Session>) {
            showTabSavedToCollectionSnackbar()
        }

        private fun showTabSavedToCollectionSnackbar() {
            view?.let { view ->
                FenixSnackbar.make(view, Snackbar.LENGTH_SHORT)
                    .setText(view.context.getString(R.string.create_collection_tab_saved))
                    .setAnchorView(browserToolbarView.view)
                    .show()
            }
        }
    }

    companion object {
        private const val SHARED_TRANSITION_MS = 200L
        private const val TAB_ITEM_TRANSITION_NAME = "tab_item"
        const val REPORT_SITE_ISSUE_URL =
            "https://webcompat.com/issues/new?url=%s&label=browser-fenix"
    }
}
