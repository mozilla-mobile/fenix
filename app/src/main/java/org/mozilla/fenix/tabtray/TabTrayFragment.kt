/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_tab_tray.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.media.ext.getSession
import mozilla.components.feature.media.state.MediaStateMachine
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.fenix.HomeActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.component_tab_tray.view.*
import kotlinx.android.synthetic.main.fragment_tab_tray.*
import mozilla.components.feature.media.ext.pauseIfPlaying
import mozilla.components.feature.media.ext.playIfPaused
import mozilla.components.feature.media.state.MediaState
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.collections.SaveCollectionStep
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.sessionsOfType
import org.mozilla.fenix.ext.setToolbarColors
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.home.BrowserSessionsObserver
import org.mozilla.fenix.home.PrivateBrowsingButtonView
import org.mozilla.fenix.utils.allowUndo

class TabTrayFragment : Fragment(), UserInteractionHandler {
    private lateinit var tabTrayView: TabTrayView
    private lateinit var tabTrayStore: TabTrayFragmentStore
    private lateinit var tabTrayInteractor: TabTrayInteractor
    private lateinit var tabTrayController: TabTrayController

    private var pendingSessionDeletion: PendingSessionDeletion? = null
    data class PendingSessionDeletion(val deletionJob: (suspend () -> Unit), val sessionIds: Set<String>)

    var snackbar: FenixSnackbar? = null

    var tabTrayMenu: Menu? = null

    private val sessionManager: SessionManager
        get() = requireComponents.core.sessionManager

