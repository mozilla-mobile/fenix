/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParentIndex
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.isChecked

/**
 * Implementation of Robot Pattern for the settings Delete Browsing Data On Quit sub menu.
 */
class SettingsSubMenuDeleteBrowsingDataOnQuitRobot {

    fun verifyNavigationToolBarHeader() = assertNavigationToolBarHeader()

    fun verifyDeleteBrowsingOnQuitButton() = assertDeleteBrowsingOnQuitButton()

    fun verifyDeleteBrowsingOnQuitButtonSummary() = assertDeleteBrowsingOnQuitButtonSummary()

    fun verifyDeleteBrowsingOnQuitButtonSwitchDefault() = assertDeleteBrowsingOnQuitButtonSwitchDefault()

    fun clickDeleteBrowsingOnQuitButtonSwitchDefaultChange() = verifyDeleteBrowsingOnQuitButtonSwitchDefault().click()

    fun verifyAllTheCheckBoxesText() = assertAllTheCheckBoxesText()

    fun verifyAllTheCheckBoxesChecked() = assertAllTheCheckBoxesChecked()

    fun verifyDeleteBrowsingDataOnQuitSubMenuItems() {
        verifyDeleteBrowsingOnQuitButton()
        verifyDeleteBrowsingOnQuitButtonSummary()
        verifyDeleteBrowsingOnQuitButtonSwitchDefault()
        clickDeleteBrowsingOnQuitButtonSwitchDefaultChange()
        verifyAllTheCheckBoxesText()
        verifyAllTheCheckBoxesChecked()
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!
        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun goBackButton() = onView(withContentDescription("Navigate up"))

private fun assertNavigationToolBarHeader() = onView(allOf(withId(R.id.navigationToolbar),
    withChild(withText(R.string.preferences_delete_browsing_data_on_quit))))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun deleteBrowsingOnQuitButton() = onView(allOf(withParentIndex(0),
    withChild(withText(R.string.preferences_delete_browsing_data_on_quit))))

private fun assertDeleteBrowsingOnQuitButton() = deleteBrowsingOnQuitButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertDeleteBrowsingOnQuitButtonSummary() = onView(
    withText(R.string.preference_summary_delete_browsing_data_on_quit_2))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertDeleteBrowsingOnQuitButtonSwitchDefault() = onView(withResourceName("switch_widget"))
    .check(matches(isChecked(false)))

private fun assertAllTheCheckBoxesText() {
    onView(withText(R.string.preferences_delete_browsing_data_tabs_title_2))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText(R.string.preferences_delete_browsing_data_on_quit_browsing_history))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText(R.string.preferences_delete_browsing_data_cookies))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText(R.string.preferences_delete_browsing_data_cookies_subtitle))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText(R.string.preferences_delete_browsing_data_cached_files))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText(R.string.preferences_delete_browsing_data_cached_files_subtitle))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText(R.string.preferences_delete_browsing_data_site_permissions))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertAllTheCheckBoxesChecked() {
    // Only verifying the options, checkboxes default value can't be verified due to issue #9471
}
