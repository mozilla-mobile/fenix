/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.view

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_edit_login.*
import kotlinx.android.synthetic.main.fragment_edit_login.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.asActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.logins.interactor.EditLoginInteractor

/**
 * View that contains and configures the Edit Login screen
 */
open class EditLoginView(
    override val containerView: ViewGroup,
    val interactor: EditLoginInteractor
) : LayoutContainer {

    protected val context: Context inline get() = containerView.context
    protected val activity = context.asActivity()

    val view: ConstraintLayout = LayoutInflater.from(containerView.context)
        .inflate(R.layout.fragment_edit_login, containerView, true)
        .findViewById(R.id.editLoginFragment)

    init {
        view.editLoginLayout.apply {
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

        with(view.clearUsernameTextButton) {
            setOnClickListener {
                usernameText.text?.clear()
                usernameText.isCursorVisible = true
                usernameText.hasFocus()
                inputLayoutUsername.hasFocus()
                it.isEnabled = false
            }
        }

        with(view.clearPasswordTextButton) {
            setOnClickListener {
                passwordText.text?.clear()
                passwordText.isCursorVisible = true
                passwordText.hasFocus()
                inputLayoutPassword.hasFocus()
                it.isEnabled = false
            }
        }

        with(view.revealPasswordButton) {
            setOnClickListener {
                togglePasswordReveal()
            }
        }

//        with(view.saved_passwords_empty_message) {
//            val appName = context.getString(R.string.app_name)
//            text = String.format(
//                context.getString(
//                    R.string.preferences_passwords_saved_logins_description_empty_text
//                ), appName
//            )
//        }
    }

    private fun setUpClickListeners() {
        clearUsernameTextButton.setOnClickListener {

        }
        clearPasswordTextButton.setOnClickListener {

        }
        revealPasswordButton.setOnClickListener {
        }

        var firstClick = true
        passwordText.setOnClickListener {
            if (firstClick) {
                togglePasswordReveal()
                firstClick = false
            }
        }
    }

    // TODO: create helper class for toggling passwords. Used in login info and edit fragments.
    private fun togglePasswordReveal() {
        val currText = passwordText.text
        if (passwordText.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD
            or InputType.TYPE_CLASS_TEXT
        ) {
            context.components.analytics.metrics.track(Event.ViewLoginPassword)
            passwordText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            revealPasswordButton.setImageDrawable(
                context.resources.getDrawable(R.drawable.mozac_ic_password_hide, null)
            )
            revealPasswordButton.contentDescription =
                context.resources.getString(R.string.saved_login_hide_password)
        } else {
            passwordText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            revealPasswordButton.setImageDrawable(
                context.resources.getDrawable(R.drawable.mozac_ic_password_reveal, null)
            )
            revealPasswordButton.contentDescription =
                context.getString(R.string.saved_login_reveal_password)
        }
        // For the new type to take effect you need to reset the text to it's current edited version
        passwordText?.text = currText
    }

    fun update() {
//        if (state.duplicateLogins) {
//            view.progress_bar.isVisible = true
//        } else {
//            view.progress_bar.isVisible = false
//            view.saved_logins_list.isVisible = state.loginList.isNotEmpty()
//            view.saved_passwords_empty_view.isVisible = state.loginList.isEmpty()
//        }
//        loginsAdapter.submitList(state.filteredItems)
    }
}
