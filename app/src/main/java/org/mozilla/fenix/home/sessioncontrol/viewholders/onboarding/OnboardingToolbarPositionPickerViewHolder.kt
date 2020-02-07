/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.onboarding_toolbar_position_picker.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.asActivity
import org.mozilla.fenix.ext.settings

class OnboardingToolbarPositionPickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    init {
        val radioTopToolbar = view.toolbar_top_radio_button
        val radioBottomToolbar = view.toolbar_bottom_radio_button

        radioTopToolbar.addToRadioGroup(radioBottomToolbar)
        radioBottomToolbar.addToRadioGroup(radioTopToolbar)

        with(view.context.settings()) {
            val radio = when {
                this.shouldUseBottomToolbar -> radioBottomToolbar
                else -> radioTopToolbar
            }
            radio.isChecked = true
        }

        radioBottomToolbar.onClickListener {
            itemView.context.asActivity()?.recreate()
        }

        view.toolbar_bottom_image.setOnClickListener {
            radioBottomToolbar.performClick()
        }

        radioTopToolbar.onClickListener {
            itemView.context.asActivity()?.recreate()
        }

        view.toolbar_top_image.setOnClickListener {
            radioTopToolbar.performClick()
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.onboarding_toolbar_position_picker
    }
}
