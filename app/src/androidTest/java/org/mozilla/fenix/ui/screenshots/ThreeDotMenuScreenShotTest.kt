package org.mozilla.fenix.ui.screenshots

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule

import org.junit.After
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.HomeActivityTestRule

import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf

class ThreeDotMenuScreenShotTest:ScreenshotTest() {
    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    @get:Rule
        var mActivityTestRule:ActivityTestRule<HomeActivity> = HomeActivityTestRule()


    @After
        fun tearDown() {
                mActivityTestRule.getActivity().finishAndRemoveTask()
            }

    @Test
        fun threeDotMenu() {

        onView(allOf(withId(R.id.menuButton))).perform(click())
        Screengrab.screenshot("three-dot-menu")
        device.pressBack()

    }

}
