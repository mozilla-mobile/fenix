package org.mozilla.fenix.ui

import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.ui.robots.homeScreen

/**
 *  Tests for verifying the about app fragment
 *
 */
class AboutMenuTest {

    @get:Rule
    val activityTestRule = HomeActivityTestRule()

    @Test
    fun aboutFragmentTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openAboutMenu {
            verifyAboutView()
        }
    }

    @Test
    fun aboutFragmentLinksTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openAboutMenu {
            verifyAboutFragmentLinks()
        }
    }

}
