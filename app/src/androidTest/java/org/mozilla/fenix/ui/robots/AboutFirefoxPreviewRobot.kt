package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.CoreMatchers.containsString
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.withPattern
import org.mozilla.fenix.ui.robots.AboutFirefoxPreviewRobot.Companion.buildAndVersionPattern
import org.mozilla.fenix.ui.robots.AboutFirefoxPreviewRobot.Companion.datePattern

class AboutFirefoxPreviewRobot {
    fun verifyFirefoxPreviewPage() = assertFirefoxPreviewPage()

    class Transition {
        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }

    companion object {
        const val datePattern = "\\w+\\s\\d{1,2}\\/\\d{1,2}\\s\\@\\s\\d{1,2}\\:\\d{1,2}\\s[A|P][M]"
        const val buildAndVersionPattern =
            "(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)\\s\\((Build)\\s\\#\\d+\\)\\n([^\\x20-\\x7E]+)\\:\\s" +
                    "(\\d+\\.)?(\\d+\\.)?(\\*|\\d+),\\s\\w+\\n([^\\x20-\\x7E]+|GV):\\s(\\d+\\.)?" +
                    "(\\d+\\-)?(\\d+)"
    }
}

private fun assertFirefoxPreviewPage() {
    assertBuildAndVersionNumber()
    assertProductCompany()
    assertCurrentTimestamp()
    assertOpenSourcedLibraries()
}

private fun assertBuildAndVersionNumber() {
    onView(withId(R.id.about_text))
        .check(matches(withPattern(buildAndVersionPattern)))
}

private fun assertProductCompany() {
    onView(withId(R.id.about_content))
        .check(matches(withText(containsString("is produced by Mozilla."))))
}

private fun assertCurrentTimestamp() {
    onView(withId(R.id.build_date))
        .check(matches(withPattern(datePattern)))
}

private fun assertOpenSourcedLibraries() {
    val view = onView(withId(R.id.view_licenses_button))
    view.check(matches(withText(containsString("Open source libraries we use"))))
    view.check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    view.perform(click())

    onView(withId(R.id.action_bar)).check(matches(hasDescendant(withText(containsString("Firefox Preview")))))
}

private fun goBackButton() = onView(withContentDescription("Navigate up"))

