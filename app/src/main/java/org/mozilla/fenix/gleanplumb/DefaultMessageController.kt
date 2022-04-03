/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplumb

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.MessageClicked
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.MessageDismissed
import org.mozilla.fenix.components.appstate.AppAction.MessagingAction.MessageDisplayed
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController

/**
 * Handles default interactions with the ui of GleanPlumb messages.
 */
class DefaultMessageController(
    private val appStore: AppStore,
    private val metrics: MetricController,
    private val messagingStorage: NimbusMessagingStorage,
    private val homeActivity: HomeActivity
) : MessageController {

    override fun onMessagePressed(message: Message) {
        val result = messagingStorage.getMessageAction(message)
        val uuid = result.first
        val action = result.second
        metrics.track(Event.Messaging.MessageClicked(message.id, uuid))
        handleAction(action)
        appStore.dispatch(MessageClicked(message))
    }

    override fun onMessageDismissed(message: Message) {
        metrics.track(Event.Messaging.MessageDismissed(message.id))
        appStore.dispatch(MessageDismissed(message))
    }

    override fun onMessageDisplayed(message: Message) {
        if (message.maxDisplayCount <= message.metadata.displayCount + 1) {
            metrics.track(Event.Messaging.MessageExpired(message.id))
        }
        metrics.track(Event.Messaging.MessageShown(message.id))
        appStore.dispatch(MessageDisplayed(message))
    }

    @VisibleForTesting
    internal fun handleAction(action: String): Intent {
        val partialAction = if (action.startsWith("http", ignoreCase = true)) {
            "://open?url=${Uri.encode(action)}"
        } else {
            action
        }
        val intent =
            Intent(Intent.ACTION_VIEW, "${BuildConfig.DEEP_LINK_SCHEME}$partialAction".toUri())
        homeActivity.processIntent(intent)

        return intent
    }
}
