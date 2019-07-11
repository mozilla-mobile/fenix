/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.quickactionsheet

import android.content.Context
import androidx.annotation.CallSuper
import mozilla.components.browser.session.Session
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.ItsNotBrokenSnack

class QuickActionInteractor(
    private val context: Context,
    private val readerViewFeature: ViewBoundFeatureWrapper<ReaderViewFeature>,
    private val quickActionStore: QuickActionSheetStore,
    private val shareUrl: (String) -> Unit,
    private val bookmarkTapped: (Session) -> Unit
) : QuickActionSheetInteractor {

    private val metrics
        inline get() = context.components.analytics.metrics
    private val selectedSession
        inline get() = context.components.core.sessionManager.selectedSession

    @CallSuper
    override fun onOpened() {
        metrics.track(Event.QuickActionSheetOpened)
    }

    @CallSuper
    override fun onClosed() {
        metrics.track(Event.QuickActionSheetClosed)
    }

    @CallSuper
    override fun onSharedPressed() {
        metrics.track(Event.QuickActionSheetShareTapped)
        selectedSession?.url?.let(shareUrl)
    }

    @CallSuper
    override fun onDownloadsPressed() {
        metrics.track(Event.QuickActionSheetDownloadTapped)
        ItsNotBrokenSnack(context).showSnackbar(issueNumber = "348")
    }

    @CallSuper
    override fun onBookmarkPressed() {
        metrics.track(Event.QuickActionSheetBookmarkTapped)
        selectedSession?.let(bookmarkTapped)
    }

    @CallSuper
    override fun onReadPressed() {
        metrics.track(Event.QuickActionSheetReadTapped)
        readerViewFeature.withFeature { feature ->
            val enabled = selectedSession?.readerMode ?: false
            if (enabled) {
                feature.hideReaderView()
            } else {
                feature.showReaderView()
            }
            quickActionStore.dispatch(QuickActionSheetAction.ReaderActiveStateChange(!enabled))
        }
    }

    @CallSuper
    override fun onReadApperancePressed() {
        // TODO telemetry: https://github.com/mozilla-mobile/fenix/issues/2267
        readerViewFeature.withFeature { feature ->
            feature.showControls()
        }
    }
}
