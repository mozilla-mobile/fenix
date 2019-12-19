/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the home screen menu.
 */
class TabScreenRobot {
    fun verifyExistingTabList() = assertExistingTabList()
    fun verifyNoTabsOpenedText() = assertNoTabsOpenedText()

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

        fun togglePrivateBrowsingMode() {
            mDevice.waitForIdle()

            privateBrowsingButton().perform(click())
        }
    }
}

fun tabScreen(interact: TabScreenRobot.() -> Unit): TabScreenRobot.Transition {
    TabScreenRobot().interact()
    return TabScreenRobot.Transition()
}

private fun threeDotButton() = onView(Matchers.allOf(ViewMatchers.withContentDescription("More options")))
private fun closeTabButton() = onView(withId(R.id.accessory_view))
private fun privateBrowsingButton() = onView(withId(R.id.private_browsing_button))

private fun assertExistingTabList() =
    onView(withId(R.id.tab_list_item))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertNoTabsOpenedText() =
    onView(withText("Your open tabs will be shown here."))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
