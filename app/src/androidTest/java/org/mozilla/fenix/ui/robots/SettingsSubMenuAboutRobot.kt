/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.helpers.TestHelper

/**
 * Implementation of Robot Pattern for the settings search sub menu.
 */
class SettingsSubMenuAboutRobot {
    fun verifyAboutFirefoxView() = assertAboutFirefoxText()
    fun verifyFirefoxBuild() = assertFirefoxBuild()
    fun verifyFirefoxVersion() = assertFirefoxVersion()
    fun verifyProducedByText() = assertProducedByText()
    fun verifyBuildDate() = assertBuildDate()
    fun verifyLibrariesText() = assertLibrariesText()
    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            TestHelper.clickGoBackButton()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}
private fun assertFirefoxBuild() = Espresso.onView(ViewMatchers.withSubstring(BuildConfig.VERSION_NAME))
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertFirefoxVersion() = Espresso.onView(ViewMatchers.withSubstring(
    mozilla.components.Build.version + ", " + mozilla.components.Build.gitHash))
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertProducedByText() = Espresso.onView(
    ViewMatchers.withSubstring(
        "Firefox Preview is" +
                " produced by Mozilla"
    )
)
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertBuildDate() = Espresso.onView(ViewMatchers.withSubstring(BuildConfig.BUILD_DATE))
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertLibrariesText() = Espresso.onView(
    ViewMatchers.withSubstring(
        "Open source libraries" +
                " we use"
    )
)
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertAboutFirefoxText() = Espresso.onView(ViewMatchers.withText("About Firefox Preview"))
    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
