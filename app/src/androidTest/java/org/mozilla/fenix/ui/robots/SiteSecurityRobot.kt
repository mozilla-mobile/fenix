/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.UiSelector
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName

/**
 * Implementation of Robot Pattern for Site Security UI.
 */
class SiteSecurityRobot {

    fun verifyQuickActionSheet(url: String = "", isConnectionSecure: Boolean) = assertQuickActionSheet(url, isConnectionSecure)
    fun openSecureConnectionSubMenu(isConnectionSecure: Boolean) =
        quickActionSheetSecurityInfo(isConnectionSecure).clickAndWaitForNewWindow(waitingTime)
    fun verifySecureConnectionSubMenu(pageTitle: String = "", url: String = "", isConnectionSecure: Boolean) =
        assertSecureConnectionSubMenu(pageTitle, url, isConnectionSecure)
    fun clickQuickActionSheetClearSiteData() = quickActionSheetClearSiteData().click()
    fun verifyClearSiteDataPrompt(url: String) = assertClearSiteDataPrompt(url)

    class Transition
}

private fun assertQuickActionSheet(url: String = "", isConnectionSecure: Boolean) {
    quickActionSheet().waitForExists(waitingTime)
    assertTrue(quickActionSheetUrl(url.tryGetHostFromUrl()).waitForExists(waitingTime))
    assertTrue(quickActionSheetSecurityInfo(isConnectionSecure).waitForExists(waitingTime))
    assertTrue(quickActionSheetTrackingProtectionSwitch().waitForExists(waitingTime))
    assertTrue(quickActionSheetClearSiteData().waitForExists(waitingTime))
}

private fun assertSecureConnectionSubMenu(pageTitle: String = "", url: String = "", isConnectionSecure: Boolean) {
    secureConnectionSubMenu().waitForExists(waitingTime)
    assertTrue(secureConnectionSubMenuPageTitle(pageTitle).waitForExists(waitingTime))
    assertTrue(secureConnectionSubMenuPageUrl(url).waitForExists(waitingTime))
    assertTrue(secureConnectionSubMenuLockIcon().waitForExists(waitingTime))
    assertTrue(secureConnectionSubMenuSecurityInfo(isConnectionSecure).waitForExists(waitingTime))
    assertTrue(secureConnectionSubMenuCertificateInfo().waitForExists(waitingTime))
}

private fun assertClearSiteDataPrompt(url: String) {
    assertTrue(clearSiteDataPrompt(url).waitForExists(waitingTime))
    cancelClearSiteDataButton.check(matches(isDisplayed()))
    deleteSiteDataButton.check(matches(isDisplayed()))
}

private fun quickActionSheet() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/quick_action_sheet"))

private fun quickActionSheetUrl(url: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/url")
            .textContains(url),
    )

private fun quickActionSheetSecurityInfo(isConnectionSecure: Boolean) =
    if (isConnectionSecure) {
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/securityInfo")
                .textContains(getStringResource(R.string.quick_settings_sheet_secure_connection_2)),
        )
    } else {
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/securityInfo")
                .textContains(getStringResource(R.string.quick_settings_sheet_insecure_connection_2)),
        )
    }

private fun quickActionSheetTrackingProtectionSwitch() =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/trackingProtectionSwitch"),
    )

private fun quickActionSheetClearSiteData() =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/clearSiteData"),
    )

private fun secureConnectionSubMenu() =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/design_bottom_sheet"),
    )

private fun secureConnectionSubMenuPageTitle(pageTitle: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/title")
            .textContains(pageTitle),
    )

private fun secureConnectionSubMenuPageUrl(url: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/url")
            .textContains(url),
    )

private fun secureConnectionSubMenuLockIcon() =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/securityInfoIcon"),
    )

private fun secureConnectionSubMenuSecurityInfo(isConnectionSecure: Boolean) =
    if (isConnectionSecure) {
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/securityInfo")
                .textContains(getStringResource(R.string.quick_settings_sheet_secure_connection_2)),
        )
    } else {
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/securityInfo")
                .textContains(getStringResource(R.string.quick_settings_sheet_insecure_connection_2)),
        )
    }

private fun secureConnectionSubMenuCertificateInfo() =
    mDevice.findObject(
        UiSelector()
            .resourceId("$packageName:id/securityInfo"),
    )

private fun clearSiteDataPrompt(url: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("android:id/message")
            .textContains(url),
    )

private val cancelClearSiteDataButton = onView(withId(android.R.id.button2)).inRoot(RootMatchers.isDialog())
private val deleteSiteDataButton = onView(withId(android.R.id.button1)).inRoot(RootMatchers.isDialog())
