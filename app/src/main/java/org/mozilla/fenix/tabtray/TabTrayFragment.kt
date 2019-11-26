package org.mozilla.fenix.tabtray

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_tab_tray.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.fenix.HomeActivity

import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.ext.logDebug
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.sessionsOfType
import org.mozilla.fenix.home.BrowserSessionsObserver
import org.mozilla.fenix.library.history.HistoryFragmentState


class TabTrayFragment : Fragment(), TabTrayInteractor {

    private lateinit var tabTrayView: TabTrayView
    private lateinit var tabTrayStore: TabTrayFragmentStore

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
            logDebug("boek", sessionManager.size.toString())
            tabTrayStore.dispatch(TabTrayFragmentAction.UpdateTabs(getListOfSessions()))
        }

        lifecycle.addObserver(sessionObserver)
    }

    private fun getListOfSessions(): List<Session> {
        return sessionManager.sessionsOfType(private = (activity as HomeActivity).browsingModeManager.mode.isPrivate)
            .toList()
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
                    tabs = getListOfSessions()
                )
            )
        }

        tabTrayView = TabTrayView(view.tab_tray_list_wrapper, this)

        return view
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consumeFrom(tabTrayStore) {
            logDebug("boek", it.tabs.toString())
            tabTrayView.update(it)
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.tab_tray_title)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        logDebug("dw-testing", "onCreateOptionsMenu!")
        inflater.inflate(R.menu.tab_tray_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_menu_item -> {
                logDebug("dw-testing", "SELECT!")
                true
            }
            R.id.share_menu_item -> {
                logDebug("dw-testing", "SHARE!")
                true
            }
            R.id.close_menu_item -> {
                logDebug("dw-testing", "CLOSE!")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun tabWasTapped(tab: Tab) {
        requireComponents.core.sessionManager.select(tab)
        val directions = TabTrayFragmentDirections.actionTabTrayFragmentToBrowserFragment(null)
        findNavController().navigate(directions)
    }

    override fun closeButtonTapped(tab: Tab) {
        requireComponents.core.sessionManager.remove(tab)
    }
}
