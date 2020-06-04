/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.feature.top.sites.TopSiteStorage
import mozilla.components.support.locale.LocaleManager
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.settings.advanced.getSelectedLocale
import org.mozilla.fenix.utils.Mockable

@Mockable
class TopSiteStorage(
    private val context: Context,
    private val historyStorage: HistoryStorage
) {
    var cachedTopSites = listOf<TopSite>()

    val storage by lazy {
        TopSiteStorage(context)
    }

    init {
        addDefaultTopSites()
    }

    /**
     * Adds a new [TopSite].
     *
     * @param title The title string.
     * @param url The URL string.
     * @param isDefault Whether or not the top site added should be a default top site. This is
     * used to identify top sites that are added by the application.
     * @param isPinned Whether or not the top site is pinned by the user. This helps the application
     * differentiate between user pinned top sites and top sites generated from frequently visited
     * sites.
     */
    fun addTopSite(
        title: String,
        url: String,
        isDefault: Boolean = false,
        isPinned: Boolean = true
    ) {
        storage.addTopSite(title, url, isDefault, isPinned)
    }

    /**
     * Returns a [LiveData] list of all the [TopSite] instances.
     */
    fun getTopSites(): LiveData<List<TopSite>> {
        return storage.getTopSites()
    }

    /**
     * Refreshes the top frequently visited sites on the home screen.
     */
    fun refreshTopFrecentSites() {
        GlobalScope.launch(Dispatchers.IO) {
            val oldTopFrecentSites = cachedTopSites.filterNot { it.isPinned }
            var newTopFrecentSites = historyStorage
                // Get 2 times the top sites limit to buffer against duplicate entries.
                .getTopFrecentSites(TOP_SITES_LIMIT * 2)
                // Filter out any entries with no title and duplicates of existing pinned top sites.
                .filter { newTopFrecentSite ->
                    if (newTopFrecentSite.title == "") {
                        return@filter false
                    }

                    cachedTopSites.filter { it.isPinned }.forEach { topSite ->
                        if (topSite.url == newTopFrecentSite.url) {
                            return@filter false
                        }
                    }
                    return@filter true
                }
            val numTopSitesToAdd = TOP_SITES_LIMIT - cachedTopSites.size

            // Refresh the old top frecent sites.
            for (i in oldTopFrecentSites.indices) {
                val oldTopSite = oldTopFrecentSites[i]
                val newTopSite = newTopFrecentSites.getOrNull(i)

                if (newTopSite == null) {
                    break
                } else if (oldTopSite.url != newTopSite.url) {
                    removeTopSite(oldTopSite)
                    addTopSite(newTopSite.title ?: "", newTopSite.url, isPinned = false)
                }
            }

            newTopFrecentSites = newTopFrecentSites.drop(oldTopFrecentSites.size)

            for (i in 0 until numTopSitesToAdd) {
                val newTopSite = newTopFrecentSites.getOrNull(i) ?: break
                addTopSite(newTopSite.title ?: "", newTopSite.url, isPinned = false)
            }
        }
    }

    /**
     * Removes the given [TopSite].
     */
    fun removeTopSite(topSite: TopSite) {
        storage.removeTopSite(topSite)
    }

    private fun addDefaultTopSites() {
        val topSiteCandidates = mutableListOf<Pair<String, String>>()
        if (!context.settings().defaultTopSitesAdded) {
            if (LocaleManager.getSelectedLocale(context).language == "en") {
                topSiteCandidates.add(
                    Pair(
                        context.getString(R.string.pocket_pinned_top_articles),
                        SupportUtils.POCKET_TRENDING_URL
                    )
                )
            }

            topSiteCandidates.add(
                Pair(
                    context.getString(R.string.default_top_site_wikipedia),
                    SupportUtils.WIKIPEDIA_URL
                )
            )

            topSiteCandidates.add(
                Pair(
                    context.getString(R.string.default_top_site_youtube),
                    SupportUtils.YOUTUBE_URL
                )
            )

            GlobalScope.launch(Dispatchers.IO) {
                topSiteCandidates.forEach { (title, url) ->
                    addTopSite(title, url, isDefault = true, isPinned = true)
                }
            }
            context.settings().defaultTopSitesAdded = true
        }
    }

    companion object {
        private const val TOP_SITES_LIMIT = 8
    }
}
