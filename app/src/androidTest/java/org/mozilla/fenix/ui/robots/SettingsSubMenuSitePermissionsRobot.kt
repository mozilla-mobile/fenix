package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.helpers.click

class SettingsSubMenuSitePermissionsRobot {
    fun verifySitePermissionsItems() {
        assertAutoplayItem()
        assertCameraItem()
        assertLocationItem()
        assertMicrophoneItem()
        assertNotificationItem()
        assertExceptionItem()
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun openExceptionsSubMenu(interact: SettingsSubMenuSitePermissionsExceptionsRobot.() -> Unit): SettingsSubMenuSitePermissionsExceptionsRobot.Transition {
            onView(withText("Exceptions")).click()

            SettingsSubMenuSitePermissionsExceptionsRobot().interact()
            return SettingsSubMenuSitePermissionsExceptionsRobot.Transition()
        }

        fun openCameraSubMenu(interact: SettingsSubMenuSitePermissionsCommonRobot.() -> Unit): SettingsSubMenuSitePermissionsCommonRobot.Transition {
            onView(withText("Camera")).click()

            SettingsSubMenuSitePermissionsCommonRobot().interact()
            return SettingsSubMenuSitePermissionsCommonRobot.Transition()
        }

        fun openLocationSubMenu(interact: SettingsSubMenuSitePermissionsCommonRobot.() -> Unit): SettingsSubMenuSitePermissionsCommonRobot.Transition {
            onView(withText("Location")).click()

            SettingsSubMenuSitePermissionsCommonRobot().interact()
            return SettingsSubMenuSitePermissionsCommonRobot.Transition()
        }

        fun openMicrophoneSubMenu(interact: SettingsSubMenuSitePermissionsCommonRobot.() -> Unit): SettingsSubMenuSitePermissionsCommonRobot.Transition {
            onView(withText("Microphone")).click()

            SettingsSubMenuSitePermissionsCommonRobot().interact()
            return SettingsSubMenuSitePermissionsCommonRobot.Transition()
        }

        fun openNotificationSubMenu(interact: SettingsSubMenuSitePermissionsCommonRobot.() -> Unit): SettingsSubMenuSitePermissionsCommonRobot.Transition {
            onView(withText("Notification")).click()

            SettingsSubMenuSitePermissionsCommonRobot().interact()
            return SettingsSubMenuSitePermissionsCommonRobot.Transition()
        }
    }
}

private fun goBackButton() =
    Espresso.onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))

private fun assertAutoplayItem() = onView(withText("Autoplay")).check(
    matches(
        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
    )
)

private fun assertCameraItem() = onView(withText("Camera")).check(
    matches(
        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
    )
)

private fun assertLocationItem() = onView(withText("Location")).check(
    matches(
        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
    )
)

private fun assertMicrophoneItem() = onView(withText("Microphone")).check(
    matches(
        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
    )
)

private fun assertNotificationItem() = onView(withText("Notification")).check(
    matches(
        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
    )
)

private fun assertExceptionItem() = onView(withText("Exceptions")).check(
    matches(
        withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
    )
)