/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.graphics.Bitmap
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.Until.findObject
import mozilla.components.browser.state.state.searchEngines
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.helpers.TestHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.TestHelper.scrollToElementByText
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.helpers.matchers.hasItem
import org.mozilla.fenix.helpers.withBitmapDrawable
import org.mozilla.fenix.ui.util.STRING_ONBOARDING_ACCOUNT_SIGN_IN_HEADER
import org.mozilla.fenix.ui.util.STRING_ONBOARDING_TOOLBAR_PLACEMENT_HEADER
import org.mozilla.fenix.ui.util.STRING_ONBOARDING_TRACKING_PROTECTION_HEADER

/**
 * Implementation of Robot Pattern for the home screen menu.
 */
class HomeScreenRobot {
    val privateSessionMessage =
        "$appName clears your search and browsing history from private tabs when you close them" +
            " or quit the app. While this doesn’t make you anonymous to websites or your internet" +
            " service provider, it makes it easier to keep what you do online private from anyone" +
            " else who uses this device."

    fun verifyNavigationToolbar() = assertNavigationToolbar()
    fun verifyFocusedNavigationToolbar() = assertFocusedNavigationToolbar()
    fun verifyHomeScreen() = assertHomeScreen()
    fun verifyHomePrivateBrowsingButton() = assertHomePrivateBrowsingButton()
    fun verifyHomeMenu() = assertHomeMenu()
    fun verifyTabButton() = assertTabButton()
    fun verifyCollectionsHeader() = assertCollectionsHeader()
    fun verifyNoCollectionsText() = assertNoCollectionsText()
    fun verifyHomeWordmark() = assertHomeWordmark()
    fun verifyHomeToolbar() = assertHomeToolbar()
    fun verifyHomeComponent() = assertHomeComponent()
    fun verifyDefaultSearchEngine(searchEngine: String) = verifySearchEngineIcon(searchEngine)
    fun verifyNoTabsOpened() = assertNoTabsOpened()
    fun verifyKeyboardVisible() = assertKeyboardVisibility(isExpectedToBeVisible = true)

    fun verifyWallpaperImageApplied(isEnabled: Boolean) {
        if (isEnabled) {
            assertTrue(
                mDevice.findObject(
                    UiSelector().resourceId("$packageName:id/wallpaperImageView")
                ).waitForExists(waitingTimeShort)
            )
        } else {
            assertFalse(
                mDevice.findObject(
                    UiSelector().resourceId("$packageName:id/wallpaperImageView")
                ).waitForExists(waitingTimeShort)
            )
        }

        mDevice.findObject(UiSelector())
    }

    // First Run elements
    fun verifyWelcomeHeader() = assertWelcomeHeader()

    fun verifyStartSyncHeader() = assertStartSyncHeader()
    fun verifyAccountsSignInButton() = assertAccountsSignInButton()
    fun verifyChooseThemeHeader() = assertChooseThemeHeader()
    fun verifyChooseThemeText() = assertChooseThemeText()
    fun verifyLightThemeToggle() = assertLightThemeToggle()
    fun verifyLightThemeDescription() = assertLightThemeDescription()
    fun verifyDarkThemeToggle() = assertDarkThemeToggle()
    fun verifyDarkThemeDescription() = assertDarkThemeDescription()
    fun verifyAutomaticThemeToggle() = assertAutomaticThemeToggle()
    fun verifyAutomaticThemeDescription() = assertAutomaticThemeDescription()
    fun verifyAutomaticPrivacyHeader() = assertAutomaticPrivacyHeader()
    fun verifyAutomaticPrivacyText() = assertAlwaysPrivacyText()

    // Pick your toolbar placement
    fun verifyTakePositionHeader() = assertTakePlacementHeader()
    fun verifyTakePositionElements() {
        assertTakePlacementBottomRadioButton()
        assertTakePacementTopRadioButton()
    }

    // Your privacy
    fun verifyYourPrivacyHeader() = assertYourPrivacyHeader()
    fun verifyYourPrivacyText() = assertYourPrivacyText()
    fun verifyPrivacyNoticeButton() = assertPrivacyNoticeButton()
    fun verifyStartBrowsingButton() = assertStartBrowsingButton()

    fun verifyPrivateSessionMessage() = assertPrivateSessionMessage()

    fun verifyExistingTopSitesList() = assertExistingTopSitesList()
    fun verifyNotExistingTopSitesList(title: String) = assertNotExistingTopSitesList(title)
    fun verifyExistingTopSitesTabs(title: String) = assertExistingTopSitesTabs(title)
    fun verifyTopSiteContextMenuItems() = assertTopSiteContextMenuItems()

