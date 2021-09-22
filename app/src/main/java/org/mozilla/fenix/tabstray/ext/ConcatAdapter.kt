/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.ext

import androidx.recyclerview.widget.ConcatAdapter
import org.mozilla.fenix.tabstray.browser.BrowserTabsAdapter
import org.mozilla.fenix.tabstray.browser.TitleHeaderAdapter
import org.mozilla.fenix.tabstray.browser.InactiveTabsAdapter
import org.mozilla.fenix.tabstray.browser.TabGroupAdapter

/**
 * A convenience binding for retrieving the [BrowserTabsAdapter] from the [ConcatAdapter].
 */
internal val ConcatAdapter.browserAdapter
    get() = adapters.find { it is BrowserTabsAdapter } as BrowserTabsAdapter

/**
 * A convenience binding for retrieving the [InactiveTabsAdapter] from the [ConcatAdapter].
 */
internal val ConcatAdapter.inactiveTabsAdapter
    get() = adapters.find { it is InactiveTabsAdapter } as InactiveTabsAdapter

/**
 * A convenience binding for retrieving the [TabGroupAdapter] from the [ConcatAdapter].
 */
internal val ConcatAdapter.tabGroupAdapter
    get() = adapters.find { it is TabGroupAdapter } as TabGroupAdapter

/**
 * A convenience binding for retrieving the [TitleHeaderAdapter] from the [ConcatAdapter].
 */
internal val ConcatAdapter.titleHeaderAdapter
    get() = adapters.find { it is TitleHeaderAdapter } as TitleHeaderAdapter
