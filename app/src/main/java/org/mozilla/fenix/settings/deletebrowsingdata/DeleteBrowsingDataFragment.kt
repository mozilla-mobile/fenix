/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.deletebrowsingdata

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_delete_browsing_data.*
import kotlinx.android.synthetic.main.fragment_delete_browsing_data.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.home.HomeFragmentDirections

@SuppressWarnings("TooManyFunctions")
class DeleteBrowsingDataFragment : Fragment(R.layout.fragment_delete_browsing_data) {
    private lateinit var sessionObserver: SessionManager.Observer
    private lateinit var controller: DeleteBrowsingDataController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller = DefaultDeleteBrowsingDataController(requireContext())

        sessionObserver = object : SessionManager.Observer {
            override fun onSessionAdded(session: Session) = updateTabCount()
            override fun onSessionRemoved(session: Session) = updateTabCount()
            override fun onSessionSelected(session: Session) = updateTabCount()
            override fun onSessionsRestored() = updateTabCount()
            override fun onAllSessionsRemoved() = updateTabCount()
        }

        requireComponents.core.sessionManager.register(sessionObserver, owner = this)
        getCheckboxes().forEach {
            it.onCheckListener = { _ -> updateDeleteButton() }
        }

        getCheckboxes().forEach { it.isChecked = true }

