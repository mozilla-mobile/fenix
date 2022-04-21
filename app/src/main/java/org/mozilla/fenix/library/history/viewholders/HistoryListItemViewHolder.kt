/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history.viewholders

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.HistoryListItemBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.hideAndDisable
import org.mozilla.fenix.ext.showAndEnable
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryFragmentState
import org.mozilla.fenix.library.history.HistoryInteractor
import org.mozilla.fenix.library.history.HistoryItemTimeGroup
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.utils.Do

class HistoryListItemViewHolder(
    view: View,
    private val historyInteractor: HistoryInteractor,
    private val selectionHolder: SelectionHolder<History>,
) : RecyclerView.ViewHolder(view) {

    private var item: History? = null
    private val binding = HistoryListItemBinding.bind(view)

    init {
        binding.recentlyClosedNavEmpty.recentlyClosedNav.setOnClickListener {
            historyInteractor.onRecentlyClosedClicked()
        }

        binding.historyLayout.overflowView.apply {
            setImageResource(R.drawable.ic_close)
            contentDescription = view.context.getString(R.string.history_delete_item)
            setOnClickListener {
                val item = item ?: return@setOnClickListener
                historyInteractor.onDeleteSome(setOf(item))
            }
        }
    }

    fun bind(
        item: History,
        timeGroup: HistoryItemTimeGroup?,
        showTopContent: Boolean,
        mode: HistoryFragmentState.Mode,
        isPendingDeletion: Boolean = false,
    ) {
        if (isPendingDeletion) {
            binding.historyLayout.visibility = View.GONE
        } else {
            binding.historyLayout.visibility = View.VISIBLE
        }

        binding.historyLayout.titleView.text = item.title

        binding.historyLayout.urlView.text = Do exhaustive when (item) {
            is History.Regular -> item.url
            is History.Metadata -> item.url
            is History.Group -> {
                val numChildren = item.items.size
                val stringId = if (numChildren == 1) {
                    R.string.history_search_group_site
                } else {
                    R.string.history_search_group_sites
                }
                String.format(itemView.context.getString(stringId), numChildren)
            }
        }

        toggleTopContent(showTopContent, mode === HistoryFragmentState.Mode.Normal)

        val headerText = timeGroup?.humanReadable(itemView.context)
        toggleHeader(headerText)

        binding.historyLayout.setSelectionInteractor(item, selectionHolder, historyInteractor)
        binding.historyLayout.changeSelected(item in selectionHolder.selectedItems)

        if (item is History.Regular &&
            (this.item as? History.Regular)?.url != item.url
        ) {
            binding.historyLayout.loadFavicon(item.url)
        } else if (item is History.Group) {
            binding.historyLayout.iconView.setImageResource(R.drawable.ic_multiple_tabs)
        }

        if (mode is HistoryFragmentState.Mode.Editing) {
            binding.historyLayout.overflowView.hideAndDisable()
        } else {
            binding.historyLayout.overflowView.showAndEnable()
        }

        this.item = item
    }

    private fun toggleHeader(headerText: String?) {
        if (headerText != null) {
            binding.headerTitle.visibility = View.VISIBLE
            binding.headerTitle.text = headerText
        } else {
            binding.headerTitle.visibility = View.GONE
        }
    }

    private fun toggleTopContent(
        showTopContent: Boolean,
        isNormalMode: Boolean,
    ) {
        binding.recentlyClosedNavEmpty.recentlyClosedNav.isVisible = showTopContent

        if (showTopContent) {
            val numRecentTabs = itemView.context.components.core.store.state.closedTabs.size
            binding.recentlyClosedNavEmpty.recentlyClosedTabsDescription.text = String.format(
                itemView.context.getString(
                    if (numRecentTabs == 1) {
                        R.string.recently_closed_tab
                    } else {
                        R.string.recently_closed_tabs
                    }
                ),
                numRecentTabs
            )
            binding.recentlyClosedNavEmpty.recentlyClosedNav.run {
                if (isNormalMode) {
                    isEnabled = true
                    alpha = 1f
                } else {
                    isEnabled = false
                    alpha = DISABLED_BUTTON_ALPHA
                }
            }
        }
    }

    companion object {
        const val DISABLED_BUTTON_ALPHA = 0.7f
        const val LAYOUT_ID = R.layout.history_list_item
    }
}
