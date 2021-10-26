/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import androidx.annotation.VisibleForTesting
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
<<<<<<< HEAD
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.feature.tabs.ext.toTabs
=======
import mozilla.components.browser.tabstray.TabsTray
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
>>>>>>> a2c7dfb26 (For #22170: Add telemetry for the auto-close prompt)
import org.mozilla.fenix.utils.Settings

class InactiveTabsAutoCloseDialogController(
    private val browserStore: BrowserStore,
    private val settings: Settings,
    private val tabFilter: (TabSessionState) -> Boolean,
    private val tray: TabsTray,
    private val metrics: MetricController
) {
    /**
     * Dismiss the auto-close dialog.
     */
    fun close() {
        settings.hasInactiveTabsAutoCloseDialogBeenDismissed = true
        refeshInactiveTabsSecion()
        metrics.track(Event.TabsTrayAutoCloseDialogDismissed)
    }

    /**
     * Enable the auto-close feature with the after a month setting.
     */
    fun enableAutoClosed() {
        settings.closeTabsAfterOneMonth = true
        settings.closeTabsAfterOneWeek = false
        settings.closeTabsAfterOneDay = false
        settings.manuallyCloseTabs = false
        refeshInactiveTabsSecion()
        metrics.track(Event.TabsTrayAutoCloseDialogTurnOnClicked)
    }

    @VisibleForTesting
    internal fun refeshInactiveTabsSecion() {
        val tabs = browserStore.state.toTabs { tabFilter.invoke(it) }
        tray.updateTabs(tabs)
    }
}
