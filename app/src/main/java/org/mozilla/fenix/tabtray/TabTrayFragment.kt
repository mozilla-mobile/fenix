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
import kotlinx.android.synthetic.main.fragment_home.*
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
import com.google.android.material.snackbar.*
import kotlinx.android.synthetic.main.component_tab_tray.view.*
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

class TabTrayFragment : Fragment(), TabTrayInteractor, UserInteractionHandler {
    private lateinit var tabTrayView: TabTrayView
    private lateinit var tabTrayStore: TabTrayFragmentStore

    private var pendingSessionDeletion: PendingSessionDeletion? = null
    data class PendingSessionDeletion(val deletionJob: (suspend () -> Unit), val sessionId: String)

    var deleteAllSessionsJob: (suspend () -> Unit)? = null

    var snackbar: FenixSnackbar? = null

    var tabTrayMenu: Menu? = null

    private val sessionManager: SessionManager
        get() = requireComponents.core.sessionManager

    private val singleSessionObserver = object : Session.Observer {
        override fun onTitleChanged(session: Session, title: String) {
            if (deleteAllSessionsJob == null) emitSessionChanges()
        }

        override fun onIconChanged(session: Session, icon: Bitmap?) {
            if (deleteAllSessionsJob == null) emitSessionChanges()
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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tab_tray, container, false)

        tabTrayStore = StoreProvider.get(this) {
            TabTrayFragmentStore(
                TabTrayFragmentState(
                    tabs = getListOfSessions().toTabs(),
                    mode = TabTrayFragmentState.Mode.Normal
                )
            )
        }

        tabTrayView = TabTrayView(view.tab_tray_list_wrapper, this)

        PrivateBrowsingButtonView(
            view.privateBrowsingButton,
            (activity as HomeActivity).browsingModeManager
        ) { newMode ->
            invokePendingDeleteJobs()

            if (newMode == BrowsingMode.Private) {
                requireContext().settings().incrementNumTimesPrivateModeOpened()
            }

            // TODO:  This doesn't immediately trigger a tab change
            // Figure out how to make sure tabs switch based on the newly triggered mode
            emitSessionChanges()
        }

        view.tab_tray_close_all.setOnClickListener {
            closeAllTabs()
        }

        view.tab_tray_open_new_tab.setOnClickListener {
            invokePendingDeleteJobs()

            val directions = TabTrayFragmentDirections.actionTabTrayFragmentToSearchFragment(
                null
            )
            nav(R.id.tabTrayFragment, directions)
            requireComponents.analytics.metrics.track(Event.SearchBarTapped(Event.SearchBarTapped.Source.HOME))
        }

        return view
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sets the navigation icon callback action
        val toolbar = activity?.findViewById<Toolbar>(R.id.navigationToolbar)

        consumeFrom(tabTrayStore) {
            tabTrayView.update(it)
            toggleEmptyMessage(it.tabs.isEmpty())

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

        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_menu_item_save -> {
                share(tabTrayStore.state.mode.selectedTabs.toList())
                true
            }
            R.id.tab_tray_menu_item_save -> {
                showCollectionCreationFragment()
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
                tabTrayStore.dispatch(TabTrayFragmentAction.EnterEditMode)
                true
            }
            R.id.close_menu_item -> {
                closeAllTabs()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun closeAllTabs() {
        val private = (activity as HomeActivity).browsingModeManager.mode.isPrivate
        if (sessionManager.sessionsOfType(private = private).toList().isEmpty()) {
            return
        }

        if (pendingSessionDeletion?.deletionJob == null) {
            removeAllTabsWithUndo(
                sessionManager.sessionsOfType(private = private),
                private
            )
        } else {
            pendingSessionDeletion?.deletionJob?.let {
                viewLifecycleOwner.lifecycleScope.launch {
                    it.invoke()
                }.invokeOnCompletion {
                    pendingSessionDeletion = null
                    removeAllTabsWithUndo(
                        sessionManager.sessionsOfType(private = private),
                        private
                    )
                }
            }
        }
    }

    override fun closeButtonTapped(tab: Tab) {
        if (pendingSessionDeletion?.deletionJob == null) {
            removeTabWithUndo(tab.sessionId, (activity as HomeActivity).browsingModeManager.mode.isPrivate)
        } else {
            pendingSessionDeletion?.deletionJob?.let {
                viewLifecycleOwner.lifecycleScope.launch {
                    it.invoke()
                }.invokeOnCompletion {
                    pendingSessionDeletion = null
                    removeTabWithUndo(tab.sessionId, (activity as HomeActivity).browsingModeManager.mode.isPrivate)
                }
            }
        }
    }

    override fun open(item: Tab) {
        val session = sessionManager.findSessionById(item.sessionId) ?: return
        sessionManager.select(session)
        val directions = TabTrayFragmentDirections.actionTabTrayFragmentToBrowserFragment(null)
        findNavController().navigate(directions)
    }

    override fun select(item: Tab) {
        tabTrayStore.dispatch(
            TabTrayFragmentAction.SelectTab(item)
        )
    }

    override fun deselect(item: Tab) {
        tabTrayStore.dispatch(
            TabTrayFragmentAction.DeselectTab(item)
        )
    }

    override fun onStart() {
        super.onStart()
        // We only want this observer live just before we navigate away to the collection creation screen
        requireComponents.core.tabCollectionStorage.unregister(collectionStorageObserver)
    }

    override fun onStop() {
        invokePendingDeleteJobs()
        snackbar?.dismiss()
        super.onStop()
    }

    override fun shouldAllowSelect(): Boolean {
        return tabTrayStore.state.mode is TabTrayFragmentState.Mode.Editing
    }

    override fun onPauseMediaClicked() {
        MediaStateMachine.state.pauseIfPlaying()
    }

    override fun onPlayMediaClicked() {
        MediaStateMachine.state.playIfPaused()
    }

    private fun invokePendingDeleteJobs() {
        pendingSessionDeletion?.deletionJob?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                it.invoke()
            }.invokeOnCompletion {
                pendingSessionDeletion = null
            }
        }

        deleteAllSessionsJob?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                it.invoke()
            }.invokeOnCompletion {
                deleteAllSessionsJob = null
            }
        }
    }

    private fun removeAllTabsWithUndo(listOfSessionsToDelete: Sequence<Session>, private: Boolean) {
        val sessionManager = requireComponents.core.sessionManager

        tabTrayStore.dispatch(TabTrayFragmentAction.UpdateTabs(emptyList()))

        val deleteOperation: (suspend () -> Unit) = {
            listOfSessionsToDelete.forEach {
                sessionManager.remove(it)
            }
        }
        deleteAllSessionsJob = deleteOperation

        val snackbarMessage = if (private) {
            getString(R.string.snackbar_private_tabs_closed)
        } else {
            getString(R.string.snackbar_tabs_closed)
        }

        snackbar = viewLifecycleOwner.lifecycleScope.allowUndo(
            view!!,
            snackbarMessage,
            getString(R.string.snackbar_deleted_undo), {
                if (private) {
                    requireComponents.analytics.metrics.track(Event.PrivateBrowsingSnackbarUndoTapped)
                }
                deleteAllSessionsJob = null
                emitSessionChanges()
            },
            operation = deleteOperation,
            anchorView = bottom_bar
        )
    }

    private fun removeTabWithUndo(sessionId: String, private: Boolean) {
        val sessionManager = requireComponents.core.sessionManager

        val deleteOperation: (suspend () -> Unit) = {
            sessionManager.findSessionById(sessionId)
                ?.let { session ->
                    pendingSessionDeletion = null
                    sessionManager.remove(session)
                }
        }

        pendingSessionDeletion?.deletionJob?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                it.invoke()
            }
        }

        pendingSessionDeletion = TabTrayFragment.PendingSessionDeletion(deleteOperation, sessionId)

        val snackbarMessage = if (private) {
            getString(R.string.snackbar_private_tab_closed)
        } else {
            getString(R.string.snackbar_tab_closed)
        }

        snackbar = viewLifecycleOwner.lifecycleScope.allowUndo(
            view!!,
            snackbarMessage,
            getString(R.string.snackbar_deleted_undo), {
                pendingSessionDeletion = null
                emitSessionChanges()
            },
            operation = deleteOperation,
            anchorView = bottom_bar
        )

        // Update the UI with the tab removed, but don't remove it from storage yet
        emitSessionChanges()
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

    private fun toggleEmptyMessage(isVisible: Boolean) {
        // Show an empty message if no tabs are opened
        view?.tab_tray_empty_view?.isVisible = isVisible
        view?.announceForAccessibility(context?.getString(R.string.no_open_tabs_description))
    }

    private fun getVisibleSessions(): List<Session> {
        if (deleteAllSessionsJob != null) {
            return emptyList()
        }
        val sessions = getListOfSessions().filterNot { it.id == pendingSessionDeletion?.sessionId }
        return sessions
    }

    private fun showCollectionCreationFragment() {
        if (findNavController().currentDestination?.id == R.id.collectionCreationFragment) return
        if (tabTrayStore.state.mode is TabTrayFragmentState.Mode.Normal) return

        val tabIds = tabTrayStore.state.mode.selectedTabs.map { it.sessionId }.toTypedArray()

        requireComponents.core.tabCollectionStorage.register(collectionStorageObserver, this)

        view?.let {
            val directions = TabTrayFragmentDirections.actionTabTrayFragmentToCreateCollectionFragment(
                tabIds = tabIds,
                previousFragmentId = R.id.tabTrayFragment,
                saveCollectionStep = SaveCollectionStep.SelectCollection,
                selectedTabIds = tabIds,
                selectedTabCollectionId = -1
            )

            nav(R.id.tabTrayFragment, directions)
        }
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
        val showCollectionIcon = tabTrayStore.state.appBarShowCollectionIcon()

        // Shows the "save to collection menu item if in selection mode
        this.tabTrayMenu?.findItem(R.id.tab_tray_menu_item_save)?.apply {
            isVisible = showCollectionIcon
            isEnabled = tabTrayStore.state.mode.selectedTabs.isNotEmpty()
            getIcon().setTint(foregroundColor)
        }

        // Show the "share" button if in selection mode
        this.tabTrayMenu?.findItem(R.id.share_menu_item_save)?.apply {
            isVisible = showCollectionIcon
            isEnabled = tabTrayStore.state.mode.selectedTabs.isNotEmpty()
            getIcon().setTint(foregroundColor)
        }

        // Hide all icons when in selection mode with nothing selected
        val showAnyOverflowIcons = tabTrayStore.state.appBarShowIcon()
        this.tabTrayMenu?.findItem(R.id.select_tabs_menu_item)?.isVisible = showAnyOverflowIcons && !showCollectionIcon
        this.tabTrayMenu?.findItem(R.id.select_to_save_menu_item)?.isVisible = showAnyOverflowIcons && !showCollectionIcon
        this.tabTrayMenu?.findItem(R.id.share_menu_item)?.isVisible = showAnyOverflowIcons && !showCollectionIcon
        this.tabTrayMenu?.findItem(R.id.close_menu_item)?.isVisible = showAnyOverflowIcons && !showCollectionIcon
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
}
