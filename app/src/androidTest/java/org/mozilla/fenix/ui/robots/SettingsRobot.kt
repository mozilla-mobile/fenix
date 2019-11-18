/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the settings menu.
 */
class SettingsRobot {

    // BASICS SECTION
    fun verifyBasicsHeading() = assertBasicsHeading()

    fun verifySearchEngineButton() = assertSearchEngineButton()
    fun verifyThemeButton() = assertThemeButton()
    fun verifyThemeSelected() = assertThemeSelected()
    fun verifyAccessibilityButton() = assertAccessibilityButton()
    fun verifySetAsDefaultBrowserButton() = assertSetAsDefaultBrowserButton()

    // PRIVACY SECTION
    fun verifyPrivacyHeading() = assertPrivacyHeading()

    fun verifyEnhancedTrackingProtectionButton() = assertEnhancedTrackingProtectionButton()
    fun verifyLoginsButton() = assertLoginsButton()
    fun verifyEnhancedTrackingProtectionValue() = assertEnhancedTrackingProtectionValue()
    fun verifyAddPrivateBrowsingShortcutButton() = assertAddPrivateBrowsingShortcutButton()
    fun verifySitePermissionsButton() = assertSitePermissionsButton()
    fun verifyDeleteBrowsingDataButton() = assertDeleteBrowsingDataButton()
    fun verifyDeleteBrowsingDataOnQuitButton() = assertDeleteBrowsingDataOnQuitButton()
    fun verifyDataCollectionButton() = assertDataCollectionButton()
    fun verifyPrivacyNoticeButton() = assertPrivacyNoticeButton()
    fun verifyLeakCanaryButton() = assertLeakCanaryButton()
    fun verifySettingsView() = assertSettingsView()

    // DEVELOPER TOOLS SECTION
    fun verifyDeveloperToolsHeading() = assertDeveloperToolsHeading()

    fun verifyRemoteDebug() = assertRemoteDebug()

    // ABOUT SECTION
    fun verifyAboutHeading() = assertAboutHeading()

    fun verifyHelp() = assertHelp()
    fun verifyRateOnGooglePlay() = assertRateOnGooglePlay()
    fun verifyAboutFirefoxPreview() = assertAboutFirefoxPreview()

    class Transition {

        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().perform(ViewActions.click())

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun openSearchSubMenu(interact: SettingsSubMenuSearchRobot.() -> Unit): SettingsSubMenuSearchRobot.Transition {
            mDevice.waitForIdle()
            fun searchEngineButton() = onView(ViewMatchers.withText("Search"))
            searchEngineButton().click()

            SettingsSubMenuSearchRobot().interact()
            return SettingsSubMenuSearchRobot.Transition()
        }

        fun openThemeSubMenu(interact: SettingsSubMenuThemeRobot.() -> Unit): SettingsSubMenuThemeRobot.Transition {
            mDevice.waitForIdle()
            fun themeButton() = onView(ViewMatchers.withText("Theme"))
            themeButton().click()

            SettingsSubMenuThemeRobot().interact()
            return SettingsSubMenuThemeRobot.Transition()
        }

        fun openAccessibilitySubMenu(interact: SettingsSubMenuAccessibilityRobot.() -> Unit): SettingsSubMenuAccessibilityRobot.Transition {
            mDevice.waitForIdle()
            fun accessibilityButton() = onView(ViewMatchers.withText("Accessibility"))
            accessibilityButton().click()

            SettingsSubMenuAccessibilityRobot().interact()
            return SettingsSubMenuAccessibilityRobot.Transition()
        }

        fun openDefaultBrowserSubMenu(interact: SettingsSubMenuDefaultBrowserRobot.() -> Unit): SettingsSubMenuDefaultBrowserRobot.Transition {
            mDevice.waitForIdle()
            fun defaultBrowserButton() = onView(ViewMatchers.withText("Set as default browser"))
            defaultBrowserButton().click()

            SettingsSubMenuDefaultBrowserRobot().interact()
            return SettingsSubMenuDefaultBrowserRobot.Transition()
        }
    }
}

private fun assertSettingsView() {
    // verify that we are in the correct library view
    assertBasicsHeading()
    assertPrivacyHeading()
    assertDeveloperToolsHeading()
    assertAboutHeading()
}

// BASICS SECTION
private fun assertBasicsHeading() = onView(ViewMatchers.withText("Basics"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertSearchEngineButton() {
    mDevice.wait(Until.findObject(By.text("Search")), waitingTime)
    onView(ViewMatchers.withText("Search"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertThemeButton() = onView(ViewMatchers.withText("Theme"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertThemeSelected() = onView(ViewMatchers.withText("Light"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertAccessibilityButton() = onView(ViewMatchers.withText("Accessibility"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertSetAsDefaultBrowserButton() =
    onView(ViewMatchers.withText("Set as default browser"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

// PRIVACY SECTION
private fun assertPrivacyHeading() {
    onView(ViewMatchers.withText("Privacy"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertEnhancedTrackingProtectionButton() =
    onView(ViewMatchers.withText("Enhanced Tracking Protection"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertEnhancedTrackingProtectionValue() = onView(ViewMatchers.withText("On"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertLoginsButton() {
    TestHelper.scrollToElementByText("Passwords")
    onView(ViewMatchers.withText("Passwords"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertAddPrivateBrowsingShortcutButton() {
    TestHelper.scrollToElementByText("Add private browsing shortcut")
    mDevice.wait(Until.findObject(By.text("Add private browsing shortcut")), waitingTime)
    onView(ViewMatchers.withText("Add private browsing shortcut"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertSitePermissionsButton() {
    TestHelper.scrollToElementByText("Site permissions")
    onView(ViewMatchers.withText("Site permissions"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertDeleteBrowsingDataButton() {
    TestHelper.scrollToElementByText("Delete browsing data")
    onView(ViewMatchers.withText("Delete browsing data"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertDeleteBrowsingDataOnQuitButton() {
    TestHelper.scrollToElementByText("Delete browsing data on quit")
    onView(ViewMatchers.withText("Delete browsing data on quit"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertDataCollectionButton() = onView(ViewMatchers.withText("Data collection"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertPrivacyNoticeButton() = onView(ViewMatchers.withText("Privacy notice"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertLeakCanaryButton() = onView(ViewMatchers.withText("LeakCanary"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

// DEVELOPER TOOLS SECTION
private fun assertDeveloperToolsHeading() {
    TestHelper.scrollToElementByText("Developer tools")
    onView(ViewMatchers.withText("Developer tools"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertRemoteDebug() = onView(ViewMatchers.withText("Remote debugging via USB"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

// ABOUT SECTION
private fun assertAboutHeading() {
    TestHelper.scrollToElementByText("About")
    onView(ViewMatchers.withText("About"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertHelp() {
    TestHelper.scrollToElementByText("About Firefox Preview")
    onView(ViewMatchers.withText("Help"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertRateOnGooglePlay() {
    TestHelper.scrollToElementByText("About Firefox Preview")
    onView(ViewMatchers.withText("Rate on Google Play"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertAboutFirefoxPreview() {
    TestHelper.scrollToElementByText("About Firefox Preview")
    onView(ViewMatchers.withText("About Firefox Preview"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))
