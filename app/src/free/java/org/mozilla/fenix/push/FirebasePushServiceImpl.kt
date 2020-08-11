/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.push

import android.content.Context
import mozilla.components.concept.push.PushService

class FirebasePushServiceImpl : PushService {
    override fun deleteToken() {}
    override fun isServiceAvailable(context: Context): Boolean { return false }
    override fun start(context: Context) {}
    override fun stop() {}
}
