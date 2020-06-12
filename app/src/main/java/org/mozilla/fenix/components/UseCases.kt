/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import mozilla.components.browser.search.SearchEngineManager
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.thumbnails.ThumbnailsUseCases
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.app.links.AppLinksUseCases
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.feature.pwa.WebAppShortcutManager
import mozilla.components.feature.pwa.WebAppUseCases
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.session.SettingsUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.fenix.utils.Mockable

/**
 * Component group for all use cases. Use cases are provided by feature
 * modules and can be triggered by UI interactions.
 */
@Mockable
class UseCases(
    private val context: Context,
    private val engine: Engine,
    private val sessionManager: SessionManager,
    private val store: BrowserStore,
    private val searchEngineManager: SearchEngineManager,
    private val shortcutManager: WebAppShortcutManager,
    private val thumbnailStorage: ThumbnailStorage
) {
    /**
     * Use cases that provide engine interactions for a given browser session.
     */
    val sessionUseCases by lazy { SessionUseCases(sessionManager) }

    /**
     * Use cases that provide tab management.
     */
    val tabsUseCases: TabsUseCases by lazy { TabsUseCases(sessionManager) }

    /**
     * Use cases that provide tab thumbnail integration.
     */
    val thumbnailUseCases: ThumbnailsUseCases by lazy {
        ThumbnailsUseCases(
            store,
            thumbnailStorage
        )
    }

    /**
     * Use cases that provide search engine integration.
     */
    val searchUseCases by lazy { SearchUseCases(context, searchEngineManager, sessionManager) }

    /**
     * Use cases that provide settings management.
     */
    val settingsUseCases by lazy { SettingsUseCases(engine, sessionManager) }

    val appLinksUseCases by lazy { AppLinksUseCases(context.applicationContext) }

    val webAppUseCases by lazy {
        WebAppUseCases(context, sessionManager, shortcutManager)
    }

    val downloadUseCases by lazy { DownloadsUseCases(store) }

    val contextMenuUseCases by lazy { ContextMenuUseCases(sessionManager, store) }
}
