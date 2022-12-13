/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.graphics.Bitmap
import android.widget.EditText
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove
import androidx.test.espresso.assertion.PositionAssertions.isPartiallyBelow
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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.Until.findObject
import mozilla.components.browser.state.state.searchEngines
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.Constants.LISTS_MAXSWIPES
import org.mozilla.fenix.helpers.Constants.LONG_CLICK_DURATION
import org.mozilla.fenix.helpers.MatcherHelper.assertCheckedItemWithResIdExists
import org.mozilla.fenix.helpers.MatcherHelper.assertItemContainingTextExists
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithResIdAndDescriptionExists
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithResIdAndTextExists
import org.mozilla.fenix.helpers.MatcherHelper.assertItemWithResIdExists
import org.mozilla.fenix.helpers.MatcherHelper.checkedItemWithResId
import org.mozilla.fenix.helpers.MatcherHelper.itemContainingText
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResId
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResIdAndDescription
import org.mozilla.fenix.helpers.MatcherHelper.itemWithResIdAndText
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.helpers.TestHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.TestHelper.scrollToElementByText
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.helpers.withBitmapDrawable

/**
 * Implementation of Robot Pattern for the home screen menu.
 */
class HomeScreenRobot {
    val privateSessionMessage =
        "$appName clears your search and browsing history from private tabs when you close them" +
            " or quit the app. While this doesn’t make you anonymous to websites or your internet" +
            " service provider, it makes it easier to keep what you do online private from anyone" +
            " else who uses this device."

    fun verifyNavigationToolbar() = assertItemWithResIdExists(navigationToolbar)
    fun verifyFocusedNavigationToolbar() = assertFocusedNavigationToolbar()
    fun verifyHomeScreen() = assertItemWithResIdExists(homeScreen)

    fun verifyHomeScreenAppBarItems() =
        assertItemWithResIdExists(homeScreen, privateBrowsingButton, homepageWordmark)

    fun verifyHomeScreenWelcomeItems() =
        assertItemContainingTextExists(welcomeHeader, welcomeSubHeader)

    fun verifyChooseYourThemeCard(
        isDarkThemeChecked: Boolean,
        isLightThemeChecked: Boolean,
        isAutomaticThemeChecked: Boolean,
    ) {
        scrollToElementByText(getStringResource(R.string.onboarding_theme_picker_header))
        assertItemContainingTextExists(
            chooseThemeHeader,
            chooseThemeText,
            darkThemeDescription,
            lightThemeDescription,
        )
        assertCheckedItemWithResIdExists(
            darkThemeToggle(isDarkThemeChecked),
            lightThemeToggle(isLightThemeChecked),
            automaticThemeToggle(isAutomaticThemeChecked),
        )
        assertItemWithResIdAndDescriptionExists(automaticThemeDescription)
    }

    fun verifyToolbarPlacementCard(isBottomChecked: Boolean, isTopChecked: Boolean) {
        scrollToElementByText(getStringResource(R.string.onboarding_toolbar_placement_header_1))
        assertItemContainingTextExists(toolbarPlacementHeader, toolbarPlacementDescription)
        assertCheckedItemWithResIdExists(
            toolbarPlacementBottomRadioButton(isBottomChecked),
            toolbarPlacementTopRadioButton(isTopChecked),
        )
        assertItemWithResIdExists(
            toolbarPlacementBottomImage,
            toolbarPlacementBottomTitle,
            toolbarPlacementTopImage,
            toolbarPlacementTopTitle,
        )
    }

    fun verifySignInToSyncCard() {
        scrollToElementByText(getStringResource(R.string.onboarding_account_sign_in_header))
        assertItemContainingTextExists(startSyncHeader, startSyncDescription)
        assertItemWithResIdExists(signInButton)
    }

    fun verifyPrivacyProtectionCard(isStandardChecked: Boolean, isStrictChecked: Boolean) {
        scrollToElementByText(getStringResource(R.string.onboarding_tracking_protection_header))
        assertItemContainingTextExists(privacyProtectionHeader, privacyProtectionDescription)
        assertCheckedItemWithResIdExists(
            standardTrackingProtectionToggle(isStandardChecked),
            strictTrackingProtectionToggle(isStrictChecked),
        )
    }

    fun verifyPrivacyNoticeCard() {
        scrollToElementByText(getStringResource(R.string.onboarding_privacy_notice_header_1))
        assertItemContainingTextExists(privacyNoticeHeader, privacyNoticeDescription)
        assertItemWithResIdExists(privacyNoticeButton)
    }

