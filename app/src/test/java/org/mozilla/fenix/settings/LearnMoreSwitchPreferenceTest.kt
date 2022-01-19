/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.widget.FrameLayout
import androidx.preference.PreferenceViewHolder
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.utils.LinkTextView

@RunWith(FenixRobolectricTestRunner::class)
class LearnMoreSwitchPreferenceTest {
    @Test
    fun `WHEN the preference is created THEN it sets a default layout resource`() {
        val preference = LearnMoreSwitchPreference(testContext)

        assertEquals(R.layout.preference_switch_learn_more, preference.layoutResource)
    }

    @Test
    fun `WHEN the learn_more text is clicked THEN inform the onLearnMoreClicked callback`() {
        val preference = LearnMoreSwitchPreference(testContext)
        val learnMore = LinkTextView(testContext).apply { id = R.id.learn_more }
        val layout = FrameLayout(testContext).apply { addView(learnMore) }
        val viewHolder = PreferenceViewHolder.createInstanceForTests(layout)
        var wasLearnMoreClicked = false
        preference.onLearnMoreClicked = {
            wasLearnMoreClicked = true
        }
        preference.onBindViewHolder(viewHolder)

        learnMore.callOnClick()

        assertTrue(wasLearnMoreClicked)
    }
}
