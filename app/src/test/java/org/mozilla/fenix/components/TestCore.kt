/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.Settings
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.pwa.WebAppShortcutManager
import mozilla.components.feature.top.sites.DefaultTopSitesStorage
import mozilla.components.support.base.crash.CrashReporting

class TestCore(context: Context, crashReporter: CrashReporting) : Core(context, crashReporter) {

    override val engine = mockk<Engine>(relaxed = true) {
        every { this@mockk getProperty "settings" } returns mockk<Settings>(relaxed = true)
    }
    override val sessionManager = SessionManager(engine)
    override val store = mockk<BrowserStore>()
    override val client = mockk<Client>()
    override val webAppShortcutManager = mockk<WebAppShortcutManager>()
    override val thumbnailStorage = mockk<ThumbnailStorage>()
    override val topSiteStorage = mockk<DefaultTopSitesStorage>()
}
