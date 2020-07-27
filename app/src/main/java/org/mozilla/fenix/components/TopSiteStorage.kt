/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.feature.top.sites.TopSiteStorage
import mozilla.components.support.locale.LocaleManager
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.observeOnce
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.settings.advanced.getSelectedLocale
import org.mozilla.fenix.utils.Mockable

@Mockable
class TopSiteStorage(private val context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(TOP_SITE_SETTINGS, Context.MODE_PRIVATE)

    private val dummyCachedTopSites = preferences.getString(TOP_SITE_JSON_STRING_KEY, "")

    var cachedTopSites = getDummyCachedTopSites()
        set(value) {
            field = value
            val serializer = Json(JsonConfiguration.Stable)
            val json = serializer.stringify(
                CachedTopSite.serializer().list,
                value.map {
                    CachedTopSite(
                        it.title,
                        it.id,
                        it.url
                    )
                })
            preferences.edit()
                .putString(
                    TOP_SITE_JSON_STRING_KEY,
                    json
                ).apply()
        }

    @Serializable
    data class CachedTopSite(
        val title: String,
        val id: Long,
        val url: String
    )

    private fun getDummyCachedTopSites(): List<TopSite> {
        if (dummyCachedTopSites.isNullOrEmpty()) return listOf()
        val serializer = Json(JsonConfiguration.Stable)
        val topSites = serializer.parse(CachedTopSite.serializer().list, dummyCachedTopSites)
        val dummyListTopSites = mutableListOf<TopSite>()
        for (topSite in topSites) {
            dummyListTopSites.add(
                DummyTopSite(
                    topSite.title,
                    topSite.url,
                    topSite.id
                )
            )
        }
        return dummyListTopSites
    }

    class DummyTopSite(
        private val dummyTitle: String,
        private val dummyUrl: String,
        private val dummyId: Long
    ) : TopSite {
        override val id: Long
            get() = dummyId
        override val isDefault: Boolean
            get() = false
        override val title: String
            get() = dummyTitle
        override val url: String
            get() = dummyUrl
    }

    val storage by lazy {
        TopSiteStorage(context)
    }

    init {
        addDefaultTopSites()
    }

    /**
     * Adds a new [TopSite].
     */
    fun addTopSite(title: String, url: String, isDefault: Boolean = false) {
        storage.addTopSite(title, url, isDefault)
    }

    /**
     * Returns a [LiveData] list of all the [TopSite] instances.
     */
    fun getTopSites(): LiveData<List<TopSite>> {
        return storage.getTopSites().asLiveData()
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
            topSiteCandidates.add(
                Pair(
                    context.getString(R.string.default_top_site_google),
                    SupportUtils.GOOGLE_URL
                )
            )

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

            GlobalScope.launch(Dispatchers.IO) {
                topSiteCandidates.forEach { (title, url) ->
                    addTopSite(title, url, isDefault = true)
                }
            }
            context.settings().defaultTopSitesAdded = true
        }
    }

    fun prefetch() {
        getTopSites().observeOnce {
            cachedTopSites = it
        }
    }

    companion object {
        const val TOP_SITE_SETTINGS = "top_sites"
        const val TOP_SITE_JSON_STRING_KEY = "cached_top_sites"
    }
}
