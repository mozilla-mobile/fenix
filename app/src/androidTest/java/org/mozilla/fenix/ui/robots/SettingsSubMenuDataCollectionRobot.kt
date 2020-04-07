/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

/**
 * Implementation of Robot Pattern for the settings Data Collection sub menu.
 */

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.assertIsEnabled
import org.mozilla.fenix.helpers.click

class SettingsSubMenuDataCollectionRobot {

    fun verifyNavigationToolBarHeader() = assertNavigationToolBarHeader()

    fun verifyDataCollectionOptions() = assertDataCollectionOptions()

    fun verifyUsageAndTechnicalDataSwitchDefault() = assertUsageAndTechnicalDataSwitchDefault()

    fun verifyMarketingDataSwitchDefault() = assertMarketingDataValueSwitchDefault()

    fun verifyDataCollectionSubMenuItems() {
        verifyDataCollectionOptions()
        verifyUsageAndTechnicalDataSwitchDefault()
        verifyMarketingDataSwitchDefault()
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!
        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun goBackButton() =
    onView(withContentDescription("Navigate up"))

private fun assertNavigationToolBarHeader() = onView(
    allOf(withParent(withId(R.id.navigationToolbar)),
        withText("Data collection")))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertDataCollectionOptions() {

    onView(withText("Usage and technical data"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    val usageAndTechnicalDataText =
        "Shares performance, usage, hardware and customisation data about your browser with Mozilla to help us make Firefox Preview better"

    onView(withText(usageAndTechnicalDataText))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Marketing data"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    val marketingDataText =
        "Shares data about what features you use in Firefox Preview with Leanplum, our mobile marketing vendor."

    onView(withText(marketingDataText))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun usageAndTechnicalDataButton() = onView(withText("Usage and technical data"))

private fun assertUsageAndTechnicalDataSwitchDefault() = usageAndTechnicalDataButton()
    .assertIsEnabled(isEnabled = true)

private fun marketingDataButton() = onView(withText("Marketing data"))

private fun assertMarketingDataValueSwitchDefault() = marketingDataButton()
    .assertIsEnabled(isEnabled = true)
