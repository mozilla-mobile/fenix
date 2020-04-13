/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_saved_logins.view.*
import kotlinx.android.synthetic.main.component_saved_logins.view.progress_bar
import kotlinx.android.synthetic.main.fragment_edit_login.view.*
import kotlinx.android.synthetic.main.fragment_login_info.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.components.metrics.Event

/**
 * Interface for the SavedLoginsViewInteractor. This interface is implemented by objects that want
 * to respond to user interaction on the SavedLoginsView
 */
interface SavedLoginsViewInteractor {
    /**
     * Called whenever one item is clicked
     */
    fun itemClicked(item: SavedLoginsItem)

    fun onLearnMore()

    fun sort(sortingStrategy: SortingStrategy)
}

/**
 * View that contains and configures the Saved Logins List
 */
class SavedLoginsView(
    override val containerView: ViewGroup,
    val interactor: SavedLoginsInteractor
) : LayoutContainer {

    val view: FrameLayout = LayoutInflater.from(containerView.context)
        .inflate(R.layout.component_saved_logins, containerView, true)
        .findViewById(R.id.saved_logins_wrapper)

    private val loginsAdapter = SavedLoginsAdapter(interactor)

    init {
        view.saved_logins_list.apply {
            adapter = loginsAdapter
            layoutManager = LinearLayoutManager(containerView.context)
            itemAnimator = null
        }

        val learnMoreText = view.saved_passwords_empty_learn_more.text.toString()
        val textWithLink = SpannableString(learnMoreText).apply {
            setSpan(UnderlineSpan(), 0, learnMoreText.length, 0)
        }
        with(view.saved_passwords_empty_learn_more) {
            movementMethod = LinkMovementMethod.getInstance()
            text = textWithLink
            setOnClickListener { interactor.onLearnMore() }
        }

        with(view.saved_passwords_empty_message) {
            val appName = context.getString(R.string.app_name)
            text = context.getString(
                R.string.preferences_passwords_saved_logins_description_empty_text,
                appName
            )
        }
    }

    fun update(state: SavedLoginsFragmentState) {
        if (state.isLoading) {
            view.progress_bar.isVisible = true
        } else {
            view.progress_bar.isVisible = false
            view.saved_logins_list.isVisible = state.items.isNotEmpty()
            view.saved_passwords_empty_view.isVisible = state.items.isEmpty()
        }
        loginsAdapter.submitList(state.filteredItems)
    }
}

open class SavedLoginsHelper(val view: View?, val context: Context?) {
    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    open fun togglePasswordReveal(item: SavedLoginsItem): Boolean =
        when (view?.id) {
            R.id.editLoginFragment -> {
                togglePasswordReveal(
                    item.password,
                    view.passwordText,
                    null,
                    view.revealPasswordButton
                )
                true
            }
            R.id.savedLoginSiteInfoFragment -> {
                togglePasswordReveal(
                    item.password,
                    null,
                    view.passwordInfoText,
                    view.revealPassword
                )
                true
            }
            else -> {false}

    }

    private fun togglePasswordReveal(
        itemPassword: String?,
        passwordEditableTextView: TextInputEditText? = null,
        passwordTextView: TextView? = null,
        revealPasswordButton: ImageButton
    ) {
        if (passwordTextView == null ) {
            if (passwordEditableTextView?.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
                context?.components?.analytics?.metrics?.track(Event.ViewLoginPassword)
                passwordEditableTextView.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                revealPasswordButton.setImageDrawable(
                    context?.resources?.getDrawable(R.drawable.mozac_ic_password_hide, null)
                )
                revealPasswordButton.contentDescription =
                    context?.getString(R.string.saved_login_hide_password)
            } else {
                passwordEditableTextView?.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                revealPasswordButton.setImageDrawable(
                    context?.resources?.getDrawable(R.drawable.mozac_ic_password_reveal, null)
                )
                revealPasswordButton.contentDescription =
                    context?.getString(R.string.saved_login_reveal_password)
            }
            passwordEditableTextView?.text = itemPassword?.toEditable()
        } else {
            if (passwordTextView.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
                context?.components?.analytics?.metrics?.track(Event.ViewLoginPassword)
                passwordTextView.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                revealPasswordButton.setImageDrawable(
                    context?.resources?.getDrawable(R.drawable.mozac_ic_password_hide, null)
                )
                revealPasswordButton.contentDescription =
                    context?.getString(R.string.saved_login_hide_password)
            } else {
                passwordTextView.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                revealPasswordButton.setImageDrawable(
                    context?.resources?.getDrawable(R.drawable.mozac_ic_password_reveal, null)
                )
                revealPasswordButton.contentDescription =
                    context?.getString(R.string.saved_login_reveal_password)
            }
            passwordTextView.text = itemPassword
        }
    }

}