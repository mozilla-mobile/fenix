/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.Matchers
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for the search fragment.
 */
class SearchRobot {
    fun verifySearchView() = assertSearchView()
    fun verifyBrowserToolbar() = assertBrowserToolbarEditView()
    fun verifyScanButton() = assertScanButton()
    fun verifySearchWithText() = assertSearchWithText()
    fun verifySearchEngineResults(searchEngineName: String) =
        assertSearchEngineResults(searchEngineName)

    fun verifySearchEngineURL(searchEngineName: String) = assertSearchEngineURL(searchEngineName)
    fun verifySearchSettings() = assertSearchSettings()
    fun verifySearchBarEmpty() = assertSearchBarEmpty()

    fun clickScanButton() {
        scanButton().perform(click())
    }

    fun clickDenyPermission() {
        denyPermissionButton().click()
    }

    fun clickAllowPermission() {
        allowPermissionButton().click()
    }

    fun typeSearch(searchTerm: String) {
        browserToolbarEditView().perform(typeText(searchTerm))
    }

    fun clickSearchEngineButton(searchEngineName: String) {
        searchEngineButton(searchEngineName).perform(click())
    }

    fun clickSearchEngineResult(searchEngineName: String) {
        mDevice.waitNotNull(
            Until.findObjects(By.text(searchEngineName)),
            TestAssetHelper.waitingTime
        )
        awesomeBar().perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                click()
            )
        )
    }

    fun scrollToSearchEngineSettings() {
        // Soft keyboard is visible on screen on view access; hide it
        onView(allOf(withId(R.id.search_layout))).perform(
            closeSoftKeyboard()
        )
        onView(allOf(withId(R.id.awesomeBar))).perform(ViewActions.swipeUp())
    }

    fun clickSearchEngineSettings() {
        onView(withText("Search engine settings")).perform(click())
    }

    fun clickClearButton() {
        clearButton().perform(click())
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun openBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitForIdle()
            browserToolbarEditView().perform(typeText("mozilla\n"))

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

private fun awesomeBar() = onView(withId(R.id.awesomeBar))

private fun browserToolbarEditView() =
    onView(Matchers.allOf(withId(R.id.mozac_browser_toolbar_edit_url_view)))

private fun searchEngineButton(searchEngineName: String): ViewInteraction {
    mDevice.waitNotNull(Until.findObject(By.text(searchEngineName)), TestAssetHelper.waitingTime)
    return onView(Matchers.allOf(withText(searchEngineName)))
}

private fun denyPermissionButton(): UiObject {
    mDevice.waitNotNull(Until.findObjects(By.text("Deny")), TestAssetHelper.waitingTime)
    return mDevice.findObject(UiSelector().text("Deny"))
}

private fun allowPermissionButton(): UiObject {
    mDevice.waitNotNull(Until.findObjects(By.text("Allow")), TestAssetHelper.waitingTime)
    return mDevice.findObject(UiSelector().text("Allow"))
}

private fun scanButton(): ViewInteraction {
    mDevice.waitNotNull(Until.findObject(By.res("org.mozilla.fenix.debug:id/search_scan_button")), TestAssetHelper.waitingTime)
    return onView(allOf(withId(R.id.search_scan_button)))
}

private fun clearButton() = onView(withId(R.id.mozac_browser_toolbar_clear_view))

private fun assertSearchEngineURL(searchEngineName: String) {
    mDevice.waitNotNull(
        Until.findObject(By.textContains("https://${searchEngineName.toLowerCase()}.com/?q=mozilla")),
        TestAssetHelper.waitingTime
    )
    onView(allOf(withText(startsWith("https://${searchEngineName.toLowerCase()}.com"))))
        .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertSearchEngineResults(searchEngineName: String) {
    val count =
        mDevice.wait(Until.findObjects(By.text((searchEngineName))), TestAssetHelper.waitingTime)
    assert(count.size > 1)
}

private fun assertSearchView() {
    onView(allOf(withId(R.id.search_layout)))
}

private fun assertBrowserToolbarEditView() =
    onView(Matchers.allOf(withId(R.id.mozac_browser_toolbar_edit_url_view)))
        .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertScanButton() =
    onView(allOf(withText("Scan")))
        .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSearchWithText() =
    onView(allOf(withText("THIS TIME, SEARCH WITH:")))
        .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSearchSettings() =
    onView(allOf(withText("Default search engine")))
        .check(matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSearchBarEmpty() = browserToolbarEditView().check(matches(withText("")))

fun searchScreen(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
    SearchRobot().interact()
    return SearchRobot.Transition()
}

private fun goBackButton() = onView(allOf(withContentDescription("Navigate up")))