    fun verifyStartBrowsingSection() {
        scrollToElementByText(getStringResource(R.string.onboarding_finish))
        assertItemWithResIdExists(startBrowsingButton)
        assertItemContainingTextExists(conclusionHeader)
    }

    fun verifyNavigationToolbarItems(numberOfOpenTabs: String) {
        assertItemWithResIdExists(navigationToolbar, menuButton)
        assertItemWithResIdAndTextExists(tabCounter(numberOfOpenTabs))
    }

    fun verifyHomePrivateBrowsingButton() = assertItemWithResIdExists(privateBrowsingButton)
    fun verifyHomeMenuButton() = assertItemWithResIdExists(menuButton)
    fun verifyTabButton() = assertTabButton()
    fun verifyCollectionsHeader() = assertCollectionsHeader()
    fun verifyNoCollectionsText() = assertNoCollectionsText()
    fun verifyHomeWordmark() = assertItemWithResIdExists(homepageWordmark)
    fun verifyHomeComponent() = assertHomeComponent()
    fun verifyDefaultSearchEngine(searchEngine: String) = verifySearchEngineIcon(searchEngine)
    fun verifyTabCounter(numberOfOpenTabs: String) =
        assertItemWithResIdAndTextExists(tabCounter(numberOfOpenTabs))
    fun verifyKeyboardVisible() = assertKeyboardVisibility(isExpectedToBeVisible = true)

    fun verifyWallpaperImageApplied(isEnabled: Boolean) {
        if (isEnabled) {
            assertTrue(
                mDevice.findObject(
                    UiSelector().resourceId("$packageName:id/wallpaperImageView"),
                ).waitForExists(waitingTimeShort),
            )
        } else {
            assertFalse(
                mDevice.findObject(
                    UiSelector().resourceId("$packageName:id/wallpaperImageView"),
                ).waitForExists(waitingTimeShort),
            )
        }

        mDevice.findObject(UiSelector())
    }

    // First Run elements
    fun verifyWelcomeHeader() = assertItemContainingTextExists(welcomeHeader)
    fun verifyAccountsSignInButton() = assertItemWithResIdExists(signInButton)
    fun verifyStartBrowsingButton() {
        scrollToElementByText(getStringResource(R.string.onboarding_finish))
        assertItemWithResIdExists(startBrowsingButton)
    }

    // Upgrading users onboarding dialog
    fun verifyUpgradingUserOnboardingFirstScreen(testRule: ComposeTestRule) {
        testRule.also {
            it.onNodeWithText(getStringResource(R.string.onboarding_home_welcome_title_2))
                .assertIsDisplayed()

            it.onNodeWithText(getStringResource(R.string.onboarding_home_welcome_description))
                .assertIsDisplayed()

            it.onNodeWithText(getStringResource(R.string.onboarding_home_get_started_button))
                .assertIsDisplayed()
        }
    }

    fun clickGetStartedButton(testRule: ComposeTestRule) =
        testRule.onNodeWithText(getStringResource(R.string.onboarding_home_get_started_button)).performClick()

    fun verifyUpgradingUserOnboardingSecondScreen(testRule: ComposeTestRule) {
        testRule.also {
            it.onNodeWithText(getStringResource(R.string.onboarding_home_sync_title_3))
                .assertIsDisplayed()

            it.onNodeWithText(getStringResource(R.string.onboarding_home_sync_description))
                .assertIsDisplayed()

            it.onNodeWithText(getStringResource(R.string.onboarding_home_sign_in_button))
                .assertIsDisplayed()

            it.onNodeWithText(getStringResource(R.string.onboarding_home_skip_button))
                .assertIsDisplayed()
        }
    }

    fun clickSkipButton(testRule: ComposeTestRule) =
        testRule
            .onNodeWithText(getStringResource(R.string.onboarding_home_skip_button))
            .performClick()

    fun verifyPrivateSessionMessage() = assertPrivateSessionMessage()

    fun verifyExistingTopSitesList() = assertExistingTopSitesList()
    fun verifyNotExistingTopSitesList(title: String) = assertNotExistingTopSitesList(title)
    fun verifyNotExistingSponsoredTopSitesList() = assertSponsoredTopSitesNotDisplayed()
    fun verifyExistingTopSitesTabs(title: String) = assertExistingTopSitesTabs(title)
    fun verifyExistingSponsoredTopSitesTabs(sponsoredShortcutTitle: String, position: Int) = assertSponsoredTopSiteIsDisplayed(sponsoredShortcutTitle, position)
    fun verifyTopSiteContextMenuItems() = assertTopSiteContextMenuItems()

