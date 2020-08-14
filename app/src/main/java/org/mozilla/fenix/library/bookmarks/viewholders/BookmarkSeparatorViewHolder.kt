/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.viewholders

import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.library.LibrarySiteItemView
import org.mozilla.fenix.library.bookmarks.BookmarkFragmentState
import org.mozilla.fenix.library.bookmarks.BookmarkPayload
import org.mozilla.fenix.library.bookmarks.BookmarkViewInteractor

/**
 * Simple view holder for dividers in the bookmarks list.
 */
class BookmarkSeparatorViewHolder(
    view: LibrarySiteItemView,
    interactor: BookmarkViewInteractor
) : BookmarkNodeViewHolder(view, interactor) {

    override var item: BookmarkNode? = null

    override fun bind(
        item: BookmarkNode,
        mode: BookmarkFragmentState.Mode
    ) {
        this.item = item
        containerView.displayAs(LibrarySiteItemView.ItemType.SEPARATOR)
        updateMenu(item.type)
    }

    override fun bind(
        item: BookmarkNode,
        mode: BookmarkFragmentState.Mode,
        payload: BookmarkPayload
    ) {
        bind(item, mode)
    }
}
