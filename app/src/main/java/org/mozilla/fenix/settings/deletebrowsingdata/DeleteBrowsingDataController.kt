/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.deletebrowsingdata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.RecentlyClosedAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.fenix.components.PermissionStorage
import kotlin.coroutines.CoroutineContext

interface DeleteBrowsingDataController {
    suspend fun deleteTabs()
    suspend fun deleteBrowsingData()
    suspend fun deleteCookies()
    suspend fun deleteCachedFiles()
    suspend fun deleteSitePermissions()
    suspend fun deleteDownloads()
}

class DefaultDeleteBrowsingDataController(
    private val removeAllTabs: TabsUseCases.RemoveAllTabsUseCase,
    private val removeAllDownloads: DownloadsUseCases.RemoveAllDownloadsUseCase,
    private val historyStorage: HistoryStorage,
    private val permissionStorage: PermissionStorage,
    private val store: BrowserStore,
    private val iconsStorage: BrowserIcons,
    private val engine: Engine,
    private val coroutineContext: CoroutineContext = Dispatchers.Main
) : DeleteBrowsingDataController {

    override suspend fun deleteTabs() {
        withContext(coroutineContext) {
            removeAllTabs.invoke(false)
        }
    }

    override suspend fun deleteBrowsingData() {
        withContext(coroutineContext) {
            engine.clearData(Engine.BrowsingData.select(Engine.BrowsingData.DOM_STORAGES))
            historyStorage.deleteEverything()
            store.dispatch(EngineAction.PurgeHistoryAction)
            iconsStorage.clear()
            store.dispatch(RecentlyClosedAction.RemoveAllClosedTabAction)
        }
    }

    override suspend fun deleteCookies() {
        withContext(coroutineContext) {
            engine.clearData(
                Engine.BrowsingData.select(
                    Engine.BrowsingData.COOKIES,
                    Engine.BrowsingData.AUTH_SESSIONS
                )
            )
        }
    }

    override suspend fun deleteCachedFiles() {
        withContext(coroutineContext) {
            engine.clearData(
                Engine.BrowsingData.select(Engine.BrowsingData.ALL_CACHES)
            )
        }
    }

    override suspend fun deleteSitePermissions() {
        withContext(coroutineContext) {
            engine.clearData(
                Engine.BrowsingData.select(Engine.BrowsingData.ALL_SITE_SETTINGS)
            )
        }
        permissionStorage.deleteAllSitePermissions()
    }

    override suspend fun deleteDownloads() {
        withContext(coroutineContext) {
            removeAllDownloads.invoke()
        }
    }
}
