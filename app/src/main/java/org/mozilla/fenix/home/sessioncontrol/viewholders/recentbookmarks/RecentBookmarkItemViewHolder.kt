/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.recentbookmarks

import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import kotlinx.android.synthetic.main.recent_bookmark_item.*
import kotlinx.android.synthetic.main.top_site_item.favicon_image
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.loadIntoView
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.utils.view.ViewHolder

class RecentBookmarkItemViewHolder(
    view: View,
    private val interactor: SessionControlInteractor
) : ViewHolder(view) {
    private lateinit var bookmark: BookmarkNode

    init {
        bookmark_item.setOnClickListener {
            interactor.onRecentBookmarkClicked(bookmark)
        }
    }

    fun bind(bookmark: BookmarkNode) {
        bookmark_title.text = bookmark.title

        when (bookmark.url) {
            SupportUtils.POCKET_TRENDING_URL -> {
                favicon_image.setImageDrawable(
                    AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_pocket
                    )
                )
            }
            SupportUtils.BAIDU_URL -> {
                favicon_image.setImageDrawable(
                    AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_baidu
                    )
                )
            }
            SupportUtils.JD_URL -> {
                favicon_image.setImageDrawable(
                    AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_jd
                    )
                )
            }
            SupportUtils.PDD_URL -> {
                favicon_image.setImageDrawable(
                    AppCompatResources.getDrawable(
                        itemView.context,
                        R.drawable.ic_pdd
                    )
                )
            }
            else -> {
                itemView.context.components.core.icons.loadIntoView(favicon_image, bookmark.url!!)
            }
        }

        this.bookmark = bookmark
    }

    companion object {
        const val LAYOUT_ID = R.layout.recent_bookmark_item
    }
}
