/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.view

import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_edit_login.*
import kotlinx.android.synthetic.main.fragment_edit_login.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.logins.LoginsListState
import org.mozilla.fenix.settings.logins.SavedLogin
import org.mozilla.fenix.settings.logins.interactor.EditLoginInteractor

/**
 * View that contains and configures the Edit Login screen
 */
class EditLoginView(
    override val containerView: ViewGroup,
    val interactor: EditLoginInteractor
) : LayoutContainer {

    private val context = containerView.context
    private fun String.toEditable(): Editable =
        Editable.Factory.getInstance().newEditable(this)

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



    fun update(login: LoginsListState) {
        containerView.hostnameText.text = login.currentItem?.origin?.toEditable()
        containerView.usernameText.text = login.currentItem?.username?.toEditable()
        containerView.passwordText.text = login.currentItem?.password?.toEditable()
    }
}
