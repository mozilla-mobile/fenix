/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.hamcrest.Matchers.allOf

/*
 * Implementation of Robot Pattern for the multiple selection toolbar of History and Bookmarks menus.
 */
class LibrarySubMenusMultipleSelectionToolbarRobot {

    fun verifyMultiSelectionCheckmark() = assertMultiSelectionCheckmark()

    fun verifyMultiSelectionCheckmark(url: Uri) = assertMultiSelectionCheckmark(url)

    fun verifyMultiSelectionCounter() = assertMultiSelectionCounter()

    fun verifyShareHistoryButton() = assertShareHistoryButton()

    fun verifyShareBookmarksButton() = assertShareBookmarksButton()

    fun verifyShareOverlay() = assertShareOverlay()

    fun verifyShareTabFavicon() = assertShareTabFavicon()

    fun verifyShareTabTitle() = assertShareTabTitle()

    fun verifyShareTabUrl() = assertShareTabUrl()

    fun verifyCloseToolbarButton() = assertCloseToolbarButton()

    fun clickShareHistoryButton() {
        shareHistoryButton().click()

        mDevice.waitNotNull(
            Until.findObject(
                By.text("ALL ACTIONS")
            ), waitingTime
        )
    }

    fun clickShareBookmarksButton() {
        shareBookmarksButton().click()

        mDevice.waitNotNull(
            Until.findObject(
                By.text("ALL ACTIONS")
            ), waitingTime
        )
    }

    fun clickMultiSelectionDelete() {
        deleteButton().click()
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
            mDevice.waitNotNull(Until.findObject(By.text("Collections")), waitingTime)

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun clickOpenPrivateTab(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            openInPrivateTabButton().click()
            mDevice.waitNotNull(
                Until.findObject(By.text(PRIVATE_SESSION_MESSAGE)),
                waitingTime
            )

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }
    }
}

fun multipleSelectionToolbar(interact: LibrarySubMenusMultipleSelectionToolbarRobot.() -> Unit): LibrarySubMenusMultipleSelectionToolbarRobot.Transition {

    LibrarySubMenusMultipleSelectionToolbarRobot().interact()
    return LibrarySubMenusMultipleSelectionToolbarRobot.Transition()
}

private fun closeToolbarButton() = onView(withContentDescription("Navigate up"))

private fun shareHistoryButton() = onView(withId(R.id.share_history_multi_select))

private fun shareBookmarksButton() = onView(withId(R.id.share_bookmark_multi_select))

private fun openInNewTabButton() = onView(withText("Open in new tab"))

private fun openInPrivateTabButton() = onView(withText("Open in private tab"))

private fun deleteButton() = onView(withText("Delete"))

private fun assertMultiSelectionCheckmark() =
    onView(withId(R.id.checkmark))
        .check(matches(isDisplayed()))

private fun assertMultiSelectionCheckmark(url: Uri) =
    onView(
        allOf(
            withId(R.id.checkmark),
            withParent(withParent(withChild(allOf(withId(R.id.url), withText(url.toString()))))),

            // This is used as part of the `multiSelectionToolbarItemsTest` test. Somehow, in the view hierarchy,
            // the match above is finding two checkmark views - one visible, one hidden, which is throwing off
            // the matcher. This 'isDisplayed' check is a hacky workaround for this, we're explicitly ignoring
            // the hidden one. Why are there two to begin with, though?
            isDisplayed()
        )
    )
        .check(matches(isDisplayed()))

private fun assertMultiSelectionCounter() =
    onView(withText("1 selected")).check(matches(isDisplayed()))

private fun assertShareHistoryButton() =
    shareHistoryButton().check(matches(isDisplayed()))

private fun assertShareBookmarksButton() =
    shareBookmarksButton().check(matches(isDisplayed()))

private fun assertShareOverlay() =
    onView(withId(R.id.shareWrapper)).check(matches(isDisplayed()))

private fun assertShareTabTitle() =
    onView(withId(R.id.share_tab_title)).check(matches(isDisplayed()))

private fun assertShareTabFavicon() =
    onView(withId(R.id.share_tab_favicon)).check(matches(isDisplayed()))

private fun assertShareTabUrl() = onView(withId(R.id.share_tab_url))

private fun assertCloseToolbarButton() = closeToolbarButton().check(matches(isDisplayed()))
