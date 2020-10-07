/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// This class implements the alternative ways to suppress StrictMode with performance
// monitoring by wrapping the raw methods. This lint check tells us not to use the raw
// methods so we suppress the check.
@file:Suppress("MozillaStrictModeSuppression")

package org.mozilla.fenix

import android.os.Build
import android.os.Looper
import android.os.StrictMode
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import mozilla.components.support.ktx.android.os.resetAfter
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.perf.Performance
import org.mozilla.fenix.utils.Mockable

private const val MANUFACTURE_HUAWEI: String = "HUAWEI"
private const val MANUFACTURE_ONE_PLUS: String = "OnePlus"

private val logger = Performance.logger
private val mainLooper = Looper.getMainLooper()

/**
 * Manages strict mode settings for the application.
 */
@Mockable
class StrictModeManager(
    config: Config,

    // Ideally, we'd pass in a more specific value but there is a circular dependency: StrictMode
    // is passed into Core but we'd need to pass in Core here. Instead, we take components and later
    // fetch the value we need from it.
    private val components: Components
) {

    private val isEnabledByBuildConfig = config.channel.isDebug

    /**
     * The number of times StrictMode has been suppressed. StrictMode can be used to prevent main
     * thread IO but it's easy to suppress. We use this value, in combination with:
     * - a test: that fails if the suppression count increases
     * - a lint check: to ensure this value gets used instead of functions that work around it
     * - code owners: to prevent modifications to these above items without perf knowing
     * to make suppressions a more deliberate act.
     */
    @VisibleForTesting(otherwise = PRIVATE)
    var suppressionCount: Long = 0

    /***
     * Enables strict mode for debug purposes. meant to be run only in the main process.
     * @param setPenaltyDeath boolean value to decide setting the penaltyDeath as a penalty.
     */
    fun enableStrictMode(setPenaltyDeath: Boolean) {
        if (isEnabledByBuildConfig) {
            val threadPolicy = StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
            if (setPenaltyDeath && Build.MANUFACTURER !in strictModeExceptionList) {
                threadPolicy.penaltyDeath()
            }
            StrictMode.setThreadPolicy(threadPolicy.build())

            val builder = StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .penaltyLog()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.detectContentUriWithoutPermission()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.detectNonSdkApiUsage()
            }
            StrictMode.setVmPolicy(builder.build())
        }
    }

    /**
     * Revert strict mode to disable penalty based on fragment lifecycle since strict mode
     * needs to switch to penalty logs. Using the fragment life cycle allows decoupling from any
     * specific fragment.
     */
    fun attachListenerToDisablePenaltyDeath(fragmentManager: FragmentManager) {
        fragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                enableStrictMode(setPenaltyDeath = false)
                fm.unregisterFragmentLifecycleCallbacks(this)
            }
        }, false)
    }

    /**
     * Runs the given [functionBlock] and sets the given [StrictMode.ThreadPolicy] after its
     * completion when in a build configuration that has StrictMode enabled. If StrictMode is
     * not enabled, simply runs the [functionBlock]. This function is written in the style of
     * [AutoCloseable.use].
     *
     * This function contains perf improvements so it should be
     * called instead of [mozilla.components.support.ktx.android.os.resetAfter] (using the wrong
     * method should be prevented by a lint check). This is significantly less convenient to run than
     * when it was written as an extension function on [StrictMode.ThreadPolicy] but I think this is
     * okay: it shouldn't be easy to ignore StrictMode.
     *
     * @return the value returned by [functionBlock].
     */
    fun <R> resetAfter(policy: StrictMode.ThreadPolicy, functionBlock: () -> R): R {
        // Calling resetAfter takes 1-2ms (unknown device) so we only execute it if StrictMode can
        // actually be enabled. https://github.com/mozilla-mobile/fenix/issues/11617
        return if (isEnabledByBuildConfig) {
            // This can overflow and crash. However, it's unlikely we'll suppress StrictMode 9
            // quintillion times in a build config where StrictMode is enabled so we don't handle it
            // because it'd increase complexity.
            suppressionCount += 1

            // We log so that devs are more likely to notice that we're suppressing StrictMode violations.
            // We add profiler markers so that the perf team can easily identify IO locations in profiles.
            logger.warn("StrictMode violation suppressed: #$suppressionCount")
            if (Thread.currentThread() == mainLooper.thread) { // markers only supported on main thread.
                components.core.engine.profiler?.addMarker("StrictMode.suppression", "Count: $suppressionCount")
            }

            policy.resetAfter(functionBlock)
        } else {
            functionBlock()
        }
    }

    /**
     * There are certain manufacturers that have custom font classes for the OS systems.
     * These classes violates the [StrictMode] policies on startup. As a workaround, we create
     * an exception list for these manufacturers so that dialogs do not show up on start up.
     * To add a new manufacturer to the list, log "Build.MANUFACTURER" from the device to get the
     * exact name of the manufacturer.
     */
    private val strictModeExceptionList = setOf(MANUFACTURE_HUAWEI, MANUFACTURE_ONE_PLUS)
}
