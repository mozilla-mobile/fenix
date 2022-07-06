/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.ext.waitNotNull

class NotificationRobot {

    fun verifySystemNotificationExists(notificationMessage: String) {
        val notification = UiSelector().text(notificationMessage)
        var notificationFound = mDevice.findObject(notification).waitForExists(waitingTime)

        while (!notificationFound) {
            scrollToEnd()
            notificationFound = mDevice.findObject(notification).waitForExists(waitingTime)
        }

        assertTrue(notificationFound)
    }

    fun clearNotifications() {
        if (clearButton.exists()) {
            clearButton.click()
        } else {
            scrollToEnd()
            if (clearButton.exists()) {
                clearButton.click()
            } else if (notificationTray().exists()) {
                mDevice.pressBack()
            }
        }
    }

    fun cancelAllShownNotifications() {
        cancelAll()
    }

    fun verifySystemNotificationGone(notificationMessage: String) {
        mDevice.waitNotNull(
            Until.gone(text(notificationMessage)),
            waitingTime
        )

        assertFalse(
            mDevice.findObject(
                UiSelector().text(notificationMessage)
            ).exists()
        )
    }

    fun verifyPrivateTabsNotification() {
        Log.i("Andi", "verifyPrivateTabsNotification: Waiting for PB notification")
        mDevice.waitNotNull(Until.hasObject(text("Close private tabs")), waitingTime)
        Log.i("Andi", "verifyPrivateTabsNotification: Waited for PB notification")
        assertPrivateTabsNotification()
        Log.i("Andi", "verifyPrivateTabsNotification: Asserted PB notification")
    }

    fun clickMediaNotificationControlButton(action: String) {
        mediaSystemNotificationButton(action).waitForExists(waitingTime)
        mediaSystemNotificationButton(action).click()
    }

    fun clickDownloadNotificationControlButton(action: String) {
        try {
            assertTrue(downloadSystemNotificationButton(action).waitForExists(waitingTimeShort))
        } catch (e: AssertionError) {
            notificationTray().flingToEnd(1)
        }

        downloadSystemNotificationButton(action).click()
        // API 30 Bug? Sometimes a click doesn't register, try again
        try {
            assertTrue(downloadSystemNotificationButton(action).waitUntilGone(waitingTime))
        } catch (e: AssertionError) {
            downloadSystemNotificationButton(action).click()
        }
    }

    fun verifyMediaSystemNotificationButtonState(action: String) {
        assertTrue(mediaSystemNotificationButton(action).waitForExists(waitingTime))
    }

    fun expandNotificationMessage() {
        while (!notificationHeader.exists()) {
            scrollToEnd()
        }

        if (notificationHeader.exists()) {
            // expand the notification
            notificationHeader.click()

            // double check if notification actions are viewable by checking for action existence; otherwise scroll again
            while (!mDevice.findObject(UiSelector().resourceId("android:id/action0")).exists() &&
                !mDevice.findObject(UiSelector().resourceId("android:id/actions_container")).exists()
            ) {
                scrollToEnd()
            }
        }
    }

    class Transition {

        fun clickClosePrivateTabsNotification(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            NotificationRobot().verifySystemNotificationExists("Close private tabs")
            Log.i("Andi", "clickClosePrivateTabsNotification: Verified system PB notification")
            closePrivateTabsNotification().click()
            Log.i("Andi", "clickClosePrivateTabsNotification: Clicked PB notification")

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }
    }
}

fun notificationShade(interact: NotificationRobot.() -> Unit): NotificationRobot.Transition {
    NotificationRobot().interact()
    return NotificationRobot.Transition()
}

private fun assertPrivateTabsNotification() {
    mDevice.findObject(UiSelector().text("Firefox Preview (Private)")).exists()
    Log.i("Andi", "assertPrivateTabsNotification: Firefox Preview (Private) exists")
    mDevice.findObject(UiSelector().text("Close private tabs")).exists()
    Log.i("Andi", "assertPrivateTabsNotification: Close private tabs exists")
}

private fun closePrivateTabsNotification() =
    mDevice.findObject(UiSelector().text("Close private tabs"))

private fun downloadSystemNotificationButton(action: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("android:id/action0")
            .textContains(action)
    )

private fun mediaSystemNotificationButton(action: String) =
    mDevice.findObject(
        UiSelector()
            .resourceId("com.android.systemui:id/action0")
            .descriptionContains(action)
    )

private fun notificationTray() = UiScrollable(
    UiSelector().resourceId("com.android.systemui:id/notification_stack_scroller")
).setAsVerticalList()

private val notificationHeader =
    mDevice.findObject(
        UiSelector()
            .resourceId("android:id/app_name_text")
            .text(appName)
    )

private fun scrollToEnd() {
    notificationTray().scrollToEnd(1)
}

private val clearButton = mDevice.findObject(UiSelector().resourceId("com.android.systemui:id/dismiss_text"))

private fun cancelAll() {
    val notificationManager: NotificationManager =
        TestHelper.appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancelAll()
}