        view.delete_data?.setOnClickListener {
            askToDelete()
        }
    }

    private fun updateDeleteButton() {
        val enabled = getCheckboxes().any { it.isChecked }

        view?.delete_data?.isEnabled = enabled
        view?.delete_data?.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_delete_browsing_data))

        getCheckboxes().forEach {
            it.visibility = View.VISIBLE
        }

        updateItemCounts()
    }

    private fun askToDelete() {
        context?.let {
            AlertDialog.Builder(it).apply {
                setMessage(
                    it.getString(
                        R.string.delete_browsing_data_prompt_message_3,
                        it.getString(R.string.app_name)
                    )
                )

                setNegativeButton(R.string.delete_browsing_data_prompt_cancel) { it: DialogInterface, _ ->
                    it.cancel()
                }

                setPositiveButton(R.string.delete_browsing_data_prompt_allow) { it: DialogInterface, _ ->
                    it.dismiss()
                    deleteSelected()
                }
                create()
            }.show()
        }
    }

    private fun deleteSelected() {
        startDeletion()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            getCheckboxes().mapIndexed { i, v ->
                if (v.isChecked) {
                    when (i) {
                        OPEN_TABS_INDEX -> controller.deleteTabs()
                        HISTORY_INDEX -> controller.deleteBrowsingData()
                        COOKIES_INDEX -> controller.deleteCookies()
                        CACHED_INDEX -> controller.deleteCachedFiles()
                        PERMS_INDEX -> controller.deleteSitePermissions()
                    }
                }
            }

            launch(Dispatchers.Main) {
                finishDeletion()
                requireComponents.analytics.metrics.track(Event.ClearedPrivateData)
            }
        }
    }

    private fun startDeletion() {
        progress_bar.visibility = View.VISIBLE
        delete_browsing_data_wrapper.isEnabled = false
        delete_browsing_data_wrapper.isClickable = false
        delete_browsing_data_wrapper.alpha = DISABLED_ALPHA
    }

    private fun finishDeletion() {
        val popAfter = open_tabs_item.isChecked
        progress_bar.visibility = View.GONE
        delete_browsing_data_wrapper.isEnabled = true
        delete_browsing_data_wrapper.isClickable = true
        delete_browsing_data_wrapper.alpha = ENABLED_ALPHA

        getCheckboxes().forEach {
            it.isChecked = false
        }

        updateItemCounts()

        FenixSnackbar.makeWithToolbarPadding(requireView(), FenixSnackbar.LENGTH_SHORT)
            .setText(resources.getString(R.string.preferences_delete_browsing_data_snackbar))
            .show()

        if (popAfter) viewLifecycleOwner.lifecycleScope.launch(
            Dispatchers.Main
        ) {
            returnToDeletionOrigin()
        }
    }

    override fun onPause() {
        super.onPause()
        progress_bar.visibility = View.GONE
    }

    private fun updateItemCounts() {
        updateTabCount()
        updateHistoryCount()
        updateCookies()
        updateCachedImagesAndFiles()
        updateSitePermissions()
    }

    private fun updateTabCount() {
        view?.open_tabs_item?.apply {
            val openTabs = requireComponents.core.sessionManager.sessions.size
            subtitleView.text = resources.getString(
                R.string.preferences_delete_browsing_data_tabs_subtitle,
                openTabs
            )
        }
    }

    private fun updateHistoryCount() {
        view?.browsing_data_item?.subtitleView?.text = ""

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val historyCount = requireComponents.core.historyStorage.getVisited().size
            launch(Dispatchers.Main) {
                view?.browsing_data_item?.apply {
                    subtitleView.text =
                        resources.getString(
                            R.string.preferences_delete_browsing_data_browsing_data_subtitle,
                            historyCount
                        )
                }
            }
        }
    }

    private fun updateCookies() {
        // NO OP until we have GeckoView methods to count cookies
    }

    private fun updateCachedImagesAndFiles() {
        // NO OP until we have GeckoView methods to count cached images and files
    }

    private fun updateSitePermissions() {
        // NO OP until we have GeckoView methods for cookies and cached files, for consistency
    }

    private fun getCheckboxes(): List<DeleteBrowsingDataItem> {
        val fragmentView = view!!
        return listOf(
            fragmentView.open_tabs_item,
            fragmentView.browsing_data_item,
            fragmentView.cookies_item,
            fragmentView.cached_files_item,
            fragmentView.site_permissions_item
        )
    }

    // If coming from Open Tab -> Settings, pop back to Home
    // If coming from Home -> Settings, pop back to Settings
    private fun returnToDeletionOrigin() {
        // If Delete browsing data fragment isn't in the backstack
        // then Android may have changed their naming convention
        // and we want to prevent a crash by defaulting to pop home behavior
        if (checkIfFragmentInBackStack(R.id.deleteBrowsingDataFragment)) {
            // If Settings is in the backstack then we can continue with intended behavior
            if (checkIfFragmentInBackStack(R.id.browserFragment)) {
                findNavController().popBackStack(
                    R.id.homeFragment,
                    false
                )
                while (findNavController().currentDestination?.id == R.id.homeFragment) {
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
                    )
                }
            } else {
                findNavController().popBackStack()
            }
        } else {
            findNavController().popBackStack(
                R.id.homeFragment,
                false
            )
        }
    }

    // For some reason, the only way you can tell if you came from
    // BrowserFrag -> Settings OR BrowserFrag -> HomeFrag -> Settings is to check whether
    // there is a browser fragment entry in the back stack. If there is, it means you came from BrowserFrag -> Settings
    // if there isn't, it means you came from HomeFrag -> Settings
    private fun checkIfFragmentInBackStack(res: Int): Boolean {
        val backStackEntryCount = parentFragmentManager.backStackEntryCount
        for (i in 0 until backStackEntryCount) {
            if (getResIdFromBackstack(parentFragmentManager.getBackStackEntryAt(i).name) == res) {
                return true
            }
        }

        return false
    }

    private fun getResIdFromBackstack(name: String?): Int {
        val idString = name?.split("-")
        return idString!![1].toInt()
    }

    companion object {
        private const val ENABLED_ALPHA = 1f
        private const val DISABLED_ALPHA = 0.6f

        private const val OPEN_TABS_INDEX = 0
        private const val HISTORY_INDEX = 1
        private const val COOKIES_INDEX = 2
        private const val CACHED_INDEX = 3
        private const val PERMS_INDEX = 4
    }
}
