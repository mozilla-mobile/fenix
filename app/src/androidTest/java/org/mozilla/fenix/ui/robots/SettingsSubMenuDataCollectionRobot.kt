package org.mozilla.fenix.ui.robots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.helpers.click
import org.junit.Assert.assertTrue
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.ext.waitNotNull

class SettingsSubMenuDataCollectionRobot {
    fun verifyHeaderUsageAndTechnicalData() = assertHeaderUsageAndTechnicalData()
    fun verifyDescUsageAndTechnicalData() = assertDescUsageAndTechnicalData()
    fun verifyUsageDataToggleDefault() = assertUsageDataToggleDefault()
    fun verifyHeaderMarketingData() = assertHeaderMarketingData()
    fun verifyDescMarketingData() = assertDescMarketingData()
    fun verifyMarketingDataToggleDefault() = assertMarketingDataToggleDefault()

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitNotNull(
                Until.findObject(By.text("Settings")),
                TestAssetHelper.waitingTime
            )
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))

private fun assertHeaderUsageAndTechnicalData() = onView(withText("Usage and technical data"))
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertHeaderMarketingData() = onView(withText("Marketing data"))
    .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertDescUsageAndTechnicalData() =
    onView(withText("Shares performance, usage, hardware and customization data about your browser with Mozilla to help us make Firefox Preview better"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertDescMarketingData() =
    onView(withText("Shares data about what features you use in Firefox Preview with Leanplum, our mobile marketing vendor."))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertUsageDataToggleDefault() {
    val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    assertTrue(getSwitchItemFromRow(mDevice, 0).isChecked)
}

private fun assertMarketingDataToggleDefault() {
    val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    assertTrue(getSwitchItemFromRow(mDevice, 1).isChecked)
}

private fun getSwitchItemFromRow(device: UiDevice, index: Int): UiObject {
    val recyclerView =
        device.findObject(UiSelector().resourceId("org.mozilla.fenix.debug:id/recycler_view"))
    val row = recyclerView.getChild(UiSelector().index(index))

    return row.getChild(UiSelector().resourceId("android:id/switch_widget"))
}