    fun verifyJumpBackInSectionIsDisplayed() = assertJumpBackInSectionIsDisplayed()
    fun verifyJumpBackInSectionIsNotDisplayed() = assertJumpBackInSectionIsNotDisplayed()
    fun verifyRecentBookmarksSectionIsDisplayed() = assertRecentBookmarksSectionIsDisplayed()
    fun verifyRecentBookmarksSectionIsNotDisplayed() = assertRecentBookmarksSectionIsNotDisplayed()

    fun verifyRecentlyVisitedSearchGroupDisplayed(shouldBeDisplayed: Boolean, searchTerm: String, groupSize: Int) {
        // checks if the search group exists in the Recently visited section
        if (shouldBeDisplayed) {
            recentlyVisitedList.waitForExists(waitingTime)
            scrollToElementByText("Recently visited")
            recentlyVisitedList.getChildByText(UiSelector().text(searchTerm), searchTerm, true)
                .waitForExists(waitingTimeShort)
            assertTrue(
                mDevice.findObject(UiSelector().text(searchTerm))
                    .getFromParent(UiSelector().text("$groupSize sites"))
                    .waitForExists(waitingTimeShort)
            )
        } else {
            assertFalse(
                mDevice.findObject(UiSelector().text(searchTerm))
                    .getFromParent(UiSelector().text("$groupSize sites"))
                    .waitForExists(waitingTimeShort)
            )
        }
    }

    fun verifyCurrentSearchGroupIsDisplayed(shouldBeDisplayed: Boolean, searchTerm: String, groupSize: Int = 0) {
        // checks search group in the Jump back in section
        if (shouldBeDisplayed) {
            assertTrue(
                mDevice.findObject(UiSelector().text("""Your search for "$searchTerm""""))
                    .getFromParent(UiSelector().textContains("$groupSize sites"))
                    .waitForExists(waitingTimeShort)
            )
        } else {
            assertFalse(
                mDevice.findObject(UiSelector().text("""Your search for "$searchTerm""""))
                    .waitForExists(waitingTimeShort)
            )
        }
    }

    // Collections elements
    fun verifyCollectionIsDisplayed(title: String, collectionExists: Boolean = true) {
        if (collectionExists) {
            scrollToElementByText(title)
            assertTrue(mDevice.findObject(UiSelector().text(title)).waitForExists(waitingTime))
        } else {
            scrollToElementByText("Collections")
            assertTrue(mDevice.findObject(UiSelector().text(title)).waitUntilGone(waitingTime))
        }
    }

    fun verifyCollectionIcon() = onView(withId(R.id.collection_icon)).check(matches(isDisplayed()))

    fun togglePrivateBrowsingModeOnOff() {
        onView(ViewMatchers.withResourceName("privateBrowsingButton"))
            .perform(click())
    }

    fun swipeToBottom() = onView(withId(R.id.homeLayout)).perform(ViewActions.swipeUp())

    fun swipeToTop() =
        onView(withId(R.id.sessionControlRecyclerView)).perform(ViewActions.swipeDown())

    fun verifySnackBarText(expectedText: String) {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mDevice.waitNotNull(findObject(By.text(expectedText)), waitingTime)
    }

    fun clickUndoCollectionDeletion(expectedText: String) {
        onView(
            allOf(
                withId(R.id.snackbar_btn),
                withText(expectedText)
            )
        ).click()
    }

    fun clickFirefoxLogo() = homepageWordmark.click()

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun openTabDrawer(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            mDevice.findObject(
                UiSelector().descriptionContains("open tab. Tap to switch tabs.")
            ).waitForExists(waitingTime)

            tabsCounter().click()
            mDevice.waitNotNull(Until.findObject(By.res("$packageName:id/tab_layout")))

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun openThreeDotMenu(interact: ThreeDotMenuMainRobot.() -> Unit): ThreeDotMenuMainRobot.Transition {
            // Issue: https://github.com/mozilla-mobile/fenix/issues/21578
            try {
                mDevice.waitNotNull(
                    Until.findObject(By.res("$packageName:id/menuButton")),
                    waitingTime
                )
            } catch (e: AssertionError) {
                mDevice.pressBack()
            } finally {
                threeDotButton().perform(click())
            }

            ThreeDotMenuMainRobot().interact()
            return ThreeDotMenuMainRobot.Transition()
        }

        fun openSearch(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
            navigationToolbar().waitForExists(waitingTime)
            navigationToolbar().click()

            SearchRobot().interact()
            return SearchRobot.Transition()
        }

        fun dismissOnboarding() {
            openThreeDotMenu { }.openSettings { }.goBack { }
        }

        fun clickStartBrowsingButton(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
            startBrowsingButton().click()

            SearchRobot().interact()
            return SearchRobot.Transition()
        }

        fun togglePrivateBrowsingMode() {
            mDevice.findObject(UiSelector().resourceId("$packageName:id/privateBrowsingButton"))
                .waitForExists(
                    waitingTime
                )
            privateBrowsingButton()
                .perform(click())
        }

        fun triggerPrivateBrowsingShortcutPrompt(interact: AddToHomeScreenRobot.() -> Unit): AddToHomeScreenRobot.Transition {
            // Loop to press the PB icon for 5 times to display the Add the Private Browsing Shortcut CFR
            for (i in 1..5) {
                mDevice.findObject(UiSelector().resourceId("$packageName:id/privateBrowsingButton"))
                    .waitForExists(
                        waitingTime
                    )

                privateBrowsingButton()
                    .perform(click())
            }

            AddToHomeScreenRobot().interact()
            return AddToHomeScreenRobot.Transition()
        }

        fun pressBack() {
            onView(ViewMatchers.isRoot()).perform(ViewActions.pressBack())
        }

        fun openNavigationToolbar(interact: NavigationToolbarRobot.() -> Unit): NavigationToolbarRobot.Transition {
            mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
                .waitForExists(waitingTime)
            navigationToolbar().click()

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }

        fun openContextMenuOnTopSitesWithTitle(
            title: String,
            interact: HomeScreenRobot.() -> Unit
        ): Transition {
            onView(withId(R.id.top_sites_list)).perform(
                actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(title)),
                    ViewActions.longClick()
                )
            )

            HomeScreenRobot().interact()
            return Transition()
        }

        fun openTopSiteTabWithTitle(
            title: String,
            interact: BrowserRobot.() -> Unit
        ): BrowserRobot.Transition {
            onView(withId(R.id.top_sites_list)).perform(
                actionOnItem<RecyclerView.ViewHolder>(hasDescendant(withText(title)), click())
            )

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun renameTopSite(title: String, interact: HomeScreenRobot.() -> Unit): Transition {
            onView(withText("Rename"))
                .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))
                .perform(click())
            onView(Matchers.allOf(withId(R.id.top_site_title), instanceOf(EditText::class.java)))
                .perform(ViewActions.replaceText(title))
            onView(withId(android.R.id.button1)).perform((click()))

            HomeScreenRobot().interact()
            return Transition()
        }

        fun removeTopSite(interact: HomeScreenRobot.() -> Unit): Transition {
            onView(withText("Remove"))
                .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))
                .perform(click())

            HomeScreenRobot().interact()
            return Transition()
        }

