/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.core.content.pm.PackageInfoCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.startsWith
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.helpers.click
import org.mozilla.geckoview.BuildConfig as GeckoViewBuildConfig

/**
 * Implementation of Robot Pattern for the settings search sub menu.
 */
class SettingsSubMenuAboutRobot {

    fun verifyBuildNo() = assertBuildNo()
    fun verifyVersionNo() = assertVersionNo()
    fun verifyProducedBy() = assertProducedBy()
    fun verifyTimeStamp() = assertTimestamp()
    fun verifyOpenSourceLibraries() = assertOpenSourceLibraries()
    fun verifyRateOnGooglePlaySubmenu() = assertRateOnGooglePlaySubmenu()
    fun verifyRedirectToSupport() = assertRedirectToSupport()

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().perform(ViewActions.click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))

private fun assertBuildNo() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext

    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toString()

    val buildNo = "${packageInfo.versionName} (Build #$versionCode)\n"

    onView(ViewMatchers.withText(startsWith(buildNo)))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}
private fun assertVersionNo() {
    val componentsVersion = "${mozilla.components.Build.version}, ${mozilla.components.Build.gitHash}"
    val geckoVersion = GeckoViewBuildConfig.MOZ_APP_VERSION + "-" + GeckoViewBuildConfig.MOZ_APP_BUILDID

    onView(ViewMatchers.withText(containsString(componentsVersion)))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(ViewMatchers.withText(containsString(geckoVersion)))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertProducedBy() {
    onView(ViewMatchers.withText("Firefox Preview is produced by Mozilla."))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertTimestamp() {
    val buildDate = BuildConfig.BUILD_DATE
    onView(ViewMatchers.withText(buildDate))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertOpenSourceLibraries() {
    onView(ViewMatchers.withText("Open source libraries we use"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}
private fun assertRateOnGooglePlaySubmenu() {
    onView(ViewMatchers.withText("Rate on Google Play"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    goBackButton().click()
}
private fun assertRedirectToSupport() {
    onView(CoreMatchers.allOf(ViewMatchers.withText(startsWith("https://support.mozilla.org/"))))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}