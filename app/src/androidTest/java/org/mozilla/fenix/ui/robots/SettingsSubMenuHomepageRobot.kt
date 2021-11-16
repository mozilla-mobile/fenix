package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the settings Homepage sub menu.
 */
class SettingsSubMenuHomepageRobot {

    fun clickStartOnHomepageButton() = homepageButton().click()
    fun clickJumpBackInButton() = jumpBackInButton().click()
    fun clickRecentBookmarksButton() = recentBookmarksButton().click()

    class Transition {

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            goBackButton().click()

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }
    }
}

private fun openingScreenHeading() = onView(withText(R.string.preferences_opening_screen))

private fun homepageButton() =
    onView(
        allOf(
            withId(R.id.title),
            withText(R.string.opening_screen_homepage),
            hasSibling(withId(R.id.radio_button))
        )
    )

private fun lastTabButton() = onView(withText(R.string.opening_screen_last_tab))

private fun homepageAfterFourHoursButton() =
    onView(withText(R.string.opening_screen_after_four_hours_of_inactivity))

private fun jumpBackInButton() =
    onView(allOf(withText(R.string.customize_toggle_jump_back_in)))

private fun recentBookmarksButton() =
    onView(allOf(withText(R.string.customize_toggle_recent_bookmarks)))

private fun goBackButton() = onView(allOf(withContentDescription(R.string.action_bar_up_description)))
