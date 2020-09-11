/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.recentlyclosed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_recently_closed.*
import mozilla.components.browser.state.state.ClosedTab
import org.mozilla.fenix.R

interface RecentlyClosedInteractor {
    /**
     * Called when an item is tapped to restore it.
     *
     * @param item the tapped item to restore.
     */
    fun restore(item: ClosedTab)

    /**
     * Called when the view more history option is tapped.
     */
    fun onNavigateToHistory()

    /**
     * Copies the URL of a recently closed tab item to the copy-paste buffer.
     *
     * @param item the recently closed tab item to copy the URL from
     */
    fun onCopyPressed(item: ClosedTab)

    /**
     * Opens the share sheet for a recently closed tab item.
     *
     * @param item the recently closed tab item to share
     */
    fun onSharePressed(item: ClosedTab)

    /**
     * Opens a recently closed tab item in a new tab.
     *
     * @param item the recently closed tab item to open in a new tab
     */
    fun onOpenInNormalTab(item: ClosedTab)

    /**
     * Opens a recently closed tab item in a private tab.
     *
     * @param item the recently closed tab item to open in a private tab
     */
    fun onOpenInPrivateTab(item: ClosedTab)

    /**
     * Deletes one recently closed tab item.
     *
     * @param item the recently closed tab item to delete.
     */
    fun onDeleteOne(tab: ClosedTab)
}

/**
 * View that contains and configures the Recently Closed List
 */
class RecentlyClosedFragmentView(
    container: ViewGroup,
    private val interactor: RecentlyClosedFragmentInteractor
) : LayoutContainer {

    override val containerView: ConstraintLayout = LayoutInflater.from(container.context)
        .inflate(R.layout.component_recently_closed, container, true)
        .findViewById(R.id.recently_closed_wrapper)

    private val recentlyClosedAdapter: RecentlyClosedAdapter = RecentlyClosedAdapter(interactor)

    init {
        recently_closed_list.apply {
            layoutManager = LinearLayoutManager(containerView.context)
            adapter = recentlyClosedAdapter
        }

        view_more_history.apply {
            titleView.text =
                containerView.context.getString(R.string.recently_closed_show_full_history)
            urlView.isVisible = false
            overflowView.isVisible = false
            iconView.background = null
            iconView.setImageDrawable(
                ContextCompat.getDrawable(
                    containerView.context,
                    R.drawable.ic_history
                )
            )
            setOnClickListener {
                interactor.onNavigateToHistory()
            }
        }
    }

    fun update(items: List<ClosedTab>) {
        recently_closed_empty_view.isVisible = items.isEmpty()
        recently_closed_list.isVisible = items.isNotEmpty()
        recentlyClosedAdapter.submitList(items)
    }
}
