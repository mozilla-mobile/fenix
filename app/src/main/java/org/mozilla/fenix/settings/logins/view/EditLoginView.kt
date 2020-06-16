/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.view

import android.text.InputType
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_edit_login.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components

/**
 * View that contains and configures the Edit Login screen
 */
@Suppress("ForbiddenComment")
class EditLoginView(
    override val containerView: ViewGroup
) : LayoutContainer {
    private val context = containerView.context

    // TODO: create helper class for toggling passwords. Used in login info and edit fragments.
    fun togglePasswordReveal() {
        val currText = containerView.passwordText?.text
        if (containerView.passwordText?.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD
            or InputType.TYPE_CLASS_TEXT
        ) {
            context.components.analytics.metrics.track(Event.ViewLoginPassword)
            containerView.passwordText?.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            containerView.revealPasswordButton?.setImageDrawable(
                context.resources.getDrawable(R.drawable.mozac_ic_password_hide, null)
            )
            containerView.revealPasswordButton?.contentDescription =
                context.resources.getString(R.string.saved_login_hide_password)
        } else {
            containerView.passwordText?.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            containerView.revealPasswordButton?.setImageDrawable(
                context.resources.getDrawable(R.drawable.mozac_ic_password_reveal, null)
            )
            containerView.revealPasswordButton?.contentDescription =
                context.getString(R.string.saved_login_reveal_password)
        }
        // For the new type to take effect you need to reset the text to it's current edited version
        containerView.passwordText?.text = currText
    }
}
