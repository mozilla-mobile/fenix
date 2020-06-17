/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.view

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_edit_login.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.logins.interactor.EditLoginInteractor

/**
 * View that contains and configures the Edit Login screen
 */
class EditLoginView(
    val container: ViewGroup,
    val interactor: EditLoginInteractor
) : LayoutContainer {

    private val context = container.context

    override val containerView: View = LayoutInflater.from(context)
        .inflate(R.layout.fragment_edit_login, container, true)

    init {
        containerView.editLoginLayout.apply {
            // ensure hostname isn't editable
            this.hostnameText.isClickable = false
            this.hostnameText.isFocusable = false

            this.usernameText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

            // TODO: extend PasswordTransformationMethod() to change bullets to asterisks
            this.passwordText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            this.passwordText.compoundDrawablePadding =
                context.resources
                    .getDimensionPixelOffset(R.dimen.saved_logins_end_icon_drawable_padding)

            togglePasswordReveal()
        }

        with(containerView.clearUsernameTextButton) {
            setOnClickListener {
                usernameText.text?.clear()
                usernameText.isCursorVisible = true
                usernameText.hasFocus()
                inputLayoutUsername.hasFocus()
                it.isEnabled = false
            }
        }

        with(containerView.clearPasswordTextButton) {
            setOnClickListener {
                passwordText.text?.clear()
                passwordText.isCursorVisible = true
                passwordText.hasFocus()
                inputLayoutPassword.hasFocus()
                it.isEnabled = false
            }
        }

        with(containerView.revealPasswordButton) {
            setOnClickListener {
                togglePasswordReveal()
            }
        }
    }

    // TODO: create helper class for toggling passwords. Used in login info and edit fragments.
    private fun togglePasswordReveal() {
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
