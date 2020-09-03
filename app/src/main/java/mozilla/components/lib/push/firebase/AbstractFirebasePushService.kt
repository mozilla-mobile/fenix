/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.push.firebase

import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mozilla.components.concept.push.PushService

abstract class AbstractFirebasePushService() : FirebaseMessagingService(), PushService {

    override fun start(context: Context) {
    }

    override fun onNewToken(newToken: String) {
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
    }

    final override fun stop() {
    }

    override fun deleteToken() {
    }

    override fun isServiceAvailable(context: Context): Boolean {
        return false
    }
}
