/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.components.metrics

import android.content.Context
import mozilla.components.service.glean.Glean
import mozilla.components.service.glean.private.NoExtraKeys
import mozilla.components.support.utils.Browsers
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.GleanMetrics.CrashReporter
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.FindInPage
import org.mozilla.fenix.GleanMetrics.ContextMenu
import org.mozilla.fenix.GleanMetrics.QuickActionSheet
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.utils.Settings

private class EventWrapper<T : Enum<T>>(
    private val recorder: ((Map<T, String>?) -> Unit),
    private val keyMapper: ((String) -> T)? = null
) {
    private val String.asCamelCase: String
        get() = this.split("_").reduceIndexed { index, acc, s ->
            if (index == 0) acc + s
            else acc + s.capitalize()
        }

    fun track(event: Event) {
        val extras = if (keyMapper != null) {
            event.extras?.mapKeys { keyMapper.invoke(it.key.asCamelCase) }
        } else {
            null
        }

        this.recorder(extras)
    }
}

private val Event.wrapper
    get() = when (this) {
        is Event.OpenedApp -> EventWrapper(
            { Events.appOpened.record(it) },
            { Events.appOpenedKeys.valueOf(it) }
        )
        is Event.SearchBarTapped -> EventWrapper(
            { Events.searchBarTapped.record(it) },
            { Events.searchBarTappedKeys.valueOf(it) }
        )
        is Event.EnteredUrl -> EventWrapper(
            { Events.enteredUrl.record(it) },
            { Events.enteredUrlKeys.valueOf(it) }
        )
        is Event.PerformedSearch -> EventWrapper(
            { Events.performedSearch.record(it) },
            { Events.performedSearchKeys.valueOf(it) }
        )
        is Event.FindInPageOpened -> EventWrapper<NoExtraKeys>(
            { FindInPage.opened.record(it) }
        )
        is Event.FindInPageClosed -> EventWrapper<NoExtraKeys>(
            { FindInPage.closed.record(it) }
        )
        is Event.FindInPageNext -> EventWrapper<NoExtraKeys>(
            { FindInPage.nextResult.record(it) }
        )
        is Event.FindInPagePrevious -> EventWrapper<NoExtraKeys>(
            { FindInPage.previousResult.record(it) }
        )
        is Event.FindInPageSearchCommitted -> EventWrapper<NoExtraKeys>(
            { FindInPage.searchedPage.record(it) }
        )
        is Event.ContextMenuItemTapped -> EventWrapper(
            { ContextMenu.itemTapped.record(it) },
            { ContextMenu.itemTappedKeys.valueOf(it) }
        )
        is Event.CrashReporterOpened -> EventWrapper<NoExtraKeys>(
            { CrashReporter.opened }
        )
        is Event.CrashReporterClosed -> EventWrapper(
            { CrashReporter.closed },
            { CrashReporter.closedKeys.valueOf(it) }
        )
        is Event.BrowserMenuItemTapped -> EventWrapper(
            { Events.browserMenuAction },
            { Events.browserMenuActionKeys.valueOf(it) }
        )
        is Event.QuickActionSheetOpened -> EventWrapper<NoExtraKeys>(
            { QuickActionSheet.opened.record(it) }
        )
        is Event.QuickActionSheetClosed -> EventWrapper<NoExtraKeys>(
            { QuickActionSheet.closed.record(it) }
        )
        is Event.QuickActionSheetShareTapped -> EventWrapper<NoExtraKeys>(
            { QuickActionSheet.shareTapped.record(it) }
        )
        is Event.QuickActionSheetBookmarkTapped -> EventWrapper<NoExtraKeys>(
            { QuickActionSheet.bookmarkTapped.record(it) }
        )
        is Event.QuickActionSheetDownloadTapped -> EventWrapper<NoExtraKeys>(
            { QuickActionSheet.downloadTapped.record(it) }
        )
        is Event.QuickActionSheetReadTapped -> EventWrapper<NoExtraKeys>(
            { QuickActionSheet.readTapped.record(it) }
        )

        // Don't track other events with Glean
        else -> null
    }

class GleanMetricsService(private val context: Context) : MetricsService {
    override fun start() {
        Glean.initialize(context)
        Glean.setUploadEnabled(IsGleanEnabled)

        Metrics.apply {
            defaultBrowser.set(Browsers.all(context).isDefaultBrowser)
        }
    }

    override fun stop() {
        Glean.setUploadEnabled(false)
    }

    override fun track(event: Event) {
        event.wrapper?.track(event)
    }

    override fun shouldTrack(event: Event): Boolean {
        return Settings.getInstance(context).isTelemetryEnabled && event.wrapper != null
    }

    companion object {
        private const val IsGleanEnabled = BuildConfig.TELEMETRY
    }
}
