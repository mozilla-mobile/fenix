/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION")

package org.mozilla.fenix.helpers

import android.app.Activity
import android.view.ViewConfiguration.getLongPressTimeout
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiSelector
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.onboarding.FenixOnboarding

/**
 * A [org.junit.Rule] to handle shared test set up for tests on [HomeActivity].
 *
 * @param initialTouchMode See [ActivityTestRule]
 * @param launchActivity See [ActivityTestRule]
 */

class HomeActivityTestRule(
    initialTouchMode: Boolean = false,
    launchActivity: Boolean = true,
    private val skipOnboarding: Boolean = false,
) :
    ActivityTestRule<HomeActivity>(HomeActivity::class.java, initialTouchMode, launchActivity) {

    /**
     * Helper for updating various app settings that could interfere with the tests.
     * Tests that use [HomeActivityTestRule] are expected to rely on this [FeatureSettingsHelper]
     * instead of instantiating their own.
     *
     * The main benefit this brings is better ordering of operations with the settings cleanup
     * automatically happening just before the [Activity] under test finishes which may as opposed to
     * cleanup happening earlier and modifying the app behavior.
     */
    val featureSettingsHelper = FeatureSettingsHelper()

    private val longTapUserPreference = getLongPressTimeout()

    override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        setLongTapTimeout(3000)
        if (skipOnboarding) { skipOnboardingBeforeLaunch() }
    }

    override fun afterActivityFinished() {
        super.afterActivityFinished()
        setLongTapTimeout(longTapUserPreference)
        featureSettingsHelper.resetAllFeatureFlags()
        closeNotificationShade()
    }
}

/**
 * A [org.junit.Rule] to handle shared test set up for tests on [HomeActivity]. This adds
 * functionality for using the Espresso-intents api, and extends from ActivityTestRule.
 *
 * @param initialTouchMode See [IntentsTestRule]
 * @param launchActivity See [IntentsTestRule]
 */

class HomeActivityIntentTestRule(
    initialTouchMode: Boolean = false,
    launchActivity: Boolean = true,
    private val skipOnboarding: Boolean = false,
) :
    IntentsTestRule<HomeActivity>(HomeActivity::class.java, initialTouchMode, launchActivity) {
    private val longTapUserPreference = getLongPressTimeout()

    override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        setLongTapTimeout(3000)
        if (skipOnboarding) { skipOnboardingBeforeLaunch() }
    }

    override fun afterActivityFinished() {
        super.afterActivityFinished()
        setLongTapTimeout(longTapUserPreference)
        closeNotificationShade()
    }
}

// changing the device preference for Touch and Hold delay, to avoid long-clicks instead of a single-click
fun setLongTapTimeout(delay: Int) {
    // Issue: https://github.com/mozilla-mobile/fenix/issues/25132
    var attempts = 0
    while (attempts++ < 3) {
        try {
            mDevice.executeShellCommand("settings put secure long_press_timeout $delay")
            break
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
    }
}

private fun skipOnboardingBeforeLaunch() {
    // The production code isn't aware that we're using
    // this API so it can be fragile.
    FenixOnboarding(appContext).finish()
}

private fun closeNotificationShade() {
    if (mDevice.findObject(
            UiSelector().resourceId("com.android.systemui:id/notification_stack_scroller"),
        ).exists()
    ) {
        mDevice.pressHome()
    }
}
