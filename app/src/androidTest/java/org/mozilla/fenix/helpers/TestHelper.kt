/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.helpers

import android.net.Uri
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.By
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.ext.waitNotNull

object TestHelper {
    fun scrollToElementByText(text: String): UiScrollable {
        val appView = UiScrollable(UiSelector().scrollable(true))
        appView.scrollTextIntoView(text)
        return appView
    }

    fun longTapSelectItem(url: Uri) {
        Espresso.onView(ViewMatchers.withText(url.toString())).perform(ViewActions.longClick())
    }
    fun verifyToolbarUrl(redirectUrl: String) {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        mDevice.waitNotNull(Until.findObject(By.res("org.mozilla.fenix.debug:id/mozac_browser_toolbar_url_view")), TestAssetHelper.waitingTime)
        Espresso.onView(ViewMatchers.withId(R.id.mozac_browser_toolbar_url_view))
            .check(
                ViewAssertions.matches(
                    ViewMatchers.withText(
                        CoreMatchers.containsString(
                            redirectUrl
                        )
                    )
                )
            )
    }

    fun clickGoBackButton() = Espresso.onView(ViewMatchers.withContentDescription("Navigate up")).click()
}
