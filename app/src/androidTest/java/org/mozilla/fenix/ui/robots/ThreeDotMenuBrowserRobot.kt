/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.syncintegration.awesomeBar

/**
 * Implementation of Robot Pattern for the Browser three dot menu.
 */
class ThreeDotMenuBrowserRobot {

    class Transition {
        private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun clickSettings(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            settingsButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun clickYourLibrary(interact: LibraryRobot.() -> Unit): LibraryRobot.Transition {
            yourLibraryButton().click()

            LibraryRobot().interact()
            return LibraryRobot.Transition()
        }

        fun clickFindInPage(interact: FindInPageRobot.() -> Unit): FindInPageRobot.Transition {
            findInPageButton().click()

            FindInPageRobot().interact()
            return FindInPageRobot.Transition()
        }

        fun clickOpenInPrivateTab(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            privateTabButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickOpenInNewTab(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            openInNewTabButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun openNewTabAndEnterToBrowser(url: Uri, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.textContains("New tab")), waitingTime)
            openInNewTabButton().click()

            awesomeBar().perform(replaceText(url.toString()), pressImeActionButton())

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun refreshPage(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitNotNull(Until.findObject(By.desc("Refresh")), waitingTime)
            refreshButton().click()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

private fun awesomeBar() = onView(ViewMatchers.withId(R.id.mozac_browser_toolbar_edit_url_view))

private fun settingsButton() = onView(withText("Settings"))

private fun yourLibraryButton() = onView(withText("Your Library"))

private fun findInPageButton() = onView(withText("Find in page"))

private fun openInNewTabButton() = onView(withText("New tab"))

private fun privateTabButton() = onView(withText("Open in private tab"))

private fun saveToCollectionButton() = onView(withText("Save to Collection"))

private fun bookmarkButton() = onView(withText("Bookmark"))

private fun shareButton() = onView(withText("Share"))

private fun refreshButton() = onView(withContentDescription("Refresh"))
