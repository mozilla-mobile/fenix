/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.helpers

import android.view.View
import android.widget.TextView
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Description
import java.util.regex.Pattern

object TestHelper {
    fun scrollToElementByText(text: String): UiScrollable {
        val appView = UiScrollable(UiSelector().scrollable(true))
        appView.scrollTextIntoView(text)
        return appView
    }
}

class RegexMatcherForTextView(private val regex: String) : BoundedMatcher<View, TextView>(TextView::class.java) {
    private val pattern = Pattern.compile(regex)

    override fun describeTo(description: Description?) {
        description?.appendText("Checking the matcher on received view: with pattern=$regex")
    }

    override fun matchesSafely(item: TextView?): Boolean {
        return item?.text?.let {
            pattern.matcher(it).matches()
        } ?: false
    }
}
