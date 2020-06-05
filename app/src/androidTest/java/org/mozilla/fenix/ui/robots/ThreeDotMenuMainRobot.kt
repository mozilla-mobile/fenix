/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.share.ShareFragment

/**
 * Implementation of Robot Pattern for the three dot (main) menu.
 */
class ThreeDotMenuMainRobot {
    fun verifySettingsButton() = assertSettingsButton()
    fun verifyHistoryButton() = assertHistoryButton()
    fun verifyBookmarksButton() = assertBookmarksButton()
    fun verifyHelpButton() = assertHelpButton()
    fun verifyThreeDotMenuExists() = threeDotMenuRecyclerViewExists()
    fun verifyForwardButton() = assertForwardButton()
    fun verifyAddBookmarkButton() = assertAddBookmarkButton()
    fun verifyEditBookmarkButton() = assertEditBookmarkButton()
    fun verifyRefreshButton() = assertRefreshButton()
    fun verifyCloseAllTabsButton() = assertCloseAllTabsButton()
    fun verifyShareButton() = assertShareButton()
    fun verifyReaderViewToggle(visible: Boolean) = assertReaderViewToggle(visible)
    fun verifyReaderViewAppearance(visible: Boolean) = assertReaderViewAppearanceButton(visible)
    fun clickShareButton() {
        shareButton().click()
        mDevice.waitNotNull(Until.findObject(By.text("ALL ACTIONS")), waitingTime)
    }

    fun verifyShareTabButton() = assertShareTabButton()
    fun verifySaveCollection() = assertSaveCollectionButton()

    fun clickBrowserViewSaveCollectionButton() {
        browserViewSaveCollectionButton().click()
    }

    fun clickAddNewCollection() {
        addNewCollectionButton().click()
    }

    fun clickAddBookmarkButton() {
        mDevice.waitNotNull(
            Until.findObject(By.desc("Bookmark")),
            waitingTime
        )
        addBookmarkButton().perform(
            click(
                /* no-op rollback action for when clicks randomly perform a long click, Espresso should attempt to click again
                https://issuetracker.google.com/issues/37078920#comment9
                */
                object : ViewAction {
                    override fun getDescription(): String {
                        return "Handle tap->longclick."
                    }

                    override fun getConstraints(): Matcher<View> {
                        return isAssignableFrom(View::class.java)
                    }

                    override fun perform(uiController: UiController?, view: View?) {
                        // do nothing
                    }
                }
            )
        )
    }

    fun verifyCollectionNameTextField() = assertCollectionNameTextField()
    fun verifyFindInPageButton() = assertFindInPageButton()
    fun verifyShareScrim() = assertShareScrim()
    fun verifySendToDeviceTitle() = assertSendToDeviceTitle()
    fun verifyShareALinkTitle() = assertShareALinkTitle()
    fun verifyWhatsNewButton() = assertWhatsNewButton()
    fun verifyAddFirefoxHome() = assertAddToFirefoxHome()

    class Transition {

        private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun openSettings(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            onView(allOf(withResourceName("text"), withText(R.string.browser_menu_settings)))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
                .check(matches(isCompletelyDisplayed()))
                .perform(ViewActions.click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun openBookmarks(interact: BookmarksRobot.() -> Unit): BookmarksRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text("Bookmarks")), waitingTime)
            bookmarksButton().click()

            BookmarksRobot().interact()
            return BookmarksRobot.Transition()
        }

        fun openHistory(interact: HistoryRobot.() -> Unit): HistoryRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text("History")), waitingTime)
            historyButton().click()

            HistoryRobot().interact()
            return HistoryRobot.Transition()
        }

        fun openHelp(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text("Help")), waitingTime)
            helpButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun goForward(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.desc("Forward")), waitingTime)
            forwardButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun goBack(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            // Close three dot
            mDevice.pressBack()
            // Nav back to previous page
            mDevice.pressBack()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun close(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            // Close three dot
            mDevice.pressBack()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun closeBrowserMenuToBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            // Close three dot
            mDevice.pressBack()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun refreshPage(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.desc("Refresh")), waitingTime)
            refreshButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun closeAllTabs(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
//            mDevice.waitNotNull(Until.findObject(By.text("Close all tabs")), waitingTime)
            closeAllTabsButton().click()

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun openFindInPage(interact: FindInPageRobot.() -> Unit): FindInPageRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text("Find in page")), waitingTime)
            findInPageButton().click()

            FindInPageRobot().interact()
            return FindInPageRobot.Transition()
        }

        fun openWhatsNew(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text("What’s New")), waitingTime)
            whatsNewButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun typeCollectionName(
            name: String,
            interact: BrowserRobot.() -> Unit
        ): BrowserRobot.Transition {
            mDevice.wait(
                Until.findObject(By.res("org.mozilla.fenix.debug:id/name_collection_edittext")),
                waitingTime
            )

            collectionNameTextField().perform(
                ViewActions.replaceText(name),
                ViewActions.pressImeActionButton()
            )
            // wait for the collection creation wrapper to be dismissed
            mDevice.waitNotNull(Until.gone(By.res("org.mozilla.fenix.debug:id/createCollectionWrapper")))

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun toggleReaderView(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            readerViewToggle().click()

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }

        fun openReaderViewAppearance(interact: ReaderViewRobot.() -> Unit): ReaderViewRobot.Transition {
            readerViewAppearanceToggle().click()

            ReaderViewRobot().interact()
            return ReaderViewRobot.Transition()
        }

        fun addToFirefoxHome(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            addToFirefoxHomeButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openAddToHomeScreen(interact: AddToHomeScreenRobot.() -> Unit): AddToHomeScreenRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text("Add to Home screen")), waitingTime)
            addToHomeScreenButton().click()

            AddToHomeScreenRobot().interact()
            return AddToHomeScreenRobot.Transition()
        }

        fun selectExistingCollection(title: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.text(title)), waitingTime)
            onView(withText(title)).click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickOpenTabsMenuSaveCollection(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            saveCollectionButton().click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }
    }
}

