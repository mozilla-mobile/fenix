package org.mozilla.fenix.components.metrics

import android.app.Application
import org.mozilla.fenix.home.intent.DeepLinkIntentProcessor
import java.util.UUID

abstract class LeanplumMetricsService(
    private val application: Application,
    private val deviceIdGenerator: () -> String = { UUID.randomUUID().toString() }
) : MetricsService, DeepLinkIntentProcessor.DeepLinkVerifier

