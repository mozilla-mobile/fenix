/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.experiments

import android.content.Context
import android.net.Uri
import android.os.StrictMode
import mozilla.components.service.nimbus.Nimbus
import mozilla.components.service.nimbus.NimbusApi
import mozilla.components.service.nimbus.NimbusAppInfo
import mozilla.components.service.nimbus.NimbusDisabled
import mozilla.components.service.nimbus.NimbusServerSettings
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.experiments.nimbus.NimbusInterface
import org.mozilla.experiments.nimbus.internal.EnrolledExperiment
import org.mozilla.experiments.nimbus.internal.NimbusException
import org.mozilla.experiments.nimbus.joinOrTimeout
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.perf.runBlockingIncrement

/**
 * Fenix specific observer of Nimbus events.
 *
 * The generated code `FxNimbus` provides a cache which should be invalidated
 * when the experiments recipes are updated.
 */
private val observer = object : NimbusInterface.Observer {
    override fun onUpdatesApplied(updated: List<EnrolledExperiment>) {
        FxNimbus.invalidateCachedValues()
    }
}

/**
 * The maximum amount of time the app launch will be blocked to load experiments from disk.
 *
 * ⚠️ This value was decided from analyzing the Focus metrics (nimbus_initial_fetch) for the ideal
 * timeout. We should NOT change this value without collecting more metrics first.
 */
private const val TIME_OUT_LOADING_EXPERIMENT_FROM_DISK_MS = 200L

@Suppress("TooGenericExceptionCaught")
fun createNimbus(context: Context, url: String?): NimbusApi {
    val errorReporter: ((String, Throwable) -> Unit) = reporter@{ message, e ->
        Logger.error("Nimbus error: $message", e)

        if (e is NimbusException && !e.isReportableError()) {
            return@reporter
        }

        context.components.analytics.crashReporter.submitCaughtException(e)
    }
    return try {
        // Eventually we'll want to use `NimbusDisabled` when we have no NIMBUS_ENDPOINT.
        // but we keep this here to not mix feature flags and how we configure Nimbus.
        val serverSettings = if (!url.isNullOrBlank()) {
            if (context.settings().nimbusUsePreview) {
                NimbusServerSettings(url = Uri.parse(url), collection = "nimbus-preview")
            } else {
                NimbusServerSettings(url = Uri.parse(url))
            }
        } else {
            null
        }

        // Global opt out state is stored in Nimbus, and shouldn't be toggled to `true`
        // from the app unless the user does so from a UI control.
        // However, the user may have opt-ed out of mako experiments already, so
        // we should respect that setting here.
        val enabled =
            context.components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
                context.settings().isExperimentationEnabled
            }

        // The name "fenix" here corresponds to the app_name defined for the family of apps
        // that encompasses all of the channels for the Fenix app.  This is defined upstream in
        // the telemetry system. For more context on where the app_name come from see:
        // https://probeinfo.telemetry.mozilla.org/v2/glean/app-listings
        // and
        // https://github.com/mozilla/probe-scraper/blob/master/repositories.yaml
        val appInfo = NimbusAppInfo(
            appName = "fenix",
            // Note: Using BuildConfig.BUILD_TYPE is important here so that it matches the value
            // passed into Glean. `Config.channel.toString()` turned out to be non-deterministic
            // and would mostly produce the value `Beta` and rarely would produce `beta`.
            channel = BuildConfig.BUILD_TYPE,
            customTargetingAttributes = mapOf(
                "isFirstRun" to context.settings().isFirstNimbusRun.toString(),
            ),
        )
        Nimbus(context, appInfo, serverSettings, errorReporter).apply {
            // We register our own internal observer for housekeeping the Nimbus SDK and
            // generated code.
            register(observer)

            val isFirstNimbusRun = context.settings().isFirstNimbusRun

            // We always want `Nimbus.initialize` to happen ASAP and before any features (engine/UI)
            // have been initialized. For that reason, we use runBlocking here to avoid
            // inconsistency in the experiments.
            // We can safely do this because Nimbus does most of it's work on background threads,
            // except for loading the initial experiments from disk. For this reason, we have a
            // `joinOrTimeout` to limit the blocking until TIME_OUT_LOADING_EXPERIMENT_FROM_DISK_MS.
            runBlockingIncrement {
                val job = initialize(
                    isFirstNimbusRun || url.isNullOrBlank(),
                    R.raw.initial_experiments,
                )
                // We only read from disk when loading first-run experiments. This is the only time
                // that we should join and block. Otherwise, we don't want to wait.
                if (isFirstNimbusRun) {
                    context.settings().isFirstNimbusRun = false
                    job.joinOrTimeout(TIME_OUT_LOADING_EXPERIMENT_FROM_DISK_MS)
                }
            }

            if (!enabled) {
                // This opts out of nimbus experiments. It involves writing to disk, so does its
                // work on the db thread.
                globalUserParticipation = enabled
            }
        }
    } catch (e: Throwable) {
        // Something went wrong. We'd like not to, but stability of the app is more important than
        // failing fast here.
        errorReporter("Failed to initialize Nimbus", e)
        NimbusDisabled(context)
    }
}

/**
 * Classifies which errors we should forward to our crash reporter or not. We want to filter out the
 * non-reportable ones if we know there is no reasonable action that we can perform.
 *
 * This fix should be upstreamed as part of: https://github.com/mozilla/application-services/issues/4333
 */
fun NimbusException.isReportableError(): Boolean {
    return when (this) {
        is NimbusException.RequestException,
        is NimbusException.ResponseException,
        -> false
        else -> true
    }
}
