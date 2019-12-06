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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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

import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.FenixSnackbarPresenter
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.logDebug
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.sessionsOfType
import org.mozilla.fenix.ext.setToolbarColors
import org.mozilla.fenix.ext.toTab
import org.mozilla.fenix.home.BrowserSessionsObserver
import org.mozilla.fenix.home.sessioncontrol.SessionControlChange
import org.mozilla.fenix.mvi.getManagedEmitter
import org.mozilla.fenix.utils.allowUndo


class TabTrayFragment : Fragment(), TabTrayInteractor {

    private lateinit var tabTrayView: TabTrayView
    private lateinit var tabTrayStore: TabTrayFragmentStore

    private var pendingSessionDeletion: PendingSessionDeletion? = null
    data class PendingSessionDeletion(val deletionJob: (suspend () -> Unit), val sessionId: String)

    var deleteAllSessionsJob: (suspend () -> Unit)? = null

    var snackbar: FenixSnackbar? = null

    private val sessionManager: SessionManager
        get() = requireComponents.core.sessionManager

    private val singleSessionObserver = object : Session.Observer {
        override fun onTitleChanged(session: Session, title: String) {
        }

        override fun onIconChanged(session: Session, icon: Bitmap?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val sessionObserver = BrowserSessionsObserver(sessionManager, singleSessionObserver) {
            tabTrayStore.dispatch(TabTrayFragmentAction.UpdateTabs(getListOfSessions()))
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tab_tray, container, false)

        tabTrayStore = StoreProvider.get(this) {
            TabTrayFragmentStore(
                TabTrayFragmentState(
                    tabs = getListOfSessions(),
                    mode = TabTrayFragmentState.Mode.Normal
                )
            )
        }

        tabTrayView = TabTrayView(view.tab_tray_list_wrapper, this)

        return view
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toggleEmptyMessage()

        consumeFrom(tabTrayStore) {
            tabTrayView.update(it)
            // activity?.title = it.appBarTitle(requireContext())
            val (foregroundColor, backgroundColor) = it.appBarBackground(requireContext())

            val toolbar = activity?.findViewById<Toolbar>(R.id.navigationToolbar)
            toolbar?.setToolbarColors(foregroundColor, backgroundColor)
        }
    }

    override fun onResume() {
        super.onResume()
        //activity?.title = getString(R.string.tab_tray_title)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tab_tray_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_menu_item -> {
                true
            }
            R.id.share_menu_item -> {
                // share(tabTrayStore.state.selecttoListedTabs.toList())
                share(tabTrayStore.state.tabs.toList())
                true
            }
            R.id.close_menu_item -> {
                val private = (activity as HomeActivity).browsingModeManager.mode.isPrivate
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
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun closeButtonTapped(tab: Tab) {
        if (pendingSessionDeletion?.deletionJob == null) {
            removeTabWithUndo(tab.id, (activity as HomeActivity).browsingModeManager.mode.isPrivate)
        } else {
            pendingSessionDeletion?.deletionJob?.let {
                viewLifecycleOwner.lifecycleScope.launch {
                    it.invoke()
                }.invokeOnCompletion {
                    pendingSessionDeletion = null
                    removeTabWithUndo(tab.id, (activity as HomeActivity).browsingModeManager.mode.isPrivate)
                }
            }
        }

        toggleEmptyMessage()
    }

    override fun open(item: Tab) {
        requireComponents.core.sessionManager.select(item)
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

    override fun onStop() {
        invokePendingDeleteJobs()
        snackbar?.dismiss()
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

        toggleEmptyMessage()
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
        tabTrayStore.dispatch(TabTrayFragmentAction.UpdateTabs(getVisibleSessions()))
        toggleEmptyMessage()
    }

    private fun List<Session>.toTabs(): List<org.mozilla.fenix.home.sessioncontrol.Tab> {
        val selected = sessionManager.selectedSession
        val mediaStateSession = MediaStateMachine.state.getSession()

        return this.map {
            val mediaState = if (mediaStateSession?.id == it.id) {
                MediaStateMachine.state
            } else {
                null
            }

            it.toTab(requireContext(), it == selected, mediaState)
        }
    }

    private fun toggleEmptyMessage() {
        // Show an empty message if no tabs are opened
        view?.tab_tray_empty_view?.visibility = if (getVisibleSessions().isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        view?.announceForAccessibility(context?.getString(R.string.no_open_tabs_description))
    }

    private fun getVisibleSessions(): List<Session> {
        if (deleteAllSessionsJob != null) {
            return emptyList()
        }
        val sessions = getListOfSessions().filterNot { it.id == pendingSessionDeletion?.sessionId }
        return sessions
    }
}
