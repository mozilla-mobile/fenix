/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import io.mockk.mockk
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.browser.storage.sync.RemoteTabsStorage
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.sync.logins.SyncableLoginsStorage

class TestBackgroundServices(
    context: Context,
    push: Push = mockk(relaxed = true),
    crashReporter: CrashReporter = mockk(relaxed = true),
    historyStorage: Lazy<PlacesHistoryStorage> = mockk(relaxed = true),
    bookmarksStorage: Lazy<PlacesBookmarksStorage> = mockk(relaxed = true),
    passwordsStorage: Lazy<SyncableLoginsStorage> = mockk(relaxed = true),
    remoteTabsStorage: Lazy<RemoteTabsStorage> = mockk(relaxed = true)
) : BackgroundServices(
    context,
    push,
    crashReporter,
    historyStorage,
    bookmarksStorage,
    passwordsStorage,
    remoteTabsStorage
) {
    override val accountManager = mockk<FxaAccountManager>(relaxed = true)
}
