/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.historymetadata

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.addons.showSnackBar
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.databinding.FragmentHistoryMetadataGroupBinding
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.setTextColor
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.library.LibraryPageFragment
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.historymetadata.controller.DefaultHistoryMetadataGroupController
import org.mozilla.fenix.library.historymetadata.interactor.DefaultHistoryMetadataGroupInteractor
import org.mozilla.fenix.library.historymetadata.interactor.HistoryMetadataGroupInteractor
import org.mozilla.fenix.library.historymetadata.view.HistoryMetadataGroupView
import org.mozilla.fenix.utils.allowUndo

/**
 * Displays a list of history metadata items for a history metadata search group.
 */
@SuppressWarnings("TooManyFunctions")
class HistoryMetadataGroupFragment :
    LibraryPageFragment<History.Metadata>(), UserInteractionHandler {

    private lateinit var historyMetadataGroupStore: HistoryMetadataGroupFragmentStore
    private lateinit var interactor: HistoryMetadataGroupInteractor

    private var _historyMetadataGroupView: HistoryMetadataGroupView? = null
    private val historyMetadataGroupView: HistoryMetadataGroupView
        get() = _historyMetadataGroupView!!
    private var _binding: FragmentHistoryMetadataGroupBinding? = null
    private val binding get() = _binding!!

    private val args by navArgs<HistoryMetadataGroupFragmentArgs>()

    override val selectedItems: Set<History.Metadata>
        get() = historyMetadataGroupStore.state.items.filter { it.selected }.toSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryMetadataGroupBinding.inflate(inflater, container, false)

        val historyItems = args.historyMetadataItems.filterIsInstance<History.Metadata>()
        historyMetadataGroupStore = StoreProvider.get(this) {
            HistoryMetadataGroupFragmentStore(
                HistoryMetadataGroupFragmentState(
                    items = historyItems,
                    pendingDeletionItems = requireContext().components.appStore.state.pendingDeletionHistoryItems,
                    isEmpty = historyItems.isEmpty()
                )
            )
        }

        interactor = DefaultHistoryMetadataGroupInteractor(
            controller = DefaultHistoryMetadataGroupController(
                historyStorage = (activity as HomeActivity).components.core.historyStorage,
                browserStore = (activity as HomeActivity).components.core.store,
                appStore = requireContext().components.appStore,
                store = historyMetadataGroupStore,
                selectOrAddUseCase = requireComponents.useCases.tabsUseCases.selectOrAddTab,
                navController = findNavController(),
                searchTerm = args.title,
                deleteSnackbar = :: deleteSnackbar,
                promptDeleteAll = :: promptDeleteAll,
                allDeletedSnackbar = ::allDeletedSnackbar,
            )
        )

        _historyMetadataGroupView = HistoryMetadataGroupView(
            container = binding.historyMetadataGroupLayout,
            interactor = interactor,
            title = args.title,
            onEmptyStateChanged = {
                historyMetadataGroupStore.dispatch(
                    HistoryMetadataGroupFragmentAction.ChangeEmptyState(it)
                )
            }
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFrom(historyMetadataGroupStore) { state ->
            historyMetadataGroupView.update(state)
            activity?.invalidateOptionsMenu()
        }

        requireContext().components.appStore.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.pendingDeletionHistoryItems }.collect { items ->
                historyMetadataGroupStore.dispatch(
                    HistoryMetadataGroupFragmentAction.UpdatePendingDeletionItems(
                        pendingDeletionItems = items
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(args.title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _historyMetadataGroupView = null
        _binding = null
    }

    override fun onBackPressed(): Boolean = interactor.onBackPressed(selectedItems)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (selectedItems.isNotEmpty()) {
            inflater.inflate(R.menu.history_select_multi, menu)

            menu.findItem(R.id.delete_history_multi_select)?.let { deleteItem ->
                deleteItem.title = SpannableString(deleteItem.title).apply {
                    setTextColor(requireContext(), R.attr.textWarning)
                }
            }
        } else {
            inflater.inflate(R.menu.history_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_history_multi_select -> {
                interactor.onShareMenuItem(selectedItems)
                true
            }
            R.id.delete_history_multi_select -> {
                interactor.onDelete(selectedItems)
                true
            }
            R.id.open_history_in_new_tabs_multi_select -> {
                openItemsInNewTab { selectedItem ->
                    selectedItem.url
                }

                showTabTray()
                true
            }
            R.id.open_history_in_private_tabs_multi_select -> {
                openItemsInNewTab(private = true) { selectedItem ->
                    selectedItem.url
                }

                (activity as HomeActivity).apply {
                    browsingModeManager.mode = BrowsingMode.Private
                    supportActionBar?.hide()
                }

                showTabTray()
                true
            }
            R.id.history_delete_all -> {
                interactor.onDeleteAllMenuItem()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteSnackbar(
        items: Set<History.Metadata>,
        undo: suspend (items: Set<History.Metadata>) -> Unit,
        delete: (Set<History.Metadata>) -> suspend (context: Context) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).allowUndo(
            requireView(),
            getSnackBarMessage(items),
            getString(R.string.snackbar_deleted_undo),
            {
                undo.invoke(items)
            },
            delete(items)
        )
    }

    private fun promptDeleteAll(delete: () -> Unit) {
        AlertDialog.Builder(requireContext()).apply {
            setMessage(R.string.delete_history_group_prompt_message)
            setNegativeButton(R.string.delete_history_group_prompt_cancel) { dialog: DialogInterface, _ ->
                dialog.cancel()
            }
            setPositiveButton(R.string.delete_history_group_prompt_allow) { dialog: DialogInterface, _ ->
                delete.invoke()
                dialog.dismiss()
            }
            create()
        }.show()
    }

    private fun allDeletedSnackbar() {
        showSnackBar(
            requireView(),
            getString(R.string.delete_history_group_snackbar)
        )
    }

    private fun showTabTray() {
        findNavController().nav(
            R.id.historyMetadataGroupFragment,
            HistoryMetadataGroupFragmentDirections.actionGlobalTabsTrayFragment()
        )
    }

    private fun getSnackBarMessage(historyItems: Set<History.Metadata>): String {
        val historyItem = historyItems.first()
        return String.format(
            requireContext().getString(R.string.history_delete_single_item_snackbar),
            historyItem.url.toShortUrl(requireComponents.publicSuffixList)
        )
    }
}
