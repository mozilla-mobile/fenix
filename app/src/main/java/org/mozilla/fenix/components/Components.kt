/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import org.mozilla.fenix.test.Mockable

/**
 * Provides access to all components.
 */
@Mockable
class Components(private val context: Context) {
    val backgroundServices by lazy { BackgroundServices(context, core.historyStorage, core.bookmarksStorage) }
    val services by lazy { Services(backgroundServices.accountManager, useCases.tabsUseCases) }
    val core by lazy { Core(context) }
    val search by lazy { Search(context) }
    val useCases by lazy { UseCases(context, core.sessionManager, search.searchEngineManager) }
    val utils by lazy { Utilities(context, core.sessionManager, useCases.sessionUseCases, useCases.searchUseCases) }
    val analytics by lazy { Analytics(context) }
    val storage by lazy { Storage(context) }
}