        fun deleteTopSiteFromHistory(interact: HomeScreenRobot.() -> Unit): Transition {
            mDevice.findObject(
                UiSelector().resourceId("$packageName:id/simple_text")
            ).waitForExists(waitingTime)
            deleteFromHistory.click()

            HomeScreenRobot().interact()
            return Transition()
        }

        fun openTopSiteInPrivateTab(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            onView(withText("Open in private tab"))
                .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))
                .perform(click())

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openCommonMythsLink(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.findObject(
                UiSelector()
                    .textContains(
                        getStringResource(R.string.private_browsing_common_myths)
                    )
            ).also { it.click() }

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickSaveTabsToCollectionButton(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            saveTabsToCollectionButton().click()

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        // fun expandCollection(
        //     title: String,
        //     interact: CollectionRobot.() -> Unit
        // ): CollectionRobot.Transition {
        //     // Depending on the screen dimensions collections might report as visible on screen
        //     // but actually have the bottom toolbar above so interactions with collections might fail.
        //     // As a quick solution we'll try scrolling to the element below collection on the homescreen
        //     // so that they are displayed above in their entirety.
        //     scrollToElementByText(appContext.getString(R.string.pocket_stories_header_1))
        //
        //     collectionTitle(title).click()
        //
        //     CollectionRobot().interact()
        //     return CollectionRobot.Transition()
        // }

        fun openRecentlyVisitedSearchGroupHistoryList(title: String, interact: HistoryRobot.() -> Unit): HistoryRobot.Transition {
            val searchGroup = recentlyVisitedList.getChildByText(UiSelector().text(title), title, true)
            searchGroup.waitForExists(waitingTimeShort)
            searchGroup.click()

            HistoryRobot().interact()
            return HistoryRobot.Transition()
        }
    }
}

fun homeScreen(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
    HomeScreenRobot().interact()
    return HomeScreenRobot.Transition()
}

val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

