/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gleanplum

import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.HomeActivity

/**
 * Handles default interactions with an ui message.
 */
class DefaultMessageController(
    private val messageManager: MessagesManager,
    private val homeActivity: HomeActivity
) : MessageController {

    override fun onMessagePressed(message: Message) {
        // TODO: report telemetry event
        messageManager.onMessagePressed(message)
        handleAction(message.data.action)
    }

    override fun onMessageDismissed(message: Message) {
        // TODO: report telemetry event
        messageManager.onMessageDismissed(message)
    }

    override fun onMessageDisplayed(message: Message) {
        // TODO: report telemetry event
        messageManager.onMessageDisplayed(message)
    }

    @VisibleForTesting
    internal fun handleAction(action: String): Intent {
        val url = if (action.startsWith("http", ignoreCase = true)) {
            "open?url=$action"
        } else {
            action
        }

        val intent =
            Intent(Intent.ACTION_VIEW, "${BuildConfig.DEEP_LINK_SCHEME}://$url".toUri())
        homeActivity.processIntent(intent)

        return intent
    }

}