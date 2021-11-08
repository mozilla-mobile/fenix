package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the settings Homepage sub menu.
 */
class SettingsSubMenuHomepageRobot {

    fun verifyHomePageView() {
        assertMostVisitedTopSitesButton()
        assertJumpBackInButton()
        assertRecentBookmarksButton()
        assertRecentSearchesButton()
        assertPocketButton()
        assertOpeningScreenHeading()
        assertHomepageButton()
        assertLastTabButton()
        assertHomepageAfterFourHoursButton()
    }

    fun clickStartOnHomepageButton() = homepageButton().click()

    class Transition {

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            goBackButton().click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }
    }
}

private fun mostVisitedTopSitesButton() =
    mDevice.findObject(UiSelector().textContains("Most visited top sites"))

private fun jumpBackInButton() =
    mDevice.findObject(UiSelector().textContains("Jump back in"))

private fun recentBookmarksButton() =
    mDevice.findObject(UiSelector().textContains("Recent bookmarks"))

private fun recentSearchesButton() =
    mDevice.findObject(UiSelector().textContains("Recent searches"))

private fun pocketButton() =
    mDevice.findObject(UiSelector().textContains("Pocket"))

private fun openingScreenHeading() = onView(withText(R.string.preferences_opening_screen))

private fun homepageButton() =
    onView(
        allOf(
            withId(R.id.title),
            withText(R.string.opening_screen_homepage),
            hasSibling(withId(R.id.radio_button))
        )
    )

private fun lastTabButton() =
    onView(
        allOf(
            withId(R.id.title),
            withText(R.string.opening_screen_last_tab),
            hasSibling(withId(R.id.radio_button))
        )
    )

private fun homepageAfterFourHoursButton() =
    onView(
        allOf(
            withId(R.id.title),
            withText(R.string.opening_screen_after_four_hours_of_inactivity),
            hasSibling(withId(R.id.radio_button))
        )
    )

private fun goBackButton() = mDevice.findObject(UiSelector().descriptionContains("Navigate up"))

private fun assertMostVisitedTopSitesButton() =
    assertTrue(mostVisitedTopSitesButton().waitForExists(waitingTime))
private fun assertJumpBackInButton() =
    assertTrue(jumpBackInButton().waitForExists(waitingTime))
private fun assertRecentBookmarksButton() =
    assertTrue(recentBookmarksButton().waitForExists(waitingTime))
private fun assertRecentSearchesButton() =
    assertTrue(recentSearchesButton().waitForExists(waitingTime))
private fun assertPocketButton() = assertTrue(pocketButton().waitForExists(waitingTime))
private fun assertOpeningScreenHeading() =
    openingScreenHeading().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertHomepageButton() =
    homepageButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertLastTabButton() =
    lastTabButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
private fun assertHomepageAfterFourHoursButton() =
    homepageAfterFourHoursButton().check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
