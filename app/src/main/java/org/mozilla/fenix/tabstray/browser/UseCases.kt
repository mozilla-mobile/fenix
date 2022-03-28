/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.fenix.GleanMetrics.TabsTray

class SelectTabUseCaseWrapper(
    private val selectTab: TabsUseCases.SelectTabUseCase,
    private val onSelect: (String) -> Unit
) : TabsUseCases.SelectTabUseCase {
    operator fun invoke(tabId: String, source: String? = null) {
        TabsTray.openedExistingTab.record(TabsTray.OpenedExistingTabExtra(source ?: "unknown"))
        selectTab(tabId)
        onSelect(tabId)
    }

    override fun invoke(tabId: String) {
        invoke(tabId, null)
    }
}

class RemoveTabUseCaseWrapper(
    private val onRemove: (String) -> Unit,
) : TabsUseCases.RemoveTabUseCase {
    operator fun invoke(tabId: String, source: String? = null) {
        TabsTray.closedExistingTab.record(TabsTray.ClosedExistingTabExtra(source ?: "unknown"))
        onRemove(tabId)
    }

    override fun invoke(tabId: String) {
        invoke(tabId, null)
    }
}