private fun homeScreenList() =
    UiScrollable(
        UiSelector()
            .resourceId("$packageName:id/sessionControlRecyclerView")
            .scrollable(true)
    ).setAsVerticalList()

private fun assertKeyboardVisibility(isExpectedToBeVisible: Boolean) =
    Assert.assertEquals(
        isExpectedToBeVisible,
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .executeShellCommand("dumpsys input_method | grep mInputShown")
            .contains("mInputShown=true")
    )

private fun navigationToolbar() = mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))

private fun assertNavigationToolbar() = assertTrue(navigationToolbar().waitForExists(waitingTime))

private fun assertFocusedNavigationToolbar() =
    onView(allOf(withHint("Search or enter address")))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertHomeScreen() {
    mDevice.findObject(UiSelector().resourceId("$packageName:id/homeLayout")).waitForExists(waitingTime)
    onView(ViewMatchers.withResourceName("homeLayout"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertHomeMenu() = onView(ViewMatchers.withResourceName("menuButton"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertHomePrivateBrowsingButton() =
    privateBrowsingButton()
        .check(matches(isDisplayed()))

private val homepageWordmark = onView(ViewMatchers.withResourceName("wordmark"))
private fun assertHomeWordmark() =
    homepageWordmark.check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertHomeToolbar() = onView(ViewMatchers.withResourceName("toolbar"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertTabButton() =
    onView(allOf(withId(R.id.tab_button), isDisplayed()))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertCollectionsHeader() =
    onView(allOf(withText("Collections")))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertNoCollectionsText() =
    onView(
        withText(
            containsString(
                "Collect the things that matter to you.\n" +
                    "Group together similar searches, sites, and tabs for quick access later."
            )
        )
    ).check(matches(isDisplayed()))

private fun assertHomeComponent() =
    onView(ViewMatchers.withResourceName("sessionControlRecyclerView"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertNoTabsOpened() = onView(withId(R.id.counter_text)).check(matches(withText("0")))

private fun threeDotButton() = onView(allOf(withId(R.id.menuButton)))

private fun verifySearchEngineIcon(searchEngineIcon: Bitmap, searchEngineName: String) {
    onView(withId(R.id.search_engine_icon))
        .check(matches(withBitmapDrawable(searchEngineIcon, searchEngineName)))
}

private fun getSearchEngine(searchEngineName: String) =
    appContext.components.core.store.state.search.searchEngines.find { it.name == searchEngineName }

private fun verifySearchEngineIcon(searchEngineName: String) {
    val ddgSearchEngine = getSearchEngine(searchEngineName)
        ?: throw AssertionError("No search engine with name $searchEngineName")
    verifySearchEngineIcon(ddgSearchEngine.icon, ddgSearchEngine.name)
}

// First Run elements
private fun assertWelcomeHeader() =
    assertTrue(
        mDevice.findObject(
            UiSelector().textContains(
                getStringResource(R.string.onboarding_header)
            )
        ).waitForExists(waitingTime)
    )

private fun assertStartSyncHeader() {
    scrollToElementByText(STRING_ONBOARDING_ACCOUNT_SIGN_IN_HEADER)
    onView(allOf(withText(R.string.onboarding_account_sign_in_header_1)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}
private fun assertAccountsSignInButton() =
    onView(ViewMatchers.withResourceName("fxa_sign_in_button"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertChooseThemeHeader() {
    scrollToElementByText("Choose your theme")
    onView(withText("Choose your theme"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}
private fun assertChooseThemeText() {
    scrollToElementByText("Choose your theme")
    onView(allOf(withText("Save some battery and your eyesight with dark mode.")))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertLightThemeToggle() {
    scrollToElementByText("Choose your theme")
    onView(ViewMatchers.withResourceName("theme_light_radio_button"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertLightThemeDescription() {
    scrollToElementByText("Choose your theme")
    onView(allOf(withText("Light theme")))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertDarkThemeToggle() {
    scrollToElementByText("Choose your theme")
    onView(ViewMatchers.withResourceName("theme_dark_radio_button"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertDarkThemeDescription() {
    scrollToElementByText("Choose your theme")
    onView(allOf(withText("Dark theme")))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}
private fun assertAutomaticThemeToggle() {
    scrollToElementByText("Choose your theme")
    onView(withId(R.id.theme_automatic_radio_button))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertAutomaticThemeDescription() {
    scrollToElementByText("Choose your theme")
    onView(allOf(withText("Automatic")))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertAutomaticPrivacyHeader() {
    scrollToElementByText(STRING_ONBOARDING_TRACKING_PROTECTION_HEADER)
    onView(allOf(withText(STRING_ONBOARDING_TRACKING_PROTECTION_HEADER)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertAlwaysPrivacyText() {
    scrollToElementByText(STRING_ONBOARDING_TRACKING_PROTECTION_HEADER)
    onView(
        allOf(
            withText(
                "$appName automatically stops companies from secretly following you around the web."
            )
        )
    )
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertYourPrivacyHeader() {
    scrollToElementByText("Your privacy")
    onView(allOf(withText("Your privacy")))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertYourPrivacyText() {
    scrollToElementByText("Your privacy")
    onView(
        allOf(
            withText(
                "We’ve designed $appName to give you control over what you share online and what you share with us."
            )
        )
    )
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertPrivacyNoticeButton() {
    scrollToElementByText("Your privacy")
    onView(allOf(withText("Read our privacy notice")))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertStartBrowsingButton() {
    assertTrue(startBrowsingButton().waitForExists(waitingTime))
}

// Pick your toolbar placement
private fun assertTakePlacementHeader() {
    scrollToElementByText(STRING_ONBOARDING_TOOLBAR_PLACEMENT_HEADER)
    onView(allOf(withText(STRING_ONBOARDING_TOOLBAR_PLACEMENT_HEADER)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertTakePacementTopRadioButton() {
    scrollToElementByText(STRING_ONBOARDING_TOOLBAR_PLACEMENT_HEADER)
    onView(ViewMatchers.withResourceName("toolbar_top_radio_button"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertTakePlacementBottomRadioButton() {
    scrollToElementByText(STRING_ONBOARDING_TOOLBAR_PLACEMENT_HEADER)
    onView(ViewMatchers.withResourceName("toolbar_bottom_radio_button"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertPrivateSessionMessage() =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .textContains(
                    getStringResource(R.string.private_browsing_common_myths)
                )
        ).waitForExists(waitingTime)
    )

private fun assertExistingTopSitesList() =
    onView(allOf(withId(R.id.top_sites_list)))
        .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertExistingTopSitesTabs(title: String) {
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/top_site_title")
            .textContains(title)
    ).waitForExists(waitingTime)

    onView(allOf(withId(R.id.top_sites_list)))
        .check(matches(hasDescendant(withText(title))))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertNotExistingTopSitesList(title: String) {
    mDevice.findObject(UiSelector().text(title))
        .waitUntilGone(waitingTime)

    onView(allOf(withId(R.id.top_sites_list)))
        .check(matches(not(hasItem(hasDescendant(withText(title))))))
}

private fun assertTopSiteContextMenuItems() {
    val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    mDevice.waitNotNull(
        findObject(By.text("Open in private tab")),
        waitingTime
    )
    mDevice.waitNotNull(
        findObject(By.text("Remove")),
        waitingTime
    )
}

private fun assertJumpBackInSectionIsDisplayed() = assertTrue(jumpBackInSection().waitForExists(waitingTime))

private fun assertJumpBackInSectionIsNotDisplayed() = assertFalse(jumpBackInSection().waitForExists(waitingTimeShort))

private fun assertRecentBookmarksSectionIsDisplayed() =
    assertTrue(recentBookmarksSection().waitForExists(waitingTime))

private fun assertRecentBookmarksSectionIsNotDisplayed() =
    assertFalse(recentBookmarksSection().waitForExists(waitingTimeShort))

private fun privateBrowsingButton() = onView(withId(R.id.privateBrowsingButton))

private fun saveTabsToCollectionButton() = onView(withId(R.id.add_tabs_to_collections_button))

private fun tabsCounter() = onView(withId(R.id.tab_button))

private fun jumpBackInSection() =
    mDevice.findObject(UiSelector().textContains(getStringResource(R.string.recent_tabs_header)))

private fun recentBookmarksSection() =
    mDevice.findObject(UiSelector().textContains(getStringResource(R.string.recent_bookmarks_title)))

private fun startBrowsingButton(): UiObject {
    val startBrowsingButton = mDevice.findObject(UiSelector().resourceId("$packageName:id/finish_button"))
    homeScreenList()
        .scrollIntoView(startBrowsingButton)
    homeScreenList()
        .ensureFullyVisible(startBrowsingButton)
    return startBrowsingButton
}

val deleteFromHistory =
    onView(
        allOf(
            withId(R.id.simple_text),
            withText(R.string.delete_from_history)
        )
    ).inRoot(RootMatchers.isPlatformPopup())

private val recentlyVisitedList =
    UiScrollable(
        UiSelector()
            .className("android.widget.HorizontalScrollView")
    ).setAsHorizontalList()
