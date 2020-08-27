/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.selectfolder

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.support.ktx.android.content.getDrawableWithTint
import org.mozilla.fenix.R
import org.mozilla.fenix.library.SelectableWidgetSiteItem
import org.mozilla.fenix.library.bookmarks.BookmarkNodeWithDepth
import org.mozilla.fenix.library.bookmarks.BookmarksSharedViewModel
import org.mozilla.fenix.library.bookmarks.flatNodeList
import org.mozilla.fenix.library.bookmarks.selectfolder.SelectBookmarkFolderAdapter.BookmarkFolderViewHolder

class SelectBookmarkFolderAdapter(private val sharedViewModel: BookmarksSharedViewModel) :
    ListAdapter<BookmarkNodeWithDepth, BookmarkFolderViewHolder>(DiffCallback) {

    fun updateData(tree: BookmarkNode?, hideFolderGuid: String?) {
        val updatedData = tree
            ?.flatNodeList(hideFolderGuid)
            ?.drop(1)
            .orEmpty()

        submitList(updatedData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BookmarkFolderViewHolder(SelectableWidgetSiteItem(parent))

    override fun onBindViewHolder(holder: BookmarkFolderViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item, selected = item.node.isSelected()) { node ->
            val lastSelectedItemPosition = getSelectedItemIndex()

            sharedViewModel.toggleSelection(node)

            notifyItemChanged(position)
            lastSelectedItemPosition
                ?.takeIf { it != position }
                ?.let { notifyItemChanged(it) }
        }
    }

    class BookmarkFolderViewHolder(
        private val viewWrapper: SelectableWidgetSiteItem
    ) : RecyclerView.ViewHolder(viewWrapper.widget) {

        fun bind(folder: BookmarkNodeWithDepth, selected: Boolean, onSelect: (BookmarkNode) -> Unit) {
            viewWrapper.changeSelected(selected)
            viewWrapper.widget.iconView.setImageDrawable(
                viewWrapper.context.getDrawableWithTint(
                    R.drawable.ic_folder_icon,
                    ContextCompat.getColor(viewWrapper.context, R.color.primary_text_light_theme)
                )
            )
            viewWrapper.widget.setText(label = folder.node.title.orEmpty(), caption = null)
            viewWrapper.widget.setOnClickListener {
                onSelect(folder.node)
            }
            val pxToIndent = view.resources.getDimensionPixelSize(R.dimen.bookmark_select_folder_indent)
            val padding = pxToIndent * minOf(MAX_DEPTH, folder.depth)
            viewWrapper.widget.updatePaddingRelative(start = padding)
        }

        companion object {
            const val viewType = 1
        }
    }

    private fun getSelectedItemIndex(): Int? {
        val selectedNode = sharedViewModel.selectedFolder
        val selectedNodeIndex = currentList.indexOfFirst { it.node == selectedNode }

        return selectedNodeIndex.takeIf { it != -1 }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun BookmarkNode.isSelected(): Boolean =
        this == sharedViewModel.selectedFolder

    companion object {
        private const val MAX_DEPTH = 10
    }
}

private object DiffCallback : DiffUtil.ItemCallback<BookmarkNodeWithDepth>() {

    override fun areItemsTheSame(
        oldItem: BookmarkNodeWithDepth,
        newItem: BookmarkNodeWithDepth
    ) = oldItem.node.guid == newItem.node.guid

    override fun areContentsTheSame(
        oldItem: BookmarkNodeWithDepth,
        newItem: BookmarkNodeWithDepth
    ) = oldItem == newItem
}

private fun BookmarksSharedViewModel.toggleSelection(node: BookmarkNode?) {
    selectedFolder = if (selectedFolder == node) null else node
}
