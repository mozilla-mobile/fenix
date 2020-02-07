/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.isChecked

/**
 * Implementation of Robot Pattern for the settings Delete Browsing Data sub menu.
 */
class SettingsSubMenuDeleteBrowsingDataRobot {

    fun verifyBrowsingData(numOfTabs: Int, numOfAddresses: Int) =
        assertBrowsingData(numOfTabs, numOfAddresses)

    fun deleteOpenTabs() = clearOpenTabs()

    fun deleteBrowsingHistoryAndSiteData() = clearBrowsingHistoryAndSiteData()

    fun deleteCookies() = clearCookies()

    fun deleteCachedImagesAndFiles() = clearCachedImagesAndFiles()

    fun deleteSitePermissions() = clearSitePermissions()

    fun verifyZeroOpenTabs() = assertZeroOpenTabs()

    fun verifyBrowsingHistoryAndSiteData() = assertBrowsingHistoryAndSiteData()

    fun verifyZeroCookies() = assertZeroCookies()

    fun verifyZeroCachedImagesAndFiles() = assertZeroCache()

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun clearBrowsingData(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            deleteBrowsingData()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun deleteBrowsingData() {
    onView(withId(R.id.delete_data))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .perform(click())

    onView(withText("Delete"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .perform(click())

    mDevice.findObject(UiSelector().text("Browsing data deleted"))
        .waitUntilGone(5000)
}

private fun assertBrowsingData(numOfTabs: Int, numOfAddresses: Int) {
    onView(withText("$numOfTabs tabs"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("$numOfAddresses addresses"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun clearOpenTabs() {
    browsingHistoryAndSiteDataCheckbox
        .perform(click())
    browsingHistoryAndSiteDataCheckbox
        .check(matches(isChecked(false)))

    cookiesCheckbox
        .perform(click())
    cookiesCheckbox
        .check(matches(isChecked(false)))

    cachedImagesAndFilesCheckbox
        .perform(click())
    cachedImagesAndFilesCheckbox
        .check(matches(isChecked(false)))

    sitePermissionsCheckbox
        .perform(click())
    sitePermissionsCheckbox
        .check(matches(isChecked(false)))

    openTabsCheckbox
        .check(matches(isChecked(true)))

    deleteBrowsingData()
}

private fun clearBrowsingHistoryAndSiteData() {
    openTabsCheckbox
        .perform(click())
    openTabsCheckbox
        .check(matches(isChecked(false)))

    cookiesCheckbox
        .perform(click())
    cookiesCheckbox
        .check(matches(isChecked(false)))

    cachedImagesAndFilesCheckbox
        .perform(click())
    cachedImagesAndFilesCheckbox
        .check(matches(isChecked(false)))

    sitePermissionsCheckbox
        .perform(click())
    sitePermissionsCheckbox
        .check(matches(isChecked(false)))

    browsingHistoryAndSiteDataCheckbox
        .check(matches(isChecked(true)))

    deleteBrowsingData()
}

private fun clearCookies() {
    openTabsCheckbox
        .perform(click())
    openTabsCheckbox
        .check(matches(isChecked(false)))

    browsingHistoryAndSiteDataCheckbox
        .perform(click())
    browsingHistoryAndSiteDataCheckbox
        .check(matches(isChecked(false)))

    cachedImagesAndFilesCheckbox
        .perform(click())
    cachedImagesAndFilesCheckbox
        .check(matches(isChecked(false)))

    sitePermissionsCheckbox
        .perform(click())
    sitePermissionsCheckbox
        .check(matches(isChecked(false)))

    cookiesCheckbox
        .check(matches(isChecked(true)))

    deleteBrowsingData()
}

private fun clearCachedImagesAndFiles() {
    openTabsCheckbox
        .perform(click())
    openTabsCheckbox
        .check(matches(isChecked(false)))

    browsingHistoryAndSiteDataCheckbox
        .perform(click())
    browsingHistoryAndSiteDataCheckbox
        .check(matches(isChecked(false)))

    cookiesCheckbox
        .perform(click())
    cookiesCheckbox
        .check(matches(isChecked(false)))

    sitePermissionsCheckbox
        .perform(click())
    sitePermissionsCheckbox
        .check(matches(isChecked(false)))

    cachedImagesAndFilesCheckbox
        .check(matches(isChecked(true)))

    deleteBrowsingData()
}

private fun clearSitePermissions() {
    openTabsCheckbox
        .perform(click())
    openTabsCheckbox
        .check(matches(isChecked(false)))

    browsingHistoryAndSiteDataCheckbox
        .perform(click())
    browsingHistoryAndSiteDataCheckbox
        .check(matches(isChecked(false)))

    cookiesCheckbox
        .perform(click())
    cookiesCheckbox
        .check(matches(isChecked(false)))

    cachedImagesAndFilesCheckbox
        .perform(click())
    cachedImagesAndFilesCheckbox
        .check(matches(isChecked(false)))

    sitePermissionsCheckbox
        .check(matches(isChecked(true)))

    deleteBrowsingData()

    sitePermissionsSubtitle
        .check(matches(withEffectiveVisibility(Visibility.GONE)))
}

private fun assertZeroOpenTabs() {
    openTabsSubtitle
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .check(matches(withText("0 tabs")))
}

private fun assertBrowsingHistoryAndSiteData() {
    browsingHistoryAndSiteDataSubtitle
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .check(matches(withText("0 addresses")))
}

private fun assertZeroCookies() {
    cookiesSubtitle
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .check(matches(withText("You’ll be logged out of most sites")))
}

private fun assertZeroCache() {
    cachedImagesAndFilesSubtitle
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .check(matches(withText("Frees up storage space")))
}

fun settingsSubMenuDeleteBrowsingData(interact: SettingsSubMenuDeleteBrowsingDataRobot.() -> Unit): SettingsSubMenuDeleteBrowsingDataRobot.Transition {
    SettingsSubMenuDeleteBrowsingDataRobot().interact()
    return SettingsSubMenuDeleteBrowsingDataRobot.Transition()
}

// Checkbox
private val openTabsCheckbox =
    onView(allOf(isDescendantOfA(withId(R.id.open_tabs_item)), withId(R.id.checkbox)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private val browsingHistoryAndSiteDataCheckbox =
    onView(allOf(isDescendantOfA(withId(R.id.browsing_data_item)), withId(R.id.checkbox)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private val cookiesCheckbox =
    onView(allOf(isDescendantOfA(withId(R.id.cookies_item)), withId(R.id.checkbox)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private val cachedImagesAndFilesCheckbox =
    onView(allOf(isDescendantOfA(withId(R.id.cached_files_item)), withId(R.id.checkbox)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private val sitePermissionsCheckbox =
    onView(allOf(isDescendantOfA(withId(R.id.site_permissions_item)), withId(R.id.checkbox)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

// Subtitle
private val openTabsSubtitle =
    onView(allOf(isDescendantOfA(withId(R.id.open_tabs_item)), withId(R.id.subtitle)))

private val browsingHistoryAndSiteDataSubtitle =
    onView(allOf(isDescendantOfA(withId(R.id.browsing_data_item)), withId(R.id.subtitle)))

private val cookiesSubtitle =
    onView(allOf(isDescendantOfA(withId(R.id.cookies_item)), withId(R.id.subtitle)))

private val cachedImagesAndFilesSubtitle =
    onView(allOf(isDescendantOfA(withId(R.id.cached_files_item)), withId(R.id.subtitle)))

private val sitePermissionsSubtitle =
    onView(allOf(isDescendantOfA(withId(R.id.site_permissions_item)), withId(R.id.subtitle)))

private fun goBackButton() =
    onView(CoreMatchers.allOf(withContentDescription("Navigate up")))
