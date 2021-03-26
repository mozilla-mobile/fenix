/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import kotlinx.android.synthetic.main.checkbox_item.view.*
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.browser.tabstray.thumbnail.TabThumbnailView
import mozilla.components.browser.toolbar.MAX_URI_LENGTH
import mozilla.components.concept.base.images.ImageLoadRequest
import mozilla.components.concept.base.images.ImageLoader
import mozilla.components.concept.engine.mediasession.MediaSession
import mozilla.components.concept.tabstray.Tab
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.ext.removeAndDisable
import org.mozilla.fenix.ext.removeTouchDelegate
import org.mozilla.fenix.ext.showAndEnable
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.tabstray.browser.BrowserTrayInteractor

/**
 * A RecyclerView ViewHolder implementation for "tab" items.
 */
abstract class TabsTrayViewHolder(
    itemView: View,
    private val imageLoader: ImageLoader,
    private val thumbnailSize: Int,
    private val browserTrayInteractor: BrowserTrayInteractor?,
    private val store: BrowserStore = itemView.context.components.core.store,
    private val metrics: MetricController = itemView.context.components.analytics.metrics
) : TabViewHolder(itemView) {

    private val faviconView: ImageView? =
        itemView.findViewById(R.id.mozac_browser_tabstray_favicon_icon)
    private val titleView: TextView = itemView.findViewById(R.id.mozac_browser_tabstray_title)
    private val closeView: AppCompatImageButton =
        itemView.findViewById(R.id.mozac_browser_tabstray_close)
    private val thumbnailView: TabThumbnailView =
        itemView.findViewById(R.id.mozac_browser_tabstray_thumbnail)

    @VisibleForTesting
    internal val urlView: TextView? = itemView.findViewById(R.id.mozac_browser_tabstray_url)
    private val playPauseButtonView: ImageButton = itemView.findViewById(R.id.play_pause_button)

    override var tab: Tab? = null

    /**
     * Displays the data of the given session and notifies the given observable about events.
     */
    @Suppress("ComplexMethod", "LongMethod")
    override fun bind(
        tab: Tab,
        isSelected: Boolean,
        styling: TabsTrayStyling,
        observable: Observable<TabsTray.Observer>
    ) {
        this.tab = tab

        updateTitle(tab)
        updateUrl(tab)
        updateFavicon(tab)
        updateCloseButtonDescription(tab.title)
        updateSelectedTabIndicator(isSelected)

        if (tab.thumbnail != null) {
            thumbnailView.setImageBitmap(tab.thumbnail)
        } else {
            loadIntoThumbnailView(thumbnailView, tab.id)
        }

        // Media state
        playPauseButtonView.increaseTapArea(PLAY_PAUSE_BUTTON_EXTRA_DPS)

        with(playPauseButtonView) {
            invalidate()
            val sessionState = store.state.findTabOrCustomTab(tab.id)
            when (sessionState?.mediaSessionState?.playbackState) {
                MediaSession.PlaybackState.PAUSED -> {
                    showAndEnable()
                    contentDescription =
                        context.getString(R.string.mozac_feature_media_notification_action_play)
                    setImageDrawable(
                        AppCompatResources.getDrawable(context, R.drawable.media_state_play)
                    )
                }

                MediaSession.PlaybackState.PLAYING -> {
                    showAndEnable()
                    contentDescription =
                        context.getString(R.string.mozac_feature_media_notification_action_pause)
                    setImageDrawable(
                        AppCompatResources.getDrawable(context, R.drawable.media_state_pause)
                    )
                }

                else -> {
                    removeTouchDelegate()
                    removeAndDisable()
                }
            }

            setOnClickListener {
                when (sessionState?.mediaSessionState?.playbackState) {
                    MediaSession.PlaybackState.PLAYING -> {
                        metrics.track(Event.TabMediaPause)
                        sessionState.mediaSessionState?.controller?.pause()
                    }

                    MediaSession.PlaybackState.PAUSED -> {
                        metrics.track(Event.TabMediaPlay)
                        sessionState.mediaSessionState?.controller?.play()
                    }
                    else -> throw AssertionError(
                        "Play/Pause button clicked without play/pause state."
                    )
                }
            }
        }

        closeView.setOnClickListener {
            observable.notifyObservers { onTabClosed(tab) }
        }
    }

    fun getItemDetails() = object : ItemDetailsLookup.ItemDetails<Long>() {
        override fun getPosition(): Int = bindingAdapterPosition
        override fun getSelectionKey(): Long = itemId
        override fun inSelectionHotspot(e: MotionEvent): Boolean {
            return browserTrayInteractor?.isMultiSelectMode() == true
        }
    }

    fun showTabIsMultiSelectEnabled(isSelected: Boolean) {
        itemView.selected_mask.isVisible = isSelected
        // TODO Enable this with https://github.com/mozilla-mobile/fenix/issues/18656
        // itemView.mozac_browser_tabstray_close.isVisible =
        //    browserTrayInteractor?.isMultiSelectMode() == false
    }

    private fun updateFavicon(tab: Tab) {
        if (tab.icon != null) {
            faviconView?.visibility = View.VISIBLE
            faviconView?.setImageBitmap(tab.icon)
        } else {
            faviconView?.visibility = View.GONE
        }
    }

    private fun updateTitle(tab: Tab) {
        val title = if (tab.title.isNotEmpty()) {
            tab.title
        } else {
            tab.url
        }
        titleView.text = title
    }

    private fun updateUrl(tab: Tab) {
        // Truncate to MAX_URI_LENGTH to prevent the UI from locking up for
        // extremely large URLs such as data URIs or bookmarklets. The same
        // is done in the toolbar and awesomebar:
        // https://github.com/mozilla-mobile/fenix/issues/1824
        // https://github.com/mozilla-mobile/android-components/issues/6985
        urlView?.text = tab.url
            .toShortUrl(itemView.context.components.publicSuffixList)
            .take(MAX_URI_LENGTH)
    }

    private fun updateCloseButtonDescription(title: String) {
        closeView.contentDescription =
            closeView.context.getString(R.string.close_tab_title, title)
    }

    private fun loadIntoThumbnailView(thumbnailView: ImageView, id: String) {
        imageLoader.loadIntoView(thumbnailView, ImageLoadRequest(id, thumbnailSize))
    }

    companion object {
        internal const val PLAY_PAUSE_BUTTON_EXTRA_DPS = 24
        internal const val GRID_ITEM_CLOSE_BUTTON_EXTRA_DPS = 24
    }
}
