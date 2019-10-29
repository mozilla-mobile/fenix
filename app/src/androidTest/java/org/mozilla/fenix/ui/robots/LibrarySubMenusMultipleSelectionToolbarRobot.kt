/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull

/*
 * Implementation of Robot Pattern for the multiple selection toolbar of History and Bookmarks menus.
 */
class LibrarySubMenusMultipleSelectionToolbarRobot {

    fun verifyMultiSelectionCheckmark() = assertMultiSelectionCheckmark()

    fun verifyMultiSelectionCounter() = assertMultiSelectionCounter()

    fun verifyShareButton() = assertShareButton()

    fun verifyShareOverlay() = assertShareOverlay()

    fun verifyShareTabFavicon() = assertShareTabFavicon()

    fun verifyShareTabTitle() = assertShareTabTitle()

    fun verifyShareTabUrl() = assertShareTabUrl()

    fun verifyCloseToolbarButton() = assertCloseToolbarButton()

    fun clickShareButton() {
        shareButton().click()

        mDevice.waitNotNull(
            Until.findObject(
                By.text("SHARE A LINK")
            ), waitingTime
        )
    }

    class Transition {
        fun closeToolbarReturnToHistory(interact: HistoryRobot.() -> Unit): HistoryRobot.Transition {
            closeToolbarButton().click()

            HistoryRobot().interact()
            return HistoryRobot.Transition()
        }

        fun closeToolbarReturnToBookmarks(interact: BookmarksRobot.() -> Unit): BookmarksRobot.Transition {
            closeToolbarButton().click()

            BookmarksRobot().interact()
            return BookmarksRobot.Transition()
        }

        fun clickOpenNewTab(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            openInNewTabButton().click()
            mDevice.waitNotNull(Until.findObject(By.text("Open tabs")), waitingTime)

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickOpenPrivateTab(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            openInPrivateTabButton().click()
            mDevice.waitNotNull(
                Until.findObject(By.text("Private session")),
                waitingTime
            )

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickMultiSelectionDelete(interact: HistoryRobot.() -> Unit): HistoryRobot.Transition {
            multiSelectionDeleteButton().click()

            HistoryRobot().interact()
            return HistoryRobot.Transition()
        }
    }
}

fun multipleSelectionToolbar(interact: LibrarySubMenusMultipleSelectionToolbarRobot.() -> Unit): LibrarySubMenusMultipleSelectionToolbarRobot.Transition {

    LibrarySubMenusMultipleSelectionToolbarRobot().interact()
    return LibrarySubMenusMultipleSelectionToolbarRobot.Transition()
}

private fun closeToolbarButton() = onView(withContentDescription("Navigate up"))

private fun shareButton() = onView(withId(R.id.share_history_multi_select))

private fun openInNewTabButton() = onView(withText("Open in new tab"))

private fun openInPrivateTabButton() = onView(withText("Open in private tab"))

private fun multiSelectionDeleteButton() = onView(withText("Delete"))

private fun assertMultiSelectionCheckmark() =
    onView(withId(R.id.checkmark))
        .check(matches(isDisplayed()))

private fun assertMultiSelectionCounter() =
    onView(withText("1 selected")).check(matches(isDisplayed()))

private fun assertShareButton() =
    shareButton().check(matches(isDisplayed()))

private fun assertShareOverlay() =
    onView(withId(R.id.shareWrapper)).check(matches(isDisplayed()))

private fun assertShareTabTitle() =
    onView(withId(R.id.share_tab_title)).check(matches(isDisplayed()))

private fun assertShareTabFavicon() =
    onView(withId(R.id.share_tab_favicon)).check(matches(isDisplayed()))

private fun assertShareTabUrl() = onView(withId(R.id.share_tab_url))

private fun assertCloseToolbarButton() = closeToolbarButton().check(matches(isDisplayed()))
