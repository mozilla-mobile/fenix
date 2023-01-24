/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplumb

import android.net.Uri
import androidx.core.net.toUri
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.GleanMetrics.Messaging

/**
 * Bookkeeping for message actions in terms of Glean messages and the messaging store.
 */
class NimbusMessagingController(
    private val messagingStorage: NimbusMessagingStorage,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    /**
     * Called when a message is just about to be shown to the user.
     *
     * Update the display count and time shown metadata for the given [message].
     */
    fun updateMessageAsDisplayed(message: Message): Message {
        val updatedMetadata = message.metadata.copy(
            displayCount = message.metadata.displayCount + 1,
            lastTimeShown = now(),
        )
        return message.copy(
            metadata = updatedMetadata,
        )
    }

    /**
     * Records telemetry and metadata for a newly processed displayed message.
     */
    suspend fun onMessageDisplayed(message: Message) {
        sendShownMessageTelemetry(message.id)
        if (message.isExpired) {
            sendExpiredMessageTelemetry(message.id)
        }
        messagingStorage.updateMetadata(message.metadata)
    }

    /**
     * Called when a message has been dismissed by the user.
     *
     * Records a messageDismissed event, and records that the message
     * has been dismissed.
     */
    suspend fun onMessageDismissed(message: Message) {
        sendDismissedMessageTelemetry(message.id)
        val updatedMetadata = message.metadata.copy(dismissed = true)
        messagingStorage.updateMetadata(updatedMetadata)
    }

    /**
     * Once a message is clicked, the action needs to be examined for string substitutions
     * and any `uuid` needs to be recorded in the Glean event.
     *
     * We call this `process` as it has a side effect of logging a Glean event while it
     * creates a URI string for the message action.
     */
    fun processMessageActionToUri(message: Message): Uri {
        val (uuid, action) = messagingStorage.generateUuidAndFormatAction(message.action)
        sendClickedMessageTelemetry(message.id, uuid)

        return action.toDeepLinkSchemeUri()
    }

    /**
     * Called once the user has clicked on a message.
     *
     * This records that the message has been clicked on, but does not record a
     * glean event. That should be done via [processMessageActionToUri].
     */
    suspend fun onMessageClicked(message: Message) {
        val updatedMetadata = message.metadata.copy(pressed = true)
        messagingStorage.updateMetadata(updatedMetadata)
    }

    private fun sendDismissedMessageTelemetry(messageId: String) {
        Messaging.messageDismissed.record(Messaging.MessageDismissedExtra(messageId))
    }

    private fun sendShownMessageTelemetry(messageId: String) {
        Messaging.messageShown.record(Messaging.MessageShownExtra(messageId))
    }

    private fun sendExpiredMessageTelemetry(messageId: String) {
        Messaging.messageExpired.record(Messaging.MessageExpiredExtra(messageId))
    }

    private fun sendClickedMessageTelemetry(messageId: String, uuid: String?) {
        Messaging.messageClicked.record(
            Messaging.MessageClickedExtra(messageKey = messageId, actionUuid = uuid),
        )
    }
}

private fun String.toDeepLinkSchemeUri(): Uri {
    val actionWithDeepLinkScheme = if (startsWith("http", ignoreCase = true)) {
        "${BuildConfig.DEEP_LINK_SCHEME}://open?url=${Uri.encode(this)}"
    } else if (startsWith("://")) {
        "${BuildConfig.DEEP_LINK_SCHEME}$this"
    } else {
        this
    }

    return actionWithDeepLinkScheme.toUri()
}
