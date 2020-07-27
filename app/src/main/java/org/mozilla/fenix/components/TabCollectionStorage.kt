/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.paging.DataSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.tab.collections.Tab
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.tab.collections.TabCollectionStorage
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.resetPoliciesAfter
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.home.sessioncontrol.viewholders.CollectionViewHolder
import org.mozilla.fenix.utils.Mockable

@Mockable
class TabCollectionStorage(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val delegate: Observable<Observer> = ObserverRegistry()
) : Observable<org.mozilla.fenix.components.TabCollectionStorage.Observer> by delegate {

    /**
     * Interface to be implemented by classes that want to observe the storage
     */
    interface Observer {
        /**
         * A collection has been created
         */
        fun onCollectionCreated(title: String, sessions: List<Session>) = Unit

        /**
         *  Tab(s) have been added to collection
         */
        fun onTabsAdded(tabCollection: TabCollection, sessions: List<Session>) = Unit

        /**
         *  Collection has been renamed
         */
        fun onCollectionRenamed(tabCollection: TabCollection, title: String) = Unit
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(COLLECTIONS_SETTINGS, Context.MODE_PRIVATE)

    private val dummyCachedCollections = preferences.getString(COLLECTIONS_JSON_STRING_KEY, "")

    @Serializable
    data class CachedCollection(val title: String, val id: Long)

    var cachedTabCollections = getDummyCachedCollections()
        set(value) {
            field = value
            val serializer = Json(JsonConfiguration.Stable)
            val json = serializer.stringify(
                CachedCollection.serializer().list,
                value.map { CachedCollection(it.title, it.id) })
            preferences.edit()
                .putString(
                    COLLECTIONS_JSON_STRING_KEY,
                    json
                ).apply()
        }

    private fun getDummyCachedCollections(): List<TabCollection> {
        if (dummyCachedCollections.isNullOrEmpty()) return listOf()
        val serializer = Json(JsonConfiguration.Stable)
        val collections =
            serializer.parse(CachedCollection.serializer().list, dummyCachedCollections)
        val dummyListTabCollection = mutableListOf<TabCollection>()
        for (collection in collections) {
            dummyListTabCollection.add(DummyTabCollection(collection.title, collection.id))
        }
        return dummyListTabCollection
    }

    class DummyTabCollection(private val dummyTitle: String, private val dummyId: Long) :
        TabCollection {
        override val id: Long
            get() = dummyId
        override val tabs: List<Tab>
            get() = listOf()
        override val title: String
            get() = dummyTitle

        override fun restore(
            context: Context,
            engine: Engine,
            restoreSessionId: Boolean
        ): List<SessionManager.Snapshot.Item> {
            return listOf()
        }

        override fun restoreSubset(
            context: Context,
            engine: Engine,
            tabs: List<Tab>,
            restoreSessionId: Boolean
        ): List<SessionManager.Snapshot.Item> {
            return listOf()
        }
    }

    private val collectionStorage by lazy {
        StrictMode.allowThreadDiskReads().resetPoliciesAfter {
            TabCollectionStorage(context, sessionManager)
        }
    }

    fun createCollection(title: String, sessions: List<Session>) {
        collectionStorage.createCollection(title, sessions)
        notifyObservers { onCollectionCreated(title, sessions) }
    }

    fun addTabsToCollection(tabCollection: TabCollection, sessions: List<Session>) {
        collectionStorage.addTabsToCollection(tabCollection, sessions)
        notifyObservers { onTabsAdded(tabCollection, sessions) }
    }

    fun getTabCollectionsCount(): Int {
        return collectionStorage.getTabCollectionsCount()
    }

    fun getCollections(limit: Int = 20): LiveData<List<TabCollection>> {
        return collectionStorage.getCollections(limit).asLiveData()
    }

    fun getCollectionsPaged(): DataSource.Factory<Int, TabCollection> {
        return collectionStorage.getCollectionsPaged()
    }

    fun removeCollection(tabCollection: TabCollection) {
        collectionStorage.removeCollection(tabCollection)
    }

    fun removeTabFromCollection(tabCollection: TabCollection, tab: Tab) {
        collectionStorage.removeTabFromCollection(tabCollection, tab)
    }

    fun renameCollection(tabCollection: TabCollection, title: String) {
        collectionStorage.renameCollection(tabCollection, title)
        notifyObservers { onCollectionRenamed(tabCollection, title) }
    }

    companion object {
        const val COLLECTIONS_SETTINGS = "collections"
        const val COLLECTIONS_JSON_STRING_KEY = "cached_collections"
    }
}

fun TabCollection.description(context: Context): String {
    return this.tabs
        .map { it.url.toShortUrl(context.components.publicSuffixList) }
        .map {
            if (it.length > CollectionViewHolder.maxTitleLength) {
                it.substring(
                    0,
                    CollectionViewHolder.maxTitleLength
                ) + "…"
            } else {
                it
            }
        }
        .distinct()
        .joinToString(", ")
}