    fun verifyJumpBackInSectionIsDisplayed() = assertJumpBackInSectionIsDisplayed()
    fun verifyJumpBackInSectionIsNotDisplayed() = assertJumpBackInSectionIsNotDisplayed()
    fun verifyJumpBackInItemTitle(itemTitle: String) = assertJumpBackInItemTitle(itemTitle)
    fun verifyJumpBackInItemWithUrl(itemUrl: String) = assertJumpBackInItemWithUrl(itemUrl)
    fun verifyJumpBackInShowAllButton() = assertJumpBackInShowAllButton()
    fun verifyRecentlyVisitedSectionIsDisplayed() = assertRecentlyVisitedSectionIsDisplayed()
    fun verifyRecentlyVisitedSectionIsNotDisplayed() = assertRecentlyVisitedSectionIsNotDisplayed()
    fun verifyRecentBookmarksSectionIsDisplayed() = assertRecentBookmarksSectionIsDisplayed()
    fun verifyRecentBookmarksSectionIsNotDisplayed() = assertRecentBookmarksSectionIsNotDisplayed()
    fun verifyPocketSectionIsDisplayed() = assertPocketSectionIsDisplayed()
    fun verifyPocketSectionIsNotDisplayed() = assertPocketSectionIsNotDisplayed()

