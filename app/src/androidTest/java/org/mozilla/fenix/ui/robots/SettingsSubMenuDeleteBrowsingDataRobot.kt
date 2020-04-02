package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.isChecked

/**
 * Implementation of Robot Pattern for the delete browsing data  option.
 */

class SettingsSubMenuDeleteBrowsingDataRobot {

    fun verifyDeleteBrowsingDataDefaults() = assertDeleteBrowsingDataDefaults()
    fun tapOnDeleteBrowsingData() = deleteBrowsingDataButton().click()

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            goBackButton().click()
            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }

    private fun assertDeleteBrowsingDataDefaults(){
        Espresso.onView(ViewMatchers.withResourceName("switch_widget")).check(
            ViewAssertions.matches(
                isChecked(
                    true
                )
            )
        )
    }
}
private fun goBackButton() =
    Espresso.onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))

private fun deleteBrowsingDataButton() = Espresso.onView(ViewMatchers.withText("Delete browsing data"))
