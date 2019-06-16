/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import org.mozilla.fenix.test.Mockable

/**
 * Provides access to all components.
 */
@Mockable
class Components(private val context: Context) : IComponents {

    override val backgroundServices by lazy {
        BackgroundServices(context, core.historyStorage, core.bookmarksStorage, utils.notificationManager)
    }
    override val services by lazy { Services(backgroundServices.accountManager) }
    override val core by lazy { Core(context) }
    override val search by lazy { Search(context) }
    override val useCases by lazy {
        UseCases(
            context,
            core.sessionManager,
            core.engine.settings,
            search.searchEngineManager
        )
    }
    override val utils by lazy {
        Utilities(
            context,
            core.sessionManager,
            useCases.sessionUseCases,
            useCases.searchUseCases
        )
    }
    override val analytics by lazy { Analytics(context) }
    override val publicSuffixList by lazy { PublicSuffixList(context) }
}
