/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.graphics.Bitmap
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.tab_list_row.view.*
import mozilla.components.feature.media.state.MediaState
import mozilla.components.support.ktx.android.util.dpToFloat
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.ext.loadIntoView

/**
 * View that represents individual tab items
 */
class TabItemViewHolder(
    private val view: View,
    private val interactor: TabTrayViewInteractor
) : RecyclerView.ViewHolder(view) {

    internal var tab: Tab? = null

    init {
        view.setOnClickListener {
            tab?.also(interactor::open)
        }

        view.close_tab_button.setOnClickListener {
            tab?.also(interactor::closeButtonTapped)
        }

        view.play_pause_button.increaseTapArea(PLAY_PAUSE_BUTTON_EXTRA_DPS)

        view.play_pause_button.setOnClickListener {
            when (tab?.mediaState) {
                is MediaState.Playing -> {
                    it.context.components.analytics.metrics.track(Event.TabMediaPause)
                    interactor.onPauseMediaClicked()
                }

                is MediaState.Paused -> {
                    it.context.components.analytics.metrics.track(Event.TabMediaPlay)
                    interactor.onPlayMediaClicked()
                }
            }
        }

        view.favicon_image.clipToOutline = true
        view.favicon_image.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                outline?.setRoundRect(
                    0,
                    0,
                    view!!.width,
                    view.height,
                    favIconBorderRadiusInPx.dpToFloat(view.context.resources.displayMetrics)
                )
            }
        }
    }

    internal fun bind(tab: Tab) {
        updateTab(tab)
        updateTitle(tab.title)
        updateHostname(tab.hostname)
        updateFavIcon(tab.url, tab.icon)
        updateSelected(tab.selected)
        updatePlayPauseButton(tab.mediaState)
        updateCloseButtonDescription(tab.title)
    }

    internal fun updatePlayPauseButton(mediaState: MediaState) {
        with(view.play_pause_button) {
            visibility = if (mediaState is MediaState.Playing || mediaState is MediaState.Paused) {
                View.VISIBLE
            } else {
                View.GONE
            }

            if (mediaState is MediaState.Playing) {
                play_pause_button.contentDescription =
                    context.getString(R.string.mozac_feature_media_notification_action_pause)
                setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.pause_with_background))
            } else {
                play_pause_button.contentDescription =
                    context.getString(R.string.mozac_feature_media_notification_action_play)
                setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.play_with_background))
            }
        }
    }

    internal fun updateTab(tab: Tab) {
        this.tab = tab
    }
    internal fun updateTitle(text: String) {
        view.tab_title.text = text
    }

    internal fun updateHostname(text: String) {
        view.hostname.text = text
    }

    internal fun updateFavIcon(url: String, icon: Bitmap?) {
        if (icon == null) {
            view.favicon_image.context.components.core.icons.loadIntoView(view.favicon_image, url)
        } else {
            view.favicon_image.setImageBitmap(icon)
        }
    }

    internal fun updateSelected(selected: Boolean) {
        view.selected_border.visibility = if (selected) View.VISIBLE else View.GONE
    }
    internal fun updateCloseButtonDescription(title: String) {
        view.close_tab_button.contentDescription =
            view.close_tab_button.context.getString(R.string.close_tab_title, title)
    }

    companion object {
        private const val PLAY_PAUSE_BUTTON_EXTRA_DPS = 24
        const val LAYOUT_ID = R.layout.tab_list_row
        const val favIconBorderRadiusInPx = 4
    }
}

/**
 * Adapter that helps facilitate the Tab Tray
 */
class TabTrayAdapter(
    private val interactor: TabTrayViewInteractor
) : RecyclerView.Adapter<TabItemViewHolder>() {
    private var state = TabTrayFragmentState(listOf())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(TabItemViewHolder.LAYOUT_ID, parent, false)
        return TabItemViewHolder(view, interactor)
    }

    override fun getItemCount() = state.tabs.size

    override fun onBindViewHolder(holder: TabItemViewHolder, position: Int) {
        holder.bind(state.tabs[position])
    }

    fun updateState(state: TabTrayFragmentState) {
        this.state = state
        notifyDataSetChanged()
    }
}
