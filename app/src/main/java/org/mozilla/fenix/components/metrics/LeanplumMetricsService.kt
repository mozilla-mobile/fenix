/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.app.Application
import org.mozilla.fenix.home.intent.DeepLinkIntentProcessor
import java.util.UUID

abstract class LeanplumMetricsService(
    private val application: Application,
    private val deviceIdGenerator: () -> String = { UUID.randomUUID().toString() }
) : MetricsService, DeepLinkIntentProcessor.DeepLinkVerifier

