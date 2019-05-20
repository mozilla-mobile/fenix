/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.tab.collections.Tab
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.tab.collections.TabCollectionStorage
import org.mozilla.fenix.test.Mockable

@Mockable
class TabCollectionStorage(private val context: Context, private val sessionManager: SessionManager) {

    var cachedTabCollections = listOf<TabCollection>()

    private val collectionStorage by lazy {
        TabCollectionStorage(context, sessionManager)
    }

    fun createCollection(title: String, sessions: List<Session>) {
        collectionStorage.createCollection(title, sessions)
    }

    fun addTabsToCollection(tabCollection: TabCollection, sessions: List<Session>) {
        collectionStorage.addTabsToCollection(tabCollection, sessions)
    }

    fun getTabCollectionsCount(): Int {
        return collectionStorage.getTabCollectionsCount()
    }

    fun getCollections(limit: Int = 20): LiveData<List<TabCollection>> {
        return collectionStorage.getCollections(limit)
    }

    fun getCollectionsPaged(): DataSource.Factory<Int, TabCollection> {
        return collectionStorage.getCollectionsPaged()
    }

    fun removeCollection(tabCollection: TabCollection) {
        collectionStorage.removeCollection(tabCollection)
    }

    fun removeTabFromCollection(tabCollection: TabCollection, tab: Tab) {
        if (tabCollection.tabs.size == 1) {
            removeCollection(tabCollection)
        } else {
            collectionStorage.removeTabFromCollection(tabCollection, tab)
        }
    }

    fun renameCollection(tabCollection: TabCollection, title: String) {
        collectionStorage.renameCollection(tabCollection, title)
    }
}
