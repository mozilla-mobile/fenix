package org.mozilla.fenix.library.bookmarks

import android.content.Context
import android.util.AttributeSet
import org.mozilla.fenix.library.LibrarySiteItemView

class BookmarkSeparatorItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LibrarySiteItemView(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Do nothing, separator should not display overflow menu
     */
    override fun showOverflowMenu() {
        /* noop */
    }
}
