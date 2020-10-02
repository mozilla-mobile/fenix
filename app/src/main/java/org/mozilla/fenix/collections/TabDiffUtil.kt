/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.collections

import androidx.recyclerview.widget.DiffUtil
import org.mozilla.fenix.home.Tab

/**
 * Diff callback for comparing tab lists with selected state.
 */
internal class TabDiffUtil(
    private val old: List<Tab>,
    private val new: List<Tab>,
    private val oldSelected: Set<Tab>,
    private val newSelected: Set<Tab>,
    private val oldHideCheckboxes: Boolean,
    private val newHideCheckboxes: Boolean
) : DiffUtil.Callback() {

    /**
     * Checks if the tabs in the given positions refer to the same tab (based on ID).
     */
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        old[oldItemPosition].sessionId == new[newItemPosition].sessionId

    /**
     * Checks if the combination of tab ID, selection, and checkbox visibility is the same.
     */
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val isSameTab = old[oldItemPosition].sessionId == new[newItemPosition].sessionId
        val sameSelectedState = oldItemSelected(oldItemPosition) == newItemSelected(newItemPosition)
        val isSameHideCheckboxes = oldHideCheckboxes == newHideCheckboxes
        return isSameTab && sameSelectedState && isSameHideCheckboxes
    }

    /**
     * Returns a change payload indication if the item is now/no longer selected.
     */
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val shouldBeChecked = newItemSelected(newItemPosition) && !oldItemSelected(oldItemPosition)
        val shouldBeUnchecked = !newItemSelected(newItemPosition) && oldItemSelected(oldItemPosition)
        return CheckChanged(shouldBeChecked, shouldBeUnchecked, newHideCheckboxes)
    }

    override fun getOldListSize(): Int = old.size
    override fun getNewListSize(): Int = new.size

    private fun oldItemSelected(oldItemPosition: Int) = oldSelected.contains(old[oldItemPosition])
    private fun newItemSelected(newItemPosition: Int) = newSelected.contains(new[newItemPosition])
}

/**
 * @property shouldBeChecked Item was previously unchecked and should be checked.
 * @property shouldBeUnchecked Item was previously checked and should be unchecked.
 * @property shouldHideCheckBox Checkbox should be visible.
 */
data class CheckChanged(
    val shouldBeChecked: Boolean,
    val shouldBeUnchecked: Boolean,
    val shouldHideCheckBox: Boolean
)
