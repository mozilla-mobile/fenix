/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.component_tabstray.view.*
import kotlinx.android.synthetic.main.component_tabstray_fab.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_tab_tray_dialog.*
import kotlinx.android.synthetic.main.fragment_tab_tray_dialog.view.*
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.utils.allowUndo

@SuppressWarnings("TooManyFunctions", "LargeClass")
class TabTrayDialogFragment : AppCompatDialogFragment() {
    private val tabsFeature = ViewBoundFeatureWrapper<TabsFeature>()
    private var _tabTrayView: TabTrayView? = null
    private val tabTrayView: TabTrayView
        get() = _tabTrayView!!

    private val snackbarAnchor: View?
        get() = if (tabTrayView.fabView.new_tab_button.isVisible) tabTrayView.fabView.new_tab_button
                else null

    private val collectionStorageObserver = object : TabCollectionStorage.Observer {
        override fun onCollectionCreated(title: String, sessions: List<Session>) {
            showCollectionSnackbar(sessions.size, true)
        }

        override fun onTabsAdded(tabCollection: TabCollection, sessions: List<Session>) {
            showCollectionSnackbar(sessions.size)
        }
    }

    private val selectTabUseCase = object : TabsUseCases.SelectTabUseCase {
        override fun invoke(tabId: String) {
            requireContext().components.analytics.metrics.track(Event.OpenedExistingTab)
            requireComponents.useCases.tabsUseCases.selectTab(tabId)
            navigateToBrowser()
        }

        override fun invoke(session: Session) {
            requireContext().components.analytics.metrics.track(Event.OpenedExistingTab)
            requireComponents.useCases.tabsUseCases.selectTab(session)
            navigateToBrowser()
        }
    }

    var sessionIdToRemove: SessionManager.Snapshot.Item? = null

