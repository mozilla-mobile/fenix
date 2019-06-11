/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.ui.screenshots
import androidx.test.rule.ActivityTestRule
import org.junit.After
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import android.os.SystemClock
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf
class DefaultHomeScreenTest:ScreenshotTest() {
    @Rule @JvmField
    val localeTestRule = LocaleTestRule()
    @get:Rule
        var mActivityTestRule:ActivityTestRule<HomeActivity> = HomeActivityTestRule()
        //var mActivityTestRule = ActivityTestRule(HomeActivity::class.java, false, false)
    @After
            fun tearDown() {
                mActivityTestRule.getActivity().finishAndRemoveTask()
          }
    @Test
        fun showDefaultHomeScreen() {
            onView(allOf(withId(R.id.homeLayout), isDisplayed(), hasFocus()))
            onView(allOf(withId(R.id.toolbar), isDisplayed()))
            SystemClock.sleep(5000)
            Screengrab.screenshot("home-screen")
          }
        }