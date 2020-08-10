/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.app.Application
import java.util.UUID

class LeanplumMetricsServiceImpl(
    private val application: Application,
    private val deviceIdGenerator: () -> String = { UUID.randomUUID().toString() }
) : MetricsService {
    override val type = MetricServiceType.Marketing
    override fun start() {}
    override fun stop() {}
    override fun track(event: Event) {}
    override fun shouldTrack(event: Event): Boolean {return false;}
}
