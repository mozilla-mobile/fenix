/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import mozilla.components.lib.publicsuffixlist.PublicSuffixList

internal class TestComponents : IComponents {

    override lateinit var backgroundServices: BackgroundServices
    override lateinit var services: Services
    override lateinit var core: Core
    override lateinit var search: Search
    override lateinit var useCases: UseCases
    override lateinit var utils: Utilities
    override lateinit var analytics: Analytics
    override lateinit var publicSuffixList: PublicSuffixList
}