    private val removeTabUseCase = object : TabsUseCases.RemoveTabUseCase {
        override fun invoke(sessionId: String) {
            requireContext().components.analytics.metrics.track(Event.ClosedExistingTab)

            Log.d("Sawyer", "showUndo called now....")

            sessionIdToRemove = requireComponents.core.sessionManager.findSessionById(sessionId)?.let {
                    requireComponents.core.sessionManager.createSessionSnapshot(it)
                } ?: return

            requireComponents.useCases.tabsUseCases.removeTab(sessionId)

            // TODO: Could just store the sessionId?  then showUndo later.


            // If the tab tray is now empty, dismiss
            if (requireComponents.core.sessionManager.sessions.isEmpty()) {
                Log.d("Sawyer", "isEmpty!")
                requireContext().components.analytics.metrics.track(Event.TabsTrayClosed)
                dismissAllowingStateLoss()
            }

        }

        override fun invoke(session: Session) {
            requireContext().components.analytics.metrics.track(Event.ClosedExistingTab)
            //showUndoSnackbarForTab(session.id)
            requireComponents.useCases.tabsUseCases.removeTab(session)

            // If the tab tray is now empty, dismiss
//            if (requireComponents.core.sessionManager.sessions.isEmpty()) {
//                requireContext().components.analytics.metrics.track(Event.TabsTrayClosed)
//                dismissAllowingStateLoss()
//            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TabTrayDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_tab_tray_dialog, container, false)

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        tabTrayView.setTopOffset(isLandscape)

        if (isLandscape) {
            tabTrayView.dismissMenu()
            tabTrayView.expand()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isPrivate = (activity as HomeActivity).browsingModeManager.mode.isPrivate

        _tabTrayView = TabTrayView(
            view.tabLayout,
            interactor = TabTrayFragmentInteractor(
                DefaultTabTrayController(
                    activity = (activity as HomeActivity),
                    navController = findNavController(),
                    dismissTabTray = ::dismissAllowingStateLoss,
                    showUndoSnackbar = ::showUndoSnackbar,
                    registerCollectionStorageObserver = ::registerCollectionStorageObserver
                )
            ),
            isPrivate = isPrivate,
            startingInLandscape = requireContext().resources.configuration.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE,
            lifecycleScope = viewLifecycleOwner.lifecycleScope
        ) { tabsFeature.get()?.filterTabs(it) }

        tabsFeature.set(
            TabsFeature(
                tabTrayView.view.tabsTray,
                view.context.components.core.store,
                selectTabUseCase,
                removeTabUseCase,
                { it.content.private == isPrivate },
                { }
            ),
            owner = viewLifecycleOwner,
            view = view
        )

        tabLayout.setOnClickListener {
            requireContext().components.analytics.metrics.track(Event.TabsTrayClosed)
            dismissAllowingStateLoss()
        }

        view.tabLayout.setOnApplyWindowInsetsListener { v, insets ->
            v.updatePadding(
                left = insets.systemWindowInsetLeft,
                right = insets.systemWindowInsetRight,
                bottom = insets.systemWindowInsetBottom
            )

            tabTrayView.view.tab_wrapper.updatePadding(
                bottom = insets.systemWindowInsetBottom
            )

            insets
        }

        consumeFrom(requireComponents.core.store) {
            tabTrayView.updateState(it)

            navigateHomeIfNeeded(it)
        }
    }

    private fun showUndoSnackbarForTab(snapshot: SessionManager.Snapshot.Item) {
        Log.d("Sawyer", "for tab")
        val sessionManager = view?.context?.components?.core?.sessionManager
        // Ah this early return is fucking me.
//        val snapshot = sessionManager
//            ?.findSessionById(sessionId)?.let {
//                sessionManager.createSessionSnapshot(it)
//            } ?: return

        val state = snapshot.engineSession?.saveState()
            //val isSelected = sessionId == requireComponents.core.store.state.selectedTabId ?: false

        val snackbarMessage = if (snapshot.session.private) {
            getString(R.string.snackbar_private_tab_closed)
        } else {
            getString(R.string.snackbar_tab_closed)
        }

        // This works for when you're *on* the homescreen. Really I want to ensure that they're on the homescreen first by navving.
        //view?.tabLayout?.let {
            requireActivity().lifecycleScope.allowUndo(
                requireActivity().getRootView()!!,
                snackbarMessage,
                getString(R.string.snackbar_deleted_undo),
                {
                    sessionManager!!.add(snapshot.session, false, engineSessionState = state)
                    //tabTrayView.scrollToTab(snapshot.session.id)
                },
                operation = { },
                elevation = ELEVATION,
                anchorView = toolbarLayout
            )
       // }
    }

    override fun onDestroyView() {
        _tabTrayView = null
        super.onDestroyView()
    }

    fun navigateToBrowser() {
        dismissAllowingStateLoss()
        if (findNavController().currentDestination?.id == R.id.browserFragment) return
        if (!findNavController().popBackStack(R.id.browserFragment, false)) {
            findNavController().navigate(R.id.browserFragment)
        }
    }

    private fun navigateHomeIfNeeded(state: BrowserState) {
        val shouldPop = if (tabTrayView.isPrivateModeSelected) {
            state.privateTabs.isEmpty()
        } else {
            state.normalTabs.isEmpty()
        }

        if (shouldPop) {
            Log.d("Sawyer", "naving home!")
            findNavController().popBackStack(R.id.homeFragment, false)
            sessionIdToRemove?.let {
                Log.d("Sawyer", "let")
                showUndoSnackbarForTab(it)
            }
        }
    }

    private fun registerCollectionStorageObserver() {
        requireComponents.core.tabCollectionStorage.register(collectionStorageObserver, this)
    }

    private fun showUndoSnackbar(snackbarMessage: String, snapshot: SessionManager.Snapshot) {
        // TODO: I think basically just don't anchor the snackbar since we want to display it on a different fragment.
        Log.d("Sawyer", "regular")

        // Hmm still doesn't work without an anchor? See what BaseBrowserFrag does.


        FenixSnackbar.make(
            view = requireActivity().getRootView()!!,
            isDisplayedWithBrowserToolbar = false
        ).setText("Hello")
            .show()



        view?.let {
            viewLifecycleOwner.lifecycleScope.allowUndo(
                it,
                snackbarMessage,
                getString(R.string.snackbar_deleted_undo),
                {
                    context?.components?.core?.sessionManager?.restore(snapshot)
                },
                operation = { },
                elevation = ELEVATION,
                    anchorView = requireParentFragment().requireActivity().getRootView()

//                anchorView = snackbarAnchor
            )
        }
    }

    private fun showCollectionSnackbar(tabSize: Int, isNewCollection: Boolean = false) {
        view.let {
            val messageStringRes = when {
                isNewCollection -> {
                    R.string.create_collection_tabs_saved_new_collection
                }
                tabSize > 1 -> {
                    R.string.create_collection_tabs_saved
                }
                else -> {
                    R.string.create_collection_tab_saved
                }
            }
            val snackbar = FenixSnackbar
                .make(
                    duration = FenixSnackbar.LENGTH_LONG,
                    isDisplayedWithBrowserToolbar = true,
                    view = (view as View)
                )
                .setAnchorView(snackbarAnchor)
                .setText(requireContext().getString(messageStringRes))
                .setAction(requireContext().getString(R.string.create_collection_view)) {
                    dismissAllowingStateLoss()
                    findNavController().navigate(
                        TabTrayDialogFragmentDirections.actionGlobalHome(focusOnAddressBar = false)
                    )
                }

            snackbar.view.elevation = ELEVATION
            snackbar.show()
        }
    }

    companion object {
        private const val ELEVATION = 80f
        private const val FRAGMENT_TAG = "tabTrayDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            // If we've killed the fragmentManager. Let's not try to show the tabs tray.
            if (fragmentManager.isDestroyed) {
                return
            }

            // We want to make sure we don't accidentally show the dialog twice if
            // a user somehow manages to trigger `show()` twice before we present the dialog.
            if (fragmentManager.findFragmentByTag(FRAGMENT_TAG) == null) {
                TabTrayDialogFragment().showNow(fragmentManager, FRAGMENT_TAG)
            }
        }
    }
}