    fun verifyRecentlyVisitedSearchGroupDisplayed(shouldBeDisplayed: Boolean, searchTerm: String, groupSize: Int) {
        // checks if the search group exists in the Recently visited section
        if (shouldBeDisplayed) {
            scrollToElementByText("Recently visited")
            assertTrue(
                mDevice.findObject(UiSelector().text(searchTerm))
                    .getFromParent(UiSelector().text("$groupSize sites"))
                    .waitForExists(waitingTimeShort),
            )
        } else {
            assertTrue(
                mDevice.findObject(UiSelector().text(searchTerm))
                    .getFromParent(UiSelector().text("$groupSize sites"))
                    .waitUntilGone(waitingTimeShort),
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

    fun togglePrivateBrowsingModeOnOff() {
        onView(ViewMatchers.withResourceName("privateBrowsingButton"))
            .perform(click())
    }

    fun swipeToBottom() = onView(withId(R.id.homeLayout)).perform(ViewActions.swipeUp())

    fun swipeToTop() =
        onView(withId(R.id.sessionControlRecyclerView)).perform(ViewActions.swipeDown())

    fun verifySnackBarText(expectedText: String) {
        mDevice.waitNotNull(findObject(By.text(expectedText)), waitingTime)
    }

    fun clickUndoCollectionDeletion(expectedText: String) {
        onView(
            allOf(
                withId(R.id.snackbar_btn),
                withText(expectedText),
            ),
        ).click()
    }

    fun clickFirefoxLogo() = homepageWordmark.click()

    fun verifyThoughtProvokingStories(enabled: Boolean) {
        if (enabled) {
            scrollToElementByText(getStringResource(R.string.pocket_stories_header_1))
            assertTrue(
                mDevice.findObject(
                    UiSelector()
                        .textContains(
                            getStringResource(R.string.pocket_stories_header_1),
                        ),
                ).waitForExists(waitingTime),
            )
        } else {
            homeScreenList().scrollToEnd(LISTS_MAXSWIPES)
            assertFalse(
                mDevice.findObject(
                    UiSelector()
                        .textContains(
                            getStringResource(R.string.pocket_stories_header_1),
                        ),
                ).waitForExists(waitingTime),
            )
        }
    }

    fun scrollToPocketProvokingStories() =
        scrollToElementByText(getStringResource(R.string.pocket_stories_categories_header))

    fun swipePocketProvokingStories() {
        UiScrollable(UiSelector().resourceId("pocket.stories")).setAsHorizontalList()
            .swipeLeft(3)
    }

    fun verifyPocketRecommendedStoriesItems(composeTestRule: ComposeTestRule, vararg positions: Int) {
        composeTestRule.onNodeWithTag("pocket.stories").assertIsDisplayed()
        positions.forEach {
            composeTestRule.onNodeWithTag("pocket.stories")
                .onChildAt(it - 1)
                .assert(hasTestTag("pocket.recommended.story"))
        }
    }

    fun verifyPocketSponsoredStoriesItems(composeTestRule: ComposeTestRule, vararg positions: Int) {
        composeTestRule.onNodeWithTag("pocket.stories").assertIsDisplayed()
        positions.forEach {
            composeTestRule.onNodeWithTag("pocket.stories")
                .onChildAt(it - 1)
                .assert(hasTestTag("pocket.sponsored.story"))
        }
    }

    fun verifyDiscoverMoreStoriesButton(composeTestRule: ComposeTestRule, position: Int) {
        composeTestRule.onNodeWithTag("pocket.stories")
            .assertIsDisplayed()
            .onChildAt(position - 1)
            .assert(hasTestTag("pocket.discover.more.story"))
    }

    fun verifyStoriesByTopic(enabled: Boolean) {
        if (enabled) {
            scrollToElementByText(getStringResource(R.string.pocket_stories_categories_header))
            assertTrue(
                mDevice.findObject(
                    UiSelector()
                        .textContains(
                            getStringResource(R.string.pocket_stories_categories_header),
                        ),
                ).waitForExists(waitingTime),
            )
        } else {
            homeScreenList().scrollToEnd(LISTS_MAXSWIPES)
            assertFalse(
                mDevice.findObject(
                    UiSelector()
                        .textContains(
                            getStringResource(R.string.pocket_stories_categories_header),
                        ),
                ).waitForExists(waitingTime),
            )
        }
    }

    fun verifyStoriesByTopicItems() =
        assertTrue(mDevice.findObject(UiSelector().resourceId("pocket.categories")).childCount > 1)

    fun verifyStoriesByTopicItemState(composeTestRule: ComposeTestRule, isSelected: Boolean, position: Int) {
        homeScreenList().scrollIntoView(mDevice.findObject(UiSelector().resourceId("pocket.header")))

        if (isSelected) {
            composeTestRule.onNodeWithTag("pocket.categories").assertIsDisplayed()
            storyByTopicItem(composeTestRule, position).assertIsSelected()
        } else {
            composeTestRule.onNodeWithTag("pocket.categories").assertIsDisplayed()
            storyByTopicItem(composeTestRule, position).assertIsNotSelected()
        }
    }

    fun clickStoriesByTopicItem(composeTestRule: ComposeTestRule, position: Int) =
        storyByTopicItem(composeTestRule, position).performClick()

    fun verifyPoweredByPocket(rule: ComposeTestRule) {
        homeScreenList().scrollIntoView(mDevice.findObject(UiSelector().resourceId("pocket.header")))
        rule.onNodeWithTag("pocket.header.title", true).assertIsDisplayed()
        rule.onNodeWithTag("pocket.header.subtitle", true).assertIsDisplayed()
    }

    fun verifyCustomizeHomepageButton(enabled: Boolean) {
        if (enabled) {
            scrollToElementByText(getStringResource(R.string.browser_menu_customize_home_1))
            assertTrue(
                mDevice.findObject(
                    UiSelector()
                        .textContains("Customize homepage"),
                ).waitForExists(waitingTime),
            )
        } else {
            homeScreenList().scrollToEnd(LISTS_MAXSWIPES)
            assertFalse(
                mDevice.findObject(
                    UiSelector()
                        .textContains("Customize homepage"),
                ).waitForExists(waitingTime),
            )
        }
    }

    fun getSponsoredShortcutTitle(position: Int): String {
        val sponsoredShortcut = mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/top_site_item")
                .index(position - 1),
        ).getChild(
            UiSelector()
                .resourceId("$packageName:id/top_site_title"),
        ).text

        return sponsoredShortcut
    }

    fun verifyJumpBackInMessage() {
        assertTrue(
            mDevice.findObject(
                UiSelector().text(
                    getStringResource(R.string.onboarding_home_screen_jump_back_contextual_hint_2),
                ),
            ).waitForExists(waitingTime),
        )
    }

    fun getProvokingStoryPublisher(position: Int): String {
        val publisher = mDevice.findObject(
            UiSelector()
                .className("android.view.View")
                .index(position - 1),
        ).getChild(
            UiSelector()
                .className("android.widget.TextView")
                .index(1),
        ).text

        return publisher
    }

    fun verifyToolbarPosition(defaultPosition: Boolean) {
        onView(withId(R.id.toolbarLayout))
            .check(
                if (defaultPosition) {
                    isPartiallyBelow(withId(R.id.sessionControlRecyclerView))
                } else {
                    isCompletelyAbove(withId(R.id.homeAppBar))
                },
            )
    }

    class Transition {

        fun openTabDrawer(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            mDevice.findObject(
                UiSelector().descriptionContains("open tab. Tap to switch tabs."),
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
                    waitingTime,
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
            navigationToolbar.waitForExists(waitingTime)
            navigationToolbar.click()

            SearchRobot().interact()
            return SearchRobot.Transition()
        }

        fun dismissOnboarding() {
            openThreeDotMenu { }.openSettings { }.goBack { }
        }

        fun clickStartBrowsingButton(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
            startBrowsingButton.click()

            SearchRobot().interact()
            return SearchRobot.Transition()
        }

        fun clickUpgradingUserOnboardingSignInButton(
            testRule: ComposeTestRule,
            interact: SyncSignInRobot.() -> Unit,
        ): SyncSignInRobot.Transition {
            testRule.onNodeWithText("Sign in").performClick()

            SyncSignInRobot().interact()
            return SyncSignInRobot.Transition()
        }

        fun togglePrivateBrowsingMode() {
            mDevice.findObject(UiSelector().resourceId("$packageName:id/privateBrowsingButton"))
                .waitForExists(
                    waitingTime,
                )
            privateBrowsingButton.click()
        }

        fun triggerPrivateBrowsingShortcutPrompt(interact: AddToHomeScreenRobot.() -> Unit): AddToHomeScreenRobot.Transition {
            // Loop to press the PB icon for 5 times to display the Add the Private Browsing Shortcut CFR
            for (i in 1..5) {
                mDevice.findObject(UiSelector().resourceId("$packageName:id/privateBrowsingButton"))
                    .waitForExists(
                        waitingTime,
                    )

                privateBrowsingButton.click()
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
            navigationToolbar.click()

            NavigationToolbarRobot().interact()
            return NavigationToolbarRobot.Transition()
        }

        fun openContextMenuOnTopSitesWithTitle(
            title: String,
            interact: HomeScreenRobot.() -> Unit,
        ): Transition {
            onView(withId(R.id.top_sites_list)).perform(
                actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(title)),
                    ViewActions.longClick(),
                ),
            )

            HomeScreenRobot().interact()
            return Transition()
        }

        fun openContextMenuOnSponsoredShortcut(sponsoredShortcutTitle: String, interact: HomeScreenRobot.() -> Unit): Transition {
            sponsoredShortcut(sponsoredShortcutTitle).click(LONG_CLICK_DURATION)

            HomeScreenRobot().interact()
            return Transition()
        }

        fun openTopSiteTabWithTitle(
            title: String,
            interact: BrowserRobot.() -> Unit,
        ): BrowserRobot.Transition {
            onView(withId(R.id.top_sites_list)).perform(
                actionOnItem<RecyclerView.ViewHolder>(hasDescendant(withText(title)), click()),
            )

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openSponsoredShortcut(sponsoredShortcutTitle: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            sponsoredShortcut(sponsoredShortcutTitle).click()

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
                UiSelector().resourceId("$packageName:id/simple_text"),
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

        fun clickSponsorsAndPrivacyButton(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            sponsorsAndPrivacyButton.waitForExists(waitingTime)
            sponsorsAndPrivacyButton.clickAndWaitForNewWindow(waitingTime)

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickSponsoredShortcutsSettingsButton(interact: SettingsSubMenuHomepageRobot.() -> Unit): SettingsSubMenuHomepageRobot.Transition {
            sponsoredShortcutsSettingsButton.waitForExists(waitingTime)
            sponsoredShortcutsSettingsButton.clickAndWaitForNewWindow(waitingTime)

            SettingsSubMenuHomepageRobot().interact()
            return SettingsSubMenuHomepageRobot.Transition()
        }

        fun openCommonMythsLink(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.findObject(
                UiSelector()
                    .textContains(
                        getStringResource(R.string.private_browsing_common_myths),
                    ),
            ).also { it.click() }

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickSaveTabsToCollectionButton(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            saveTabsToCollectionButton().click()

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun expandCollection(title: String, rule: ComposeTestRule, interact: CollectionRobot.() -> Unit): CollectionRobot.Transition {
            homeScreenList().waitForExists(waitingTime)
            homeScreenList().scrollToEnd(LISTS_MAXSWIPES)

            collectionTitle(title, rule)
                .assertIsDisplayed()
                .performClick()

            CollectionRobot().interact()
            return CollectionRobot.Transition()
        }

        fun openRecentlyVisitedSearchGroupHistoryList(title: String, interact: HistoryRobot.() -> Unit): HistoryRobot.Transition {
            scrollToElementByText("Recently visited")
            val searchGroup = mDevice.findObject(UiSelector().text(title))
            searchGroup.waitForExists(waitingTimeShort)
            searchGroup.click()

            HistoryRobot().interact()
            return HistoryRobot.Transition()
        }

        fun openCustomizeHomepage(interact: SettingsSubMenuHomepageRobot.() -> Unit): SettingsSubMenuHomepageRobot.Transition {
            homeScreenList().scrollToEnd(LISTS_MAXSWIPES)
            mDevice.findObject(
                UiSelector()
                    .textContains(
                        "Customize homepage",
                    ),
            ).clickAndWaitForNewWindow(waitingTime)

            SettingsSubMenuHomepageRobot().interact()
            return SettingsSubMenuHomepageRobot.Transition()
        }

        fun clickJumpBackInShowAllButton(interact: TabDrawerRobot.() -> Unit): TabDrawerRobot.Transition {
            mDevice
                .findObject(
                    UiSelector()
                        .textContains(getStringResource(R.string.recent_tabs_show_all)),
                ).clickAndWaitForNewWindow(waitingTime)

            TabDrawerRobot().interact()
            return TabDrawerRobot.Transition()
        }

        fun clickJumpBackInItemWithTitle(itemTitle: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice
                .findObject(
                    UiSelector()
                        .resourceId("recent.tab.title")
                        .textContains(itemTitle),
                ).clickAndWaitForNewWindow(waitingTime)

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickPocketStoryItem(publisher: String, position: Int, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.findObject(
                UiSelector()
                    .className("android.view.View")
                    .index(position - 1),
            ).getChild(
                UiSelector()
                    .className("android.widget.TextView")
                    .index(1)
                    .textContains(publisher),
            ).clickAndWaitForNewWindow(waitingTime)

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickPocketDiscoverMoreButton(composeTestRule: ComposeTestRule, position: Int, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            composeTestRule.onNodeWithTag("pocket.stories")
                .assertIsDisplayed()
                .onChildAt(position - 1)
                .assert(hasTestTag("pocket.discover.more.story"))
                .performClick()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickPocketLearnMoreLink(composeTestRule: ComposeTestRule, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            composeTestRule.onNodeWithTag("pocket.header.subtitle", true).performClick()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

fun homeScreen(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
    HomeScreenRobot().interact()
    return HomeScreenRobot.Transition()
}

private fun homeScreenList() =
    UiScrollable(
        UiSelector()
            .resourceId("$packageName:id/sessionControlRecyclerView")
            .scrollable(true),
    ).setAsVerticalList()

private fun assertKeyboardVisibility(isExpectedToBeVisible: Boolean) =
    Assert.assertEquals(
        isExpectedToBeVisible,
        mDevice
            .executeShellCommand("dumpsys input_method | grep mInputShown")
            .contains("mInputShown=true"),
    )

private fun assertFocusedNavigationToolbar() =
    onView(allOf(withHint("Search or enter address")))
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
                    "Group together similar searches, sites, and tabs for quick access later.",
            ),
        ),
    ).check(matches(isDisplayed()))

private fun assertHomeComponent() =
    onView(ViewMatchers.withResourceName("sessionControlRecyclerView"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun threeDotButton() = onView(allOf(withId(R.id.menuButton)))

private fun verifySearchEngineIcon(searchEngineIcon: Bitmap, searchEngineName: String) {
    onView(withId(R.id.search_engine_icon))
        .check(matches(withBitmapDrawable(searchEngineIcon, searchEngineName)))
}

private fun getSearchEngine(searchEngineName: String) =
    appContext.components.core.store.state.search.searchEngines.find { it.name == searchEngineName }

private fun verifySearchEngineIcon(searchEngineName: String) {
    val defaultSearchEngine = getSearchEngine(searchEngineName)
        ?: throw AssertionError("No search engine with name $searchEngineName")
    verifySearchEngineIcon(defaultSearchEngine.icon, defaultSearchEngine.name)
}

private fun assertPrivateSessionMessage() =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .textContains(
                    getStringResource(R.string.private_browsing_common_myths),
                ),
        ).waitForExists(waitingTime),
    )

private fun collectionTitle(title: String, rule: ComposeTestRule) =
    rule.onNode(hasText(title))

private fun assertExistingTopSitesList() =
    onView(allOf(withId(R.id.top_sites_list)))
        .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertExistingTopSitesTabs(title: String) {
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/top_site_title")
            .textContains(title),
    ).waitForExists(waitingTime)

    onView(allOf(withId(R.id.top_sites_list)))
        .check(matches(hasDescendant(withText(title))))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertSponsoredTopSiteIsDisplayed(sponsoredShortcutTitle: String, position: Int) {
    assertSponsoredShortcutTitle(sponsoredShortcutTitle, position)
    assertSponsoredSubtitleIsDisplayed(position)
}

private fun assertSponsoredSubtitleIsDisplayed(position: Int) =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/top_site_item")
                .index(position - 1),
        ).getChild(
            UiSelector()
                .resourceId("$packageName:id/top_site_subtitle"),
        ).waitForExists(waitingTime),
    )

private fun assertSponsoredShortcutTitle(sponsoredShortcutTitle: String, position: Int) {
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/top_site_item")
                .index(position - 1),
        ).getChild(
            UiSelector()
                .textContains(sponsoredShortcutTitle),
        ).waitForExists(waitingTime),
    )
}

private fun assertNotExistingTopSitesList(title: String) {
    mDevice.findObject(UiSelector().text(title)).waitUntilGone(waitingTime)

    assertFalse(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/top_site_title")
                .textContains(title),
        ).waitForExists(waitingTime),
    )
}

private fun assertSponsoredTopSitesNotDisplayed() {
    assertFalse(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/top_site_subtitle")
                .textContains(getStringResource(R.string.top_sites_sponsored_label)),
        ).waitForExists(waitingTime),
    )
}

private fun assertTopSiteContextMenuItems() {
    mDevice.waitNotNull(
        findObject(By.text("Open in private tab")),
        waitingTime,
    )
    mDevice.waitNotNull(
        findObject(By.text("Remove")),
        waitingTime,
    )
}

private fun assertJumpBackInSectionIsDisplayed() = assertTrue(jumpBackInSection().waitForExists(waitingTime))

private fun assertJumpBackInSectionIsNotDisplayed() = assertFalse(jumpBackInSection().waitForExists(waitingTimeShort))

private fun assertJumpBackInItemTitle(itemTitle: String) =
    assertTrue(
        mDevice
            .findObject(
                UiSelector()
                    .resourceId("recent.tab.title")
                    .textContains(itemTitle),
            ).waitForExists(waitingTime),
    )

private fun assertJumpBackInItemWithUrl(itemUrl: String) =
    assertTrue(
        mDevice
            .findObject(
                UiSelector()
                    .resourceId("recent.tab.url")
                    .textContains(itemUrl),
            ).waitForExists(waitingTime),
    )

private fun assertJumpBackInShowAllButton() =
    assertTrue(
        mDevice
            .findObject(
                UiSelector()
                    .textContains(getStringResource(R.string.recent_tabs_show_all)),
            ).waitForExists(waitingTime),
    )

private fun assertRecentlyVisitedSectionIsDisplayed() = assertTrue(recentlyVisitedSection().waitForExists(waitingTime))

private fun assertRecentlyVisitedSectionIsNotDisplayed() = assertFalse(recentlyVisitedSection().waitForExists(waitingTime))

private fun assertRecentBookmarksSectionIsDisplayed() =
    assertTrue(recentBookmarksSection().waitForExists(waitingTime))

private fun assertRecentBookmarksSectionIsNotDisplayed() =
    assertFalse(recentBookmarksSection().waitForExists(waitingTimeShort))

private fun assertPocketSectionIsDisplayed() = assertTrue(pocketSection().waitForExists(waitingTime))

private fun assertPocketSectionIsNotDisplayed() = assertFalse(pocketSection().waitForExists(waitingTime))

private fun saveTabsToCollectionButton() = onView(withId(R.id.add_tabs_to_collections_button))

private fun tabsCounter() = onView(withId(R.id.tab_button))

private fun jumpBackInSection() =
    mDevice.findObject(UiSelector().textContains(getStringResource(R.string.recent_tabs_header)))

private fun recentlyVisitedSection() =
    mDevice.findObject(UiSelector().textContains(getStringResource(R.string.history_metadata_header_2)))

private fun recentBookmarksSection() =
    mDevice.findObject(UiSelector().textContains(getStringResource(R.string.recently_saved_title)))

private fun pocketSection() =
    mDevice.findObject(UiSelector().textContains(getStringResource(R.string.pocket_stories_header_1)))

private fun sponsoredShortcut(sponsoredShortcutTitle: String) =
    mDevice.findObject(
        By
            .res("$packageName:id/top_site_title")
            .textContains(sponsoredShortcutTitle),
    )

private fun storyByTopicItem(composeTestRule: ComposeTestRule, position: Int) =
    composeTestRule.onNodeWithTag("pocket.categories").onChildAt(position - 1)

private val homeScreen =
    itemWithResId("$packageName:id/homeLayout")
private val privateBrowsingButton =
    itemWithResId("$packageName:id/privateBrowsingButton")
private val homepageWordmark =
    itemWithResId("$packageName:id/wordmark")
private val welcomeHeader = itemContainingText(getStringResource(R.string.onboarding_header_2))
private val welcomeSubHeader =
    itemContainingText(getStringResource(R.string.onboarding_message))
private val chooseThemeHeader =
    itemContainingText(getStringResource(R.string.onboarding_theme_picker_header))
private val chooseThemeText =
    itemContainingText(getStringResource(R.string.onboarding_theme_picker_description_2))
private val darkThemeDescription =
    itemContainingText(getStringResource(R.string.onboarding_theme_dark_title))
private val lightThemeDescription =
    itemContainingText(getStringResource(R.string.onboarding_theme_light_title))
private val automaticThemeDescription =
    itemWithResIdAndDescription(
        "$packageName:id/clickable_region_automatic",
        "${getStringResource(R.string.onboarding_theme_automatic_title)} ${getStringResource(R.string.onboarding_theme_automatic_summary)}",
    )
private fun darkThemeToggle(isChecked: Boolean) =
    checkedItemWithResId("$packageName:id/theme_dark_radio_button", isChecked)
private fun lightThemeToggle(isChecked: Boolean) =
    checkedItemWithResId("$packageName:id/theme_light_radio_button", isChecked)
private fun automaticThemeToggle(isChecked: Boolean) =
    checkedItemWithResId("$packageName:id/theme_automatic_radio_button", isChecked)
private val toolbarPlacementHeader =
    itemContainingText(getStringResource(R.string.onboarding_toolbar_placement_header_1))
private val toolbarPlacementDescription =
    itemContainingText(getStringResource(R.string.onboarding_toolbar_placement_description))
private fun toolbarPlacementBottomRadioButton(isChecked: Boolean) =
    checkedItemWithResId("$packageName:id/toolbar_bottom_radio_button", isChecked)
private fun toolbarPlacementTopRadioButton(isChecked: Boolean) =
    checkedItemWithResId("$packageName:id/toolbar_top_radio_button", isChecked)
private val toolbarPlacementBottomImage =
    itemWithResId("$packageName:id/toolbar_bottom_image")
private val toolbarPlacementBottomTitle =
    itemWithResId("$packageName:id/toolbar_bottom_title")
private val toolbarPlacementTopTitle =
    itemWithResId("$packageName:id/toolbar_top_title")
private val toolbarPlacementTopImage =
    itemWithResId("$packageName:id/toolbar_top_image")
private val startSyncHeader =
    itemContainingText(getStringResource(R.string.onboarding_account_sign_in_header))
private val startSyncDescription =
    itemContainingText(getStringResource(R.string.onboarding_manual_sign_in_description))
private val signInButton =
    itemWithResId("$packageName:id/fxa_sign_in_button")
private val privacyProtectionHeader =
    itemContainingText(getStringResource(R.string.onboarding_tracking_protection_header))
private val privacyProtectionDescription =
    itemContainingText(getStringResource(R.string.onboarding_tracking_protection_description))
private fun standardTrackingProtectionToggle(isChecked: Boolean) =
    checkedItemWithResId("$packageName:id/tracking_protection_standard_option", isChecked)
private fun strictTrackingProtectionToggle(isChecked: Boolean) =
    checkedItemWithResId("$packageName:id/tracking_protection_strict_default", isChecked)
private val privacyNoticeHeader =
    itemContainingText(getStringResource(R.string.onboarding_privacy_notice_header_1))
private val privacyNoticeDescription =
    itemContainingText(getStringResource(R.string.onboarding_privacy_notice_description))
private val privacyNoticeButton =
    itemWithResId("$packageName:id/read_button")
private val startBrowsingButton =
    itemWithResId("$packageName:id/finish_button")
private val conclusionHeader =
    itemContainingText(getStringResource(R.string.onboarding_conclusion_header))
private val navigationToolbar =
    itemWithResId("$packageName:id/toolbar")
private val menuButton =
    itemWithResId("$packageName:id/menuButton")
private fun tabCounter(numberOfOpenTabs: String) =
    itemWithResIdAndText("$packageName:id/counter_text", numberOfOpenTabs)

val deleteFromHistory =
    onView(
        allOf(
            withId(R.id.simple_text),
            withText(R.string.delete_from_history),
        ),
    ).inRoot(RootMatchers.isPlatformPopup())

private val recentlyVisitedList =
    UiScrollable(
        UiSelector()
            .className("android.widget.HorizontalScrollView"),
    ).setAsHorizontalList()

private val sponsoredShortcutsSettingsButton =
    mDevice
        .findObject(
            UiSelector()
                .textContains(getStringResource(R.string.top_sites_menu_settings))
                .resourceId("$packageName:id/simple_text"),
        )

private val sponsorsAndPrivacyButton =
    mDevice
        .findObject(
            UiSelector()
                .textContains(getStringResource(R.string.top_sites_menu_sponsor_privacy))
                .resourceId("$packageName:id/simple_text"),
        )
