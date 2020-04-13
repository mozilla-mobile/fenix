/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_edit_login.*
import kotlinx.android.synthetic.main.fragment_edit_login.view.*
import kotlinx.android.synthetic.main.fragment_login_info.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.nav


/**
 * Displays the editable saved login information for a single website.
 */
class EditLoginFragment : Fragment(R.layout.fragment_edit_login) {

    private val args by navArgs<EditLoginFragmentArgs>()
    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
    private val savedLoginHelper = SavedLoginsHelper(view, context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ensure hostname isn't editable
        hostnameText.text = args.savedLoginItem.url.toEditable()
        hostnameText.isClickable = false
        hostnameText.isFocusable = false

        usernameText.text = args.savedLoginItem.userName?.toEditable() ?: "".toEditable()
        passwordText.text = args.savedLoginItem.password!!.toEditable()

        passwordInfoText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        revealPassword.setOnClickListener {
            savedLoginHelper.togglePasswordReveal(args.savedLoginItem)
        }
        setClearTextListeners()
    }

    private fun setupKeyboardFocus(view: View) {
        view.editLoginLayout.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                closeKeyboard()
            }
        }

        view.editLoginLayout.setOnTouchListener { _, _ ->
            closeKeyboard()
            view.clearFocus()
            true
        }
    }

    fun closeKeyboard() {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun setClearTextListeners() {
        clearUsernameTextButton.setOnClickListener {
            usernameText.text = "".toEditable()
        }
        clearPasswordTextButton.setOnClickListener {
            passwordText.text = "".toEditable()
        }
        revealPassword.setOnClickListener {
            savedLoginHelper.togglePasswordReveal(args.savedLoginItem)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_save, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save_login_button -> {
            attemptSaveAndExit()
            true
        }
        else -> false
    }

    private fun attemptSaveAndExit() {
        Log.v("ELISE: SAVE", "ELISE: SAVE")
        val itemToSave = SavedLoginsItem(
            url = hostnameText.text.toString(),
            title = hostnameText.text.toString(),
            userName = usernameText.text.toString(),
            password = passwordText.text.toString(),
            id = args.savedLoginItem.id
        )
        val directions = EditLoginFragmentDirections
            .actionEditLoginFragmentToSavedLoginsInfoFragment(itemToSave)
        findNavController().navigate(directions)
    }
}