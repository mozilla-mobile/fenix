/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import mozilla.components.service.glean.private.TimespanMetricType
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.home.sessioncontrol.viewholders.topsites.TopSiteItemViewHolder
import org.mozilla.fenix.perf.StartupTimelineStateMachine.StartupActivity
import org.mozilla.fenix.perf.StartupTimelineStateMachine.StartupDestination
import org.mozilla.fenix.perf.StartupTimelineStateMachine.StartupState

/**
 * A collection of functionality to instrument, measure, and understand startup performance. The
 * responsibilities of this class are to update the internal [StartupState] based on the methods
 * called and to delegate calls to its dependencies, which handle other functionality related to
 * understanding startup.
 *
 * This class, and its dependencies, may need to be modified for any changes in startup.
 *
 * This class is not thread safe and should only be called from the main thread.
 */
@UiThread
object StartupTimeline {

    private var state: StartupState = StartupState.Cold(StartupDestination.UNKNOWN)

    val homeActivityLifecycleObserver = StartupHomeActivityLifecycleObserver()
    private val reportFullyDrawn = StartupReportFullyDrawn()

    fun onActivityCreateEndIntentReceiver() {
        advanceState(StartupActivity.INTENT_RECEIVER)
    }

    fun onActivityCreateEndHome(activity: HomeActivity) {
        advanceState(StartupActivity.HOME)
        reportFullyDrawn.onActivityCreateEndHome(state, activity)
    }

    fun onTopSitesItemBound(holder: TopSiteItemViewHolder) {
        // no advanceState associated with this method.
        reportFullyDrawn.onTopSitesItemBound(state, holder)
    }

    private fun advanceState(startingActivity: StartupActivity) {
        state = StartupTimelineStateMachine.getNextState(state, startingActivity)
    }

    /**
     * Measures the given [measuredFunction] under the given [metric].
     *
     * For debug purposes, measured values may be logged to logcat or seen in the Glean Debug View by:
     * - Enabling telemetry if it's not already enabled
     * - Following the steps in the Glean docs:
     *   https://mozilla.github.io/glean/book/user/debugging/android.html
     *
     * We prefer to aggregate all measurements through this class, rather than using the metrics
     * directly, in order to:
     * - Centralize startup performance analysis in this class
     * - Be able to quickly find all startup measurements with tooling (i.e. "find usages")
     * - Easily modify the code that runs for all measurements
     *
     * We use Glean for all of our measurements to avoid reimplementing non-trivial measurement code
     * that already exists and to ensure the values reported by telemetry are the same that are
     * extracted by our perf test CI.
     *
     * Example usage:
     * ```
     * import ...Telemetry.geckoRuntimeCreate
     *
     * val runtime = StartupTimeline.measure(geckoRuntimeCreate) {
     *     GeckoRuntime.create()
     * }
     * ```
     *
     * @return the return value of [measuredFunction].
     */
    @Suppress("TooGenericExceptionCaught") // we need to catch everything to cancel correctly.
    inline fun <R> measure(metric: TimespanMetricType, measuredFunction: MeasuredFunction<R>): R {
        metric.start()

        val returnValue = try {
            measuredFunction()
        } catch (e: Exception) {
            metric.cancel()
            throw e
        }

        metric.stop()
        return returnValue
    }
}

typealias MeasuredFunction<R> = () -> R

/**
 * A [LifecycleObserver] for [HomeActivity] focused on startup performance measurement.
 */
class StartupHomeActivityLifecycleObserver : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        // Startup metrics placed in the Activity should be re-recorded each time the Activity
        // is started so we need to clear the ping lifetime by submitting once per each startup.
        // It's less complex to add it here rather than the visual completeness task manager.
        //
        // N.B.: this submission location may need to be changed if we add metrics outside of the
        // HomeActivity startup path (e.g. if the user goes directly to a separate activity and
        // closes the app, they will never hit this) to appropriately adjust for the ping lifetimes.
        Pings.startupTimeline.submit()
    }
}
