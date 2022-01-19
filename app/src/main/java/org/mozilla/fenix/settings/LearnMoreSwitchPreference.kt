/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import org.mozilla.fenix.R
import org.mozilla.fenix.utils.LinkTextView

/**
 * Switch preference showing a "Learn More" text below the summary and informing through a callback
 * when this text is clicked by the user.
 */
class LearnMoreSwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchPreference(context, attrs) {

    /**
     * Lazy callback for when the "Learn more" text is clicked by the user.
     */
    var onLearnMoreClicked = { }

    init {
        layoutResource = R.layout.preference_switch_learn_more
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        (holder.findViewById(R.id.learn_more) as LinkTextView).setOnClickListener {
            onLearnMoreClicked()
        }
    }
}
