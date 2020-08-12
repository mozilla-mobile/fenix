/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.app.Application
import android.net.Uri
import java.util.UUID

@SuppressWarnings("EmptyFunctionBlock")
class LeanplumMetricsServiceImpl(
    application: Application,
    deviceIdGenerator: () -> String = { UUID.randomUUID().toString() }
) : LeanplumMetricsService(application, deviceIdGenerator) {
    override val type = MetricServiceType.Marketing
    override fun start() {}
    override fun stop() {}
    override fun track(event: Event) {}
    override fun shouldTrack(event: Event): Boolean { return false }
    override fun verifyDeepLink(deepLink: Uri): Boolean { return false }
}
