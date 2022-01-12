/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.Matchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.ext.waitNotNull

class ShareOverlayRobot {

    // This function verifies the share layout when more than one tab is shared - a list of tabs is shown
    fun verifyShareTabsOverlay(vararg tabsTitles: String) {
        onView(withId(R.id.shared_site_list))
            .check(matches(isDisplayed()))
        for (tabs in tabsTitles) {
            onView(withText(tabs))
                .check(
                    matches(
                        allOf(
                            hasSibling(withId(R.id.share_tab_favicon)),
                            hasSibling(withId(R.id.share_tab_url))
                        )
                    )
                )
        }
    }

    // This function verifies the share layout when a single tab is shared - no tab info shown
    fun verifyShareTabLayout() = assertShareTabLayout()

    // this verifies the Android sharing layout - not customized for sharing tabs
    fun verifyAndroidShareLayout() {
        mDevice.waitNotNull(Until.findObject(By.res("android:id/resolver_list")))
    }

    fun selectAppToShareWith(appName: String) =
        mDevice.findObject(UiSelector().text(appName)).clickAndWaitForNewWindow()

    fun verifySendToDeviceTitle() = assertSendToDeviceTitle()

    fun verifyShareALinkTitle() = assertShareALinkTitle()

    fun verifySharedTabsIntent(text: String, subject: String) {
        Intents.intended(
            allOf(
                IntentMatchers.hasExtra(Intent.EXTRA_TEXT, text),
                IntentMatchers.hasExtra(Intent.EXTRA_SUBJECT, subject)
            )
        )
    }

    class Transition
}

private fun shareTabsLayout() = onView(withResourceName("shareWrapper"))

private fun assertShareTabLayout() =
    shareTabsLayout().check(matches(isDisplayed()))

private fun sendToDeviceTitle() =
    onView(
        allOf(
            withText("SEND TO DEVICE"),
            withResourceName("accountHeaderText")
        )
    )

private fun assertSendToDeviceTitle() = sendToDeviceTitle()
    .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun shareALinkTitle() =
    onView(
        allOf(
            withText("ALL ACTIONS"),
            withResourceName("apps_link_header")
        )
    )

private fun assertShareALinkTitle() = shareALinkTitle()
