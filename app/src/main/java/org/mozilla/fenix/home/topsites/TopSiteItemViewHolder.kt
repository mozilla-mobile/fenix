/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.topsites

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.TopSites
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.TopSiteItemBinding
import org.mozilla.fenix.ext.bitmapForUrl
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.loadIntoView
import org.mozilla.fenix.ext.name
import org.mozilla.fenix.home.sessioncontrol.TopSiteInteractor
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.utils.view.ViewHolder

class TopSiteItemViewHolder(
    view: View,
    private val viewLifecycleOwner: LifecycleOwner,
    private val interactor: TopSiteInteractor
) : ViewHolder(view) {
    private lateinit var topSite: TopSite
    private val binding = TopSiteItemBinding.bind(view)

    init {
        binding.topSiteItem.setOnLongClickListener {
            interactor.onTopSiteMenuOpened()
            TopSites.longPress.record(TopSites.LongPressExtra(topSite.name()))

            val topSiteMenu = TopSiteItemMenu(
                context = view.context,
                topSite = topSite
            ) { item ->
                when (item) {
                    is TopSiteItemMenu.Item.OpenInPrivateTab -> interactor.onOpenInPrivateTabClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.RenameTopSite -> interactor.onRenameTopSiteClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.RemoveTopSite -> interactor.onRemoveTopSiteClicked(
                        topSite
                    )
                    is TopSiteItemMenu.Item.Settings -> interactor.onSettingsClicked()
                    is TopSiteItemMenu.Item.SponsorPrivacy -> interactor.onSponsorPrivacyClicked()
                }
            }
            val menu = topSiteMenu.menuBuilder.build(view.context).show(anchor = it)

            it.setOnTouchListener @SuppressLint("ClickableViewAccessibility") { v, event ->
                onTouchEvent(v, event, menu)
            }

            true
        }
    }

    fun bind(topSite: TopSite, position: Int) {
        binding.topSiteItem.setOnClickListener {
            interactor.onSelectTopSite(topSite, position)
        }

        binding.topSiteTitle.text = topSite.title

        if (topSite is TopSite.Pinned || topSite is TopSite.Default) {
            val pinIndicator = getDrawable(itemView.context, R.drawable.ic_new_pin)
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(pinIndicator, null, null, null)
        } else {
            binding.topSiteTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        if (topSite is TopSite.Provided) {
            binding.topSiteSubtitle.isVisible = true

            viewLifecycleOwner.lifecycleScope.launch(IO) {
                itemView.context.components.core.client.bitmapForUrl(topSite.imageUrl)?.let { bitmap ->
                    withContext(Main) {
                        binding.faviconImage.setImageBitmap(bitmap)
                        submitTopSitesImpressionPing(topSite, position)
                    }
                }
            }
        } else {
            when (topSite.url) {
                SupportUtils.POCKET_TRENDING_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_pocket))
                }
                SupportUtils.BAIDU_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_baidu))
                }
                SupportUtils.JD_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_jd))
                }
                SupportUtils.PDD_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_pdd))
                }
                SupportUtils.TC_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_tc))
                }
                SupportUtils.MEITUAN_URL -> {
                    binding.faviconImage.setImageDrawable(getDrawable(itemView.context, R.drawable.ic_meituan))
                }
                else -> {
                    itemView.context.components.core.icons.loadIntoView(binding.faviconImage, topSite.url)
                }
            }
        }

        this.topSite = topSite
    }

    @VisibleForTesting
    internal fun submitTopSitesImpressionPing(topSite: TopSite.Provided, position: Int) {
        TopSites.contileImpression.record(
            TopSites.ContileImpressionExtra(
                position = position + 1,
                source = "newtab"
            )
        )

        topSite.id?.let { TopSites.contileTileId.set(it) }
        topSite.title?.let { TopSites.contileAdvertiser.set(it.lowercase()) }
        TopSites.contileReportingUrl.set(topSite.impressionUrl)
        Pings.topsitesImpression.submit()
    }

    private fun onTouchEvent(
        v: View,
        event: MotionEvent,
        menu: PopupWindow
    ): Boolean {
        if (event.action == MotionEvent.ACTION_CANCEL) {
            menu.dismiss()
        }
        return v.onTouchEvent(event)
    }

    companion object {
        const val LAYOUT_ID = R.layout.top_site_item
    }
}
