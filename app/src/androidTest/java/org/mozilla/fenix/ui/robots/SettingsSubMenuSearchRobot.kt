/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers

/**
 * Implementation of Robot Pattern for the settings search sub menu.
 */
class SettingsSubMenuSearchRobot {
    fun verifyDefaultSearchEngineHeader() = assertDefaultSearchEngineHeader()
    fun verifySearchEngineList() = assertSearchEngineList()
    fun verifyShowSearchSuggestions() = assertShowSearchSuggestions()
    fun verifyShowSearchShortcuts() = assertShowSearchShortcuts()
    fun verifyShowClipboardSuggestions() = assertShowClipboardSuggestions()
    fun verifySearchBrowsingHistory() = assertSearchBrowsingHistory()
    fun verifySearchBookmarks() = assertSearchBookmarks()

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

private fun assertDefaultSearchEngineHeader() =
    onView(ViewMatchers.withText("Default search engine"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSearchEngineList() {
    onView(ViewMatchers.withText("Google"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(ViewMatchers.withText("Amazon.com"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(ViewMatchers.withText("Bing"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(ViewMatchers.withText("DuckDuckGo"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(ViewMatchers.withText("Twitter"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(ViewMatchers.withText("Wikipedia"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    onView(ViewMatchers.withText("Add search engine"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertShowSearchSuggestions() {
    onView(withId(androidx.preference.R.id.recycler_view)).perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
            hasDescendant(withText("Show search suggestions"))
        )
    )
    onView(ViewMatchers.withText("Show search suggestions"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertShowSearchShortcuts() {
    onView(withId(androidx.preference.R.id.recycler_view)).perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
            hasDescendant(withText("Show search shortcuts"))
        )
    )
    onView(ViewMatchers.withText("Show search shortcuts"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertShowClipboardSuggestions() {
    onView(withId(androidx.preference.R.id.recycler_view)).perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
            hasDescendant(withText("Show clipboard suggestions"))
        )
    )
    onView(ViewMatchers.withText("Show clipboard suggestions"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertSearchBrowsingHistory() {
    onView(withId(androidx.preference.R.id.recycler_view)).perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
            hasDescendant(withText("Search browsing history"))
        )
    )
    onView(ViewMatchers.withText("Search browsing history"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun assertSearchBookmarks() {
    onView(withId(androidx.preference.R.id.recycler_view)).perform(
        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
            hasDescendant(withText("Search bookmarks"))
        )
    )
    onView(ViewMatchers.withText("Search bookmarks"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))
