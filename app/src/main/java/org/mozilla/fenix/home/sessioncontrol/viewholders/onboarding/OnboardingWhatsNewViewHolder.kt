/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding

import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.onboarding_whats_new.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.settings.SupportUtils

class OnboardingWhatsNewViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    init {
        view.header_text.setOnboardingIcon(R.drawable.ic_whats_new)

        val appName = view.context.getString(R.string.app_name)
        view.description_text.text = view.context.getString(R.string.onboarding_whats_new_description, appName)

        val getAnswersText = view.get_answers.text.toString()
        val textWithLink = SpannableString(getAnswersText).apply {
            setSpan(UnderlineSpan(), 0, getAnswersText.length, 0)
        }

        view.get_answers.text = textWithLink
        view.get_answers.setOnClickListener {
            val intent = SupportUtils.createCustomTabIntent(view.context, SupportUtils.getWhatsNewUrl(view.context))
            view.context.startActivity(intent)
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.onboarding_whats_new
    }
}