    private val singleSessionObserver = object : Session.Observer {
        override fun onTitleChanged(session: Session, title: String) {
            if (pendingSessionDeletion == null) emitSessionChanges()
        }

        override fun onIconChanged(session: Session, icon: Bitmap?) {
            if (pendingSessionDeletion == null) emitSessionChanges()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val sessionObserver = BrowserSessionsObserver(sessionManager, singleSessionObserver) {
            tabTrayStore.dispatch(TabTrayFragmentAction.UpdateTabs(getListOfSessions().toTabs()))
        }

        lifecycle.addObserver(sessionObserver)
    }

    private fun getListOfSessions(): List<Session> {
        return sessionManager.sessionsOfType(private = (activity as HomeActivity).browsingModeManager.mode.isPrivate)
            .toList()
    }

    private fun share(tabs: List<Tab>) {
        val data = tabs.map {
            ShareData(url = it.url, title = it.title)
        }
        val directions = TabTrayFragmentDirections.actionGlobalShareFragment(
            data = data.toTypedArray()
        )
        nav(R.id.tabTrayFragment, directions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tab_tray, container, false)

        tabTrayStore = StoreProvider.get(this) {
            TabTrayFragmentStore(
                TabTrayFragmentState(
                    tabs = getListOfSessions().toTabs(),
                    mode = TabTrayFragmentState.Mode.Normal
                )
            )
        }

        tabTrayController = DefaultTabTrayController(
            browsingModeManager = (activity as HomeActivity).browsingModeManager,
            navController = findNavController(),
            sessionManager = sessionManager,
            tabTrayFragmentStore = tabTrayStore,
            tabCloser = ::tabCloser,
            onModeChange = { newMode ->
                invokePendingDeleteJobs()

                if (newMode == BrowsingMode.Private) {
                    requireContext().settings().incrementNumTimesPrivateModeOpened()
                }

                emitSessionChanges()
            }
        )

        tabTrayInteractor = TabTrayInteractor(tabTrayController)

        tabTrayView = TabTrayView(view.tab_tray_list_wrapper, tabTrayInteractor)

        return view
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sets the navigation icon callback action
        val toolbar = activity?.findViewById<Toolbar>(R.id.navigationToolbar)

        consumeFrom(tabTrayStore) {
            tabTrayView.update(it, (activity as HomeActivity).browsingModeManager.mode)

            // Set the title based on mode and number of selected tabs
            activity?.title = it.appBarTitle(requireContext())

            // Set title bar colors
            val (foregroundColor, backgroundColor) = it.appBarBackground(requireContext())
            toolbar?.setToolbarColors(foregroundColor, backgroundColor)

            // Sets the navigation icon to close
            val icon = resources.getDrawable(it.appBarIcon(), requireContext().theme)
            icon.setTint(foregroundColor)
            toolbar?.setNavigationIcon(icon)

            updateMenuItems()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tab_tray_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        this.tabTrayMenu = menu
        updateMenuItems()
    }

    override fun onBackPressed(): Boolean {
        if (tabTrayStore.state.mode is TabTrayFragmentState.Mode.Editing) {
            tabTrayStore.dispatch(TabTrayFragmentAction.ExitEditMode)
            return true
        }

        if (tabTrayStore.state.tabs.isEmpty()) {
            findNavController().popBackStack(R.id.homeFragment, false)
            return true
        }

        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_menu_item_save -> {
                share(tabTrayStore.state.mode.selectedTabs.toList())
                true
            }
            R.id.tab_tray_menu_item_save -> {
                tabTrayController.navigateToCollectionCreator()
                true
            }
            R.id.select_tabs_menu_item -> {
                tabTrayStore.dispatch(TabTrayFragmentAction.EnterEditMode)
                true
            }
            R.id.select_to_save_menu_item -> {
                tabTrayStore.dispatch(TabTrayFragmentAction.EnterEditMode)
                true
            }
            R.id.share_menu_item -> {
                share(tabTrayStore.state.tabs.toList())
                true
            }
            R.id.close_menu_item -> {
                tabTrayController.closeAllTabs()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun tabCloser(tabs: Sequence<Session>, isPrivate: Boolean) {
        val job = pendingSessionDeletion?.deletionJob ?: { }

        viewLifecycleOwner.lifecycleScope.launch {
            job()
        }.invokeOnCompletion {
            pendingSessionDeletion = null
            val sessionIdsToRemove = tabs.map { it.id }.toSet()

            val temporaryListOfTabs = tabTrayStore.state.tabs.filterNot {
                sessionIdsToRemove.contains(it.sessionId)
            }

            tabTrayStore.dispatch(TabTrayFragmentAction.UpdateTabs(temporaryListOfTabs))

            val deleteOperation: (suspend () -> Unit) = {
                tabs.forEach {
                    sessionManager.remove(it)
                }
            }

            pendingSessionDeletion = PendingSessionDeletion(deleteOperation, sessionIdsToRemove)

            val undoOperation: (suspend () -> Unit) = {
                if (isPrivate) {
                    requireComponents.analytics.metrics.track(Event.PrivateBrowsingSnackbarUndoTapped)
                }

                pendingSessionDeletion = null
                emitSessionChanges()
            }

            val snackbarMessage = when (Pair(sessionIdsToRemove.size > 1, isPrivate)) {
                Pair(first = true, second = true) -> getString(R.string.snackbar_private_tabs_closed)
                Pair(first = true, second = false) -> getString(R.string.snackbar_tabs_closed)
                Pair(first = false, second = true) -> getString(R.string.snackbar_private_tab_closed)
                Pair(first = false, second = false) -> getString(R.string.snackbar_tabs_closed)
                else -> getString(R.string.snackbar_tabs_closed)
            }

            tabTrayView.showUndoSnackbar(
                snackbarMessage,
                deleteOperation,
                undoOperation,
                viewLifecycleOwner
            )
        }
    }

    override fun onStart() {
        super.onStart()
        requireComponents.core.tabCollectionStorage.register(collectionStorageObserver, this)
    }

    override fun onStop() {
        invokePendingDeleteJobs()
        tabTrayView.hideSnackbar()
        // We only want this observer live just before we navigate away to the collection creation screen
        requireComponents.core.tabCollectionStorage.unregister(collectionStorageObserver)
        super.onStop()
    }

    private fun invokePendingDeleteJobs() {
        pendingSessionDeletion?.deletionJob?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                it.invoke()
            }.invokeOnCompletion {
                pendingSessionDeletion = null
            }
        }
    }

    private fun emitSessionChanges() {
        tabTrayStore.dispatch(TabTrayFragmentAction.UpdateTabs(getVisibleSessions().toTabs()))
    }

    private fun List<Session>.toTabs(): List<Tab> {
        val selected = sessionManager.selectedSession
        val mediaStateSession = MediaStateMachine.state.getSession()

        return this.map {
            val mediaState = if (mediaStateSession?.id == it.id) MediaStateMachine.state else MediaState.None
            it.toTab(requireContext(), it == selected, mediaState)
        }
    }

    private fun getVisibleSessions(): List<Session> {
        val pendingSessionIdsForRemoval = pendingSessionDeletion?.sessionIds ?: setOf()
        val sessions = getListOfSessions().filterNot { pendingSessionIdsForRemoval.contains(it.id) }

        return sessions
    }

    private fun showSavedSnackbar(tabSize: Int) {
        view?.let { view ->
            @StringRes
            val stringRes = if (tabSize > 1) {
                R.string.create_collection_tabs_saved
            } else {
                R.string.create_collection_tab_saved
            }
            FenixSnackbar.make(view, Snackbar.LENGTH_LONG)
                .setText(view.context.getString(stringRes))
                .show()
        }
    }

    private fun updateMenuItems() {
        val (foregroundColor, _) = tabTrayStore.state.appBarBackground(requireContext())
        val setupMenuIcon: (MenuItem) -> Unit = {
            it.isVisible = tabTrayStore.state.mode.isEditing
            it.isEnabled = tabTrayStore.state.mode.selectedTabs.isNotEmpty()
            it.icon.setTint(foregroundColor)
            it.icon.alpha = if (tabTrayStore.state.mode.selectedTabs.isNotEmpty()) {
                ICON_ENABLED_ALPHA
            } else {
                ICON_DISABLED_ALPHA
            }
        }
        val inPrivateMode = (activity as HomeActivity).browsingModeManager.mode.isPrivate

        // Shows the "save to collection menu item if in selection mode
        this.tabTrayMenu?.findItem(R.id.tab_tray_menu_item_save)?.also(setupMenuIcon)
        this.tabTrayMenu?.findItem(R.id.tab_tray_menu_item_save)?.isVisible =
            tabTrayStore.state.mode.isEditing && !inPrivateMode

        // Show the "share" button if in selection mode
        this.tabTrayMenu?.findItem(R.id.share_menu_item_save)?.also(setupMenuIcon)
        this.tabTrayMenu?.findItem(R.id.share_menu_item_save)?.isVisible = tabTrayStore.state.mode.isEditing

        // Hide all icons when in selection mode with nothing selected
        val showAnyOverflowIcons = !tabTrayStore.state.mode.isEditing && tabTrayStore.state.tabs.isNotEmpty()
        this.tabTrayMenu?.findItem(R.id.select_tabs_menu_item)?.isVisible = showAnyOverflowIcons
        this.tabTrayMenu?.findItem(R.id.select_to_save_menu_item)?.isVisible =
            showAnyOverflowIcons && !(activity as HomeActivity).browsingModeManager.mode.isPrivate
        this.tabTrayMenu?.findItem(R.id.share_menu_item)?.isVisible = showAnyOverflowIcons
        this.tabTrayMenu?.findItem(R.id.close_menu_item)?.isVisible = showAnyOverflowIcons

        // Disable the bottom trash icon when there are no tabs open
        if (tabTrayStore.state.tabs.isNotEmpty()) {
            view?.tab_tray_close_all?.alpha = CLOSE_ALL_ENABLED_ALPHA
            view?.tab_tray_close_all?.isEnabled = true
        } else {
            view?.tab_tray_close_all?.alpha = CLOSE_ALL_DISABLED_ALPHA
            view?.tab_tray_close_all?.isEnabled = true
        }
    }

    private val collectionStorageObserver = object : TabCollectionStorage.Observer {
        override fun onCollectionCreated(title: String, sessions: List<Session>) {
            emitSessionChanges()
            tabTrayStore.dispatch(TabTrayFragmentAction.ExitEditMode)
            showSavedSnackbar(sessions.size)
        }

        override fun onTabsAdded(tabCollection: TabCollection, sessions: List<Session>) {
            emitSessionChanges()
            tabTrayStore.dispatch(TabTrayFragmentAction.ExitEditMode)
            showSavedSnackbar(sessions.size)
        }
    }

    companion object {
        const val ICON_ENABLED_ALPHA = 255
        const val ICON_DISABLED_ALPHA = 102
        const val CLOSE_ALL_DISABLED_ALPHA = 0.4f
        const val CLOSE_ALL_ENABLED_ALPHA = 1.0f
    }
}