private fun threeDotMenuRecyclerViewExists() {
    onView(withId(R.id.mozac_browser_menu_recyclerView)).check(matches(isDisplayed()))
}

private fun settingsButton() = onView(allOf(withResourceName("text"), withText(R.string.browser_menu_settings)))
private fun assertSettingsButton() = settingsButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    .check(matches(isCompletelyDisplayed()))

private fun historyButton() = onView(allOf(withText(R.string.library_history)))
private fun assertHistoryButton() = historyButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun bookmarksButton() = onView(allOf(withText(R.string.library_bookmarks)))
private fun assertBookmarksButton() = bookmarksButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun helpButton() = onView(allOf(withText(R.string.browser_menu_help)))
private fun assertHelpButton() = helpButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun forwardButton() = onView(ViewMatchers.withContentDescription("Forward"))
private fun assertForwardButton() = forwardButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun addBookmarkButton() = onView(ViewMatchers.withContentDescription("Bookmark"))
private fun assertAddBookmarkButton() = addBookmarkButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun editBookmarkButton() = onView(ViewMatchers.withContentDescription("Edit bookmark"))
private fun assertEditBookmarkButton() = editBookmarkButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun refreshButton() = onView(ViewMatchers.withContentDescription("Refresh"))
private fun assertRefreshButton() = refreshButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun closeAllTabsButton() = onView(allOf(withText("Close all tabs"))).inRoot(RootMatchers.isPlatformPopup())
private fun assertCloseAllTabsButton() = closeAllTabsButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun shareTabButton() = onView(allOf(withText("Share all tabs"))).inRoot(RootMatchers.isPlatformPopup())
private fun assertShareTabButton() = shareTabButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun shareButton() = onView(ViewMatchers.withContentDescription("Share"))
private fun assertShareButton() = shareButton()
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun browserViewSaveCollectionButton() = onView(
    allOf(
        withText("Save to collection"),
        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
    )
)

private fun saveCollectionButton() = onView(allOf(withText("Save to collection"))).inRoot(RootMatchers.isPlatformPopup())
private fun assertSaveCollectionButton() = saveCollectionButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun addNewCollectionButton() = onView(allOf(withText("Add new collection")))
private fun assertaddNewCollectionButton() = addNewCollectionButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun collectionNameTextField() = onView(allOf(withResourceName("name_collection_edittext")))
private fun assertCollectionNameTextField() = collectionNameTextField()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun findInPageButton() = onView(allOf(withText("Find in page")))
private fun assertFindInPageButton() = findInPageButton()

private fun shareScrim() = onView(withResourceName("closeSharingScrim"))
private fun assertShareScrim() =
    shareScrim().check(matches(ViewMatchers.withAlpha(ShareFragment.SHOW_PAGE_ALPHA)))

private fun SendToDeviceTitle() =
    onView(allOf(withText("SEND TO DEVICE"), withResourceName("accountHeaderText")))

private fun assertSendToDeviceTitle() = SendToDeviceTitle()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun ShareALinkTitle() =
    onView(allOf(withText("ALL ACTIONS"), withResourceName("apps_link_header")))

private fun assertShareALinkTitle() = ShareALinkTitle()

private fun whatsNewButton() = onView(
    allOf(
        withText("What’s New"),
        withEffectiveVisibility(Visibility.VISIBLE)
    )
)

private fun assertWhatsNewButton() = whatsNewButton()
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun addToHomeScreenButton() = onView(withText("Add to Home screen"))

private fun readerViewToggle() = onView(allOf(withText(R.string.browser_menu_read)))
private fun assertReaderViewToggle(visible: Boolean) = readerViewToggle()
    .check(
        if (visible) matches(withEffectiveVisibility(Visibility.VISIBLE)) else ViewAssertions.doesNotExist()
    )

private fun readerViewAppearanceToggle() =
    onView(allOf(withText(R.string.browser_menu_read_appearance)))

private fun assertReaderViewAppearanceButton(visible: Boolean) = readerViewAppearanceToggle()
    .check(
        if (visible) matches(withEffectiveVisibility(Visibility.VISIBLE)) else ViewAssertions.doesNotExist()
    )

private fun addToFirefoxHomeButton() =
    onView(allOf(withText(R.string.browser_menu_add_to_top_sites)))
private fun assertAddToFirefoxHome() {
    onView(withId(R.id.mozac_browser_menu_recyclerView))
        .perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                hasDescendant(withText(R.string.browser_menu_add_to_top_sites))
            )
        ).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}
