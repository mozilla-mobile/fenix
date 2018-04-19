/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.fenix

import android.app.Application
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory

class FenixApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initializeCrashReporting()
    }

    private fun initializeCrashReporting() {
        if (BuildConfig.SENTRY_TOKEN != null) {
            Sentry.init(BuildConfig.SENTRY_TOKEN, AndroidSentryClientFactory(this))
        }
    }
}
