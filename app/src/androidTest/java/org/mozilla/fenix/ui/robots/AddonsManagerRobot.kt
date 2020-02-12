package org.mozilla.fenix.ui.robots

import android.widget.RelativeLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.ext.waitNotNull

class AddonsManagerRobot {
//uBlock Origin
    fun verifyAddonPrompt(addonName: String) = assertAddonPrompt(addonName)
    fun clickInstallAddon(addonName: String) = installAddon(addonName)
    fun verifyDownloadAddonPrompt() = assertDownloadingAddonPrompt()
    fun cancelInstallAddon() = cancelInstall()
    fun acceptInstallAddon() = allowInstall()
    class Transition {

    }

    private fun installButtonForAddon(addonName: String) =
        onView(
            allOf(
                withContentDescription(R.string.mozac_feature_addons_install_addon_content_description),
                isDescendantOfA(withId(R.id.add_on_item)),
                hasSibling(withChild(withText(addonName)))
            )
        )

    private fun installAddon(addonName: String) {
        installButtonForAddon(addonName)
            .check(matches(isCompletelyDisplayed()))
            .perform(click())
    }

    private fun assertAddonIsEnabled(addonName: String) {
        installButtonForAddon(addonName)
            .check(matches(not(isCompletelyDisplayed())))
    }

    private fun assertAddonPrompt(addonName: String) {
        onView(allOf(withId(R.id.title), withText("Add $addonName?")))
            .check(matches(isCompletelyDisplayed()))

        onView(
            allOf(
                withId(R.id.permissions),
                withText(containsString("It requires your permission to:"))
            )
        )
            .check(matches(isCompletelyDisplayed()))

        onView(allOf(withId(R.id.allow_button), withText("Add")))
            .check(matches(isCompletelyDisplayed()))

        onView(allOf(withId(R.id.deny_button), withText("Cancel")))
            .check(matches(isCompletelyDisplayed()))
    }

    private fun assertDownloadingAddonPrompt() {

        mDevice.waitNotNull(
            Until.findObject(By.textContains("Successfully installed")),
            waitingTime
        )
    }

    private fun cancelInstall() {
        onView(allOf(withId(R.id.deny_button), withText("Cancel")))
            .check(matches(isCompletelyDisplayed()))
            .perform(click())
    }

    private fun allowInstall() {
        onView(allOf(withId(R.id.allow_button), withText("Add")))
            .check(matches(isCompletelyDisplayed()))
            .perform(click())
    }

}
