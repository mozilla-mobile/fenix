/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for the your library menu.
 */
class LibraryRobot {
    fun verifyLibraryView() = assertLibraryView()
    fun verifyBookmarksButton() = assertBookmarksButton()
    fun verifyHistoryButton() = assertHistoryButton()

    class Transition {

        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {

            mDevice.waitForIdle()
            goBackButton().perform(click())

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun closeMenu(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            closeButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openBookmarks(interact: BookmarksRobot.() -> Unit): BookmarksRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text("Bookmarks")), TestAssetHelper.waitingTime)
            bookmarksButton().click()

            BookmarksRobot().interact()
            return BookmarksRobot.Transition()
        }

        fun openHistory(interact: HistoryRobot.() -> Unit): HistoryRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text("History")), TestAssetHelper.waitingTime)
            historyButton().click()

            HistoryRobot().interact()
            return HistoryRobot.Transition()
        }
    }
}

private fun goBackButton() = onView(allOf(withContentDescription("Navigate up")))
private fun closeButton() = onView(withId(R.id.libraryClose))
private fun bookmarksButton() = onView(allOf(withText("Bookmarks")))
private fun historyButton() = onView(allOf(withText("History")))

private fun assertLibraryView() {
    onView(
        allOf(
            withText("Library"),
            ViewMatchers.withParent(withId(R.id.navigationToolbar))
        )
    )
        .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertBookmarksButton() = bookmarksButton().check(
    ViewAssertions.matches(
        ViewMatchers.withEffectiveVisibility(
            ViewMatchers.Visibility.VISIBLE
        )
    )
)

private fun assertHistoryButton() = historyButton().check(
    ViewAssertions.matches(
        ViewMatchers.withEffectiveVisibility(
            ViewMatchers.Visibility.VISIBLE
        )
    )
)
