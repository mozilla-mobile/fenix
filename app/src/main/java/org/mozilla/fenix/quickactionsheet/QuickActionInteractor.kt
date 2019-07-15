/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.quickactionsheet

import android.content.Context
import androidx.annotation.CallSuper
import mozilla.components.browser.session.Session
import org.mozilla.fenix.browser.readermode.ReaderModeController
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.utils.ItsNotBrokenSnack

class QuickActionInteractor(
    private val context: Context,
    private val readerModeController: ReaderModeController,
    private val quickActionStore: QuickActionSheetStore,
    private val shareUrl: (String) -> Unit,
    private val bookmarkTapped: (Session) -> Unit
) : QuickActionSheetInteractor {

    private val selectedSession
        inline get() = context.components.core.sessionManager.selectedSession

    @CallSuper
    override fun onOpened() {
        context.metrics.track(Event.QuickActionSheetOpened)
    }

    @CallSuper
    override fun onClosed() {
        context.metrics.track(Event.QuickActionSheetClosed)
    }

    @CallSuper
    override fun onSharedPressed() {
        context.metrics.track(Event.QuickActionSheetShareTapped)
        selectedSession?.url?.let(shareUrl)
    }

    @CallSuper
    override fun onDownloadsPressed() {
        context.metrics.track(Event.QuickActionSheetDownloadTapped)
        ItsNotBrokenSnack(context).showSnackbar(issueNumber = "348")
    }

    @CallSuper
    override fun onBookmarkPressed() {
        context.metrics.track(Event.QuickActionSheetBookmarkTapped)
        selectedSession?.let(bookmarkTapped)
    }

    @CallSuper
    override fun onReadPressed() {
        context.metrics.track(Event.QuickActionSheetReadTapped)
        val enabled = selectedSession?.readerMode ?: false
        if (enabled) {
            readerModeController.hideReaderView()
        } else {
            readerModeController.showReaderView()
        }
        quickActionStore.dispatch(QuickActionSheetAction.ReaderActiveStateChange(!enabled))
    }

    @CallSuper
    override fun onAppearancePressed() {
        // TODO telemetry: https://github.com/mozilla-mobile/fenix/issues/2267
        readerModeController.showReaderView()
    }
}
