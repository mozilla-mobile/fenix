/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.telemetry

import android.util.Log
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.tabstray.TabsTrayAction
import org.mozilla.fenix.tabstray.TabsTrayState
import org.mozilla.fenix.utils.Settings

/**
 * [Middleware] that reacts to various [TabsTrayAction]s.
 *
 * @property settings reference to the application [Settings].
 * @property metrics reference to the configured [MetricController] to record general page load events.
 */
class TabsTrayMiddleware(
    private val settings: Settings,
    private val metrics: MetricController
) : Middleware<TabsTrayState, TabsTrayAction> {

    override fun invoke(
        context: MiddlewareContext<TabsTrayState, TabsTrayAction>,
        next: (TabsTrayAction) -> Unit,
        action: TabsTrayAction
    ) {
        when (action) {
            is TabsTrayAction.ReportTabMetrics -> {
                if (settings.inactiveTabsAreEnabled) {
                    metrics.track(Event.TabsTrayHasInactiveTabs(action.inactiveTabsCount))
                }

                if (action.tabGroups.isNotEmpty()) {
                    val averageTabsPerGroup = action.tabGroups.map { it.tabs.size }.average()
                    metrics.track(Event.AverageTabsPerSearchTermGroup(averageTabsPerGroup))
                }
                metrics.track(Event.SearchTermGroupCount(action.tabGroups.size))
            }
            else -> {}
        }
    }
}
