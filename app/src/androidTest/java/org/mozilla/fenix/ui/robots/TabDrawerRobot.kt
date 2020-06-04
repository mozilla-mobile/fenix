/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the home screen menu.
 */
class TabDrawerRobot {
    fun verifyExistingTabList() = assertExistingTabList()
    fun verifyNoTabsOpenedText() = assertNoTabsOpenedText()
    fun verifyPrivateModeSelected() = assertPrivateModeSelected()
    fun verifyNormalModeSelected() = assertNormalModeSelected()

    fun closeTab() {
        closeTabButton().click()
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun openThreeDotMenu(interact: ThreeDotMenuMainRobot.() -> Unit): ThreeDotMenuMainRobot.Transition {
            mDevice.waitForIdle()

            Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext<Context>())
            ThreeDotMenuMainRobot().interact()
            return ThreeDotMenuMainRobot.Transition()
        }

        fun openHomeScreen(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.waitForIdle()

            newTabButton().perform(click())
            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun togglePrivateBrowsingMode(interact: TabDrawerRobot.() -> Unit): Transition {
            mDevice.waitForIdle()
            privateBrowsingButton().perform(click())
            TabDrawerRobot().interact()
            return Transition()
        }

        fun newTabAndEnterURLAndEnterToBrowser(url: Uri, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitForIdle()
            newTabButton().perform(click())

            return searchScreen {
                typeSearch(url.toString())
            }.openBrowser(interact)
        }
    }
}

fun tabDrawer(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
    TabDrawerRobot().interact()
    return TabDrawerRobot.Transition()
}

private fun closeTabButton() = onView(withId(R.id.close_tab_button))
private fun normalBrowsingButton() = onView(withId(R.id.default_tab_item))
private fun privateBrowsingButton() = onView(withId(R.id.private_tab_item))
private fun newTabButton() = onView(withId(R.id.new_tab_button))

private fun assertExistingTabList() =
    onView(allOf(withId(R.id.tab_item)))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertNoTabsOpenedText() =
    onView(withText("Your open tabs will be shown here."))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertNormalModeSelected() =
    onView(withContentDescription("Open tabs"))
        .check(matches(ViewMatchers.isSelected()))

private fun assertPrivateModeSelected() =
    onView(withContentDescription("Private tabs"))
        .check(matches(ViewMatchers.isSelected()))