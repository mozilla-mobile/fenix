/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_delete_browsing_data.*
import kotlinx.android.synthetic.main.fragment_delete_browsing_data.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.tab.collections.TabCollection
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.ext.requireComponents

@SuppressWarnings("TooManyFunctions")
class DeleteBrowsingDataFragment : Fragment() {
    private lateinit var sessionObserver: SessionManager.Observer
    private var tabCollections: List<TabCollection> = listOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_delete_browsing_data, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionObserver = object : SessionManager.Observer {
            override fun onSessionAdded(session: Session) {
                super.onSessionAdded(session)
                updateTabCount()
            }

            override fun onSessionRemoved(session: Session) {
                super.onSessionRemoved(session)
                updateTabCount()
            }

            override fun onSessionSelected(session: Session) {
                super.onSessionSelected(session)
                updateTabCount()
            }

            override fun onSessionsRestored() {
                super.onSessionsRestored()
                updateTabCount()
            }

            override fun onAllSessionsRemoved() {
                super.onAllSessionsRemoved()
                updateTabCount()
            }
        }

        requireComponents.core.sessionManager.register(sessionObserver, owner = this)
        requireComponents.core.tabCollectionStorage.apply {
            getCollections().observe(this@DeleteBrowsingDataFragment, Observer {
                this@DeleteBrowsingDataFragment.tabCollections = it
            })
        }

        view.open_tabs_item?.onCheckListener = { _ -> updateDeleteButton() }
        view.browsing_data_item?.onCheckListener = { _ -> updateDeleteButton() }
        view.collections_item?.onCheckListener = { _ -> updateDeleteButton() }
        view.delete_data?.setOnClickListener {
            askToDelete()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).apply {
            title = getString(R.string.preferences_delete_browsing_data)
            supportActionBar?.show()
        }

        updateTabCount()
        updateHistoryCount()
        updateCollectionsCount()
        updateDeleteButton()
    }

    private fun askToDelete() {
        context?.let {
            AlertDialog.Builder(it).apply {
                val appName = context.getString(R.string.app_name)
                val message = context.getString(R.string.preferences_delete_browsing_data_prompt_message, appName)
                setMessage(message)

                setNegativeButton(R.string.preferences_delete_browsing_data_prompt_cancel) { it: DialogInterface, _ ->
                    it.cancel()
                }

                setPositiveButton(R.string.preferences_delete_browsing_data_prompt_allow) { it: DialogInterface, _ ->
                    it.dismiss()
                    deleteSelected()
                }
                create()
            }.show()
        }
    }

    private fun deleteSelected() {
        val openTabsChecked = view!!.open_tabs_item!!.isChecked
        val browsingDataChecked = view!!.browsing_data_item!!.isChecked
        val collectionsChecked = view!!.collections_item!!.isChecked

        startDeletion()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (openTabsChecked) deleteTabs()
            if (browsingDataChecked) deleteBrowsingData()
            if (collectionsChecked) deleteCollections()

            launch(Dispatchers.Main) {
                finishDeletion()
            }
        }
    }

    fun startDeletion() {
        progress_bar.visibility = View.VISIBLE
        delete_browsing_data_wrapper.isEnabled = false
        delete_browsing_data_wrapper.isClickable = false
        delete_browsing_data_wrapper.alpha = DISABLED_ALPHA
    }

    fun finishDeletion() {
        progress_bar.visibility = View.GONE
        delete_browsing_data_wrapper.isEnabled = true
        delete_browsing_data_wrapper.isClickable = true
        delete_browsing_data_wrapper.alpha = ENABLED_ALPHA

        listOf(open_tabs_item, browsing_data_item, collections_item).forEach { it.isChecked = false }

        updateTabCount()
        updateHistoryCount()
        updateCollectionsCount()

        FenixSnackbar.make(view!!, FenixSnackbar.LENGTH_SHORT)
            .setText(resources.getString(R.string.preferences_delete_browsing_data_snackbar))
            .show()
    }

    private fun updateDeleteButton() {
        val openTabs = view!!.open_tabs_item!!.isChecked
        val browsingData = view!!.browsing_data_item!!.isChecked
        val collections = view!!.collections_item!!.isChecked
        val enabled = openTabs || browsingData || collections

        view?.delete_data?.isEnabled = enabled
        view?.delete_data?.alpha = if (enabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    private fun updateTabCount() {
        view?.open_tabs_item?.apply {
            val openTabs = requireComponents.core.sessionManager.size
            subtitleView.text = resources.getString(R.string.preferences_delete_browsing_data_tabs_subtitle, openTabs)
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

    private fun updateCollectionsCount() {
        view?.browsing_data_item?.subtitleView?.text = ""

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val collectionsCount = requireComponents.core.tabCollectionStorage.getTabCollectionsCount()
            launch(Dispatchers.Main) {
                view?.collections_item?.apply {
                    subtitleView.text =
                        resources.getString(
                            R.string.preferences_delete_browsing_data_collections_subtitle,
                            collectionsCount
                        )
                }
            }
        }
    }

    private suspend fun deleteTabs() {
        withContext(Dispatchers.Main) {
            requireComponents.useCases.tabsUseCases.removeAllTabs.invoke()
        }
    }

    private suspend fun deleteBrowsingData() {
        withContext(Dispatchers.Main) {
            requireComponents.core.engine.clearData(Engine.BrowsingData.all())
        }
        requireComponents.core.historyStorage.deleteEverything()
    }

    private suspend fun deleteCollections() {
        while (requireComponents.core.tabCollectionStorage.getTabCollectionsCount() != tabCollections.size) {
            delay(DELAY_IN_MILLIS)
        }

        tabCollections.forEach { requireComponents.core.tabCollectionStorage.removeCollection(it) }
    }

    companion object {
        private const val ENABLED_ALPHA = 1f
        private const val DISABLED_ALPHA = 0.6f
        private const val DELAY_IN_MILLIS = 500L
    }
}
