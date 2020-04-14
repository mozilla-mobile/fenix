/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_edit_login.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import mozilla.components.concept.storage.Login
import mozilla.components.service.sync.logins.InvalidRecordException
import mozilla.components.service.sync.logins.LoginsStorageException
import mozilla.components.service.sync.logins.NoSuchRecordException
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components

/**
 * Displays the editable saved login information for a single website.
 */
class EditLoginFragment : Fragment(R.layout.fragment_edit_login) {

    private val args by navArgs<EditLoginFragmentArgs>()
    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
    private var saveEnabled: Boolean = true

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
        // TODO: extend PasswordTransformationMethod() to change bullets to asterisks
        passwordText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        setUpClickListeners()
    }

    private fun setUpClickListeners() {
        clearUsernameTextButton.setOnClickListener {
            usernameText.text?.clear()
            usernameText.hasFocus()
            inputLayoutUsername.hasFocus()
            usernameText.isCursorVisible = true
        }
        clearPasswordTextButton.setOnClickListener {
            passwordText.text?.clear()
            passwordText.hasFocus()
            inputLayoutPassword.hasFocus()
            passwordText.isCursorVisible = true
            saveEnabled = false
        }
        revealPasswordButton.setOnClickListener {
            togglePasswordReveal()
        }

        inputLayoutPassword?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                saveEnabled = !passwordText.text.isNullOrEmpty()
            }
        }
    }

    // disable save button when fields are invalid
    override fun onPrepareOptionsMenu(menu: Menu) {
        val saveButton = menu.getItem(0)
        saveButton.isEnabled = saveEnabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            saveButton.iconTintList = AppCompatResources.getColorStateList(context!!, R.color.toggle_save_enabled)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_save, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save_login_button -> {
            try {
                if (saveEnabled) attemptSaveAndExit()
            } catch (loginException: Exception) {
                when (loginException) {
                    is NoSuchRecordException,
                    is InvalidRecordException,
                    is LoginsStorageException -> {
                        Log.e("Edit login", "Failed to save edited login.", loginException)
                    }
                    else -> Log.e("Edit login", "Failed to save edited login.", loginException)
                }
            }
            true
        }
        else -> false
    }

    // TODO: Move interactions with the component's password storage into a separate datastore
    // This includes Delete, Update/Edit, Create
    private fun attemptSaveAndExit() {
        var saveLoginJob: Deferred<Unit>? = null
        lifecycleScope.launch(IO) {
            saveLoginJob = async {
                val oldLogin = requireContext().components.core.passwordsStorage.get(args.savedLoginItem.id)
                // Update requires a Login type, which needs at least one of httpRealm or formActionOrigin
                val loginToSave = Login(
                    guid = oldLogin?.guid,
                    origin = oldLogin?.origin!!,
                    username = usernameText.text.toString(), // new value
                    password = passwordText.text.toString(), // new value
                    httpRealm = oldLogin.httpRealm,
                    formActionOrigin = oldLogin.formActionOrigin
                )
                requireContext().components.core.passwordsStorage.update(loginToSave)
            }
            saveLoginJob?.await()
            withContext(Main) {
                findNavController().popBackStack(R.id.savedLoginSiteInfoFragment, false)
            }
        }
        saveLoginJob?.invokeOnCompletion {
            if (it is CancellationException) {
                saveLoginJob?.cancel()
            }
        }
    }

    fun closeKeyboard() {
        val inputMethodManager =
            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    // TODO: create helper class for toggling passwords. Used in login info and edit fragments.
    private fun togglePasswordReveal() {
        val currText = passwordText.text
        if (passwordText.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
            context?.components?.analytics?.metrics?.track(Event.ViewLoginPassword)
            passwordText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            revealPasswordButton.setImageDrawable(
                resources.getDrawable(R.drawable.mozac_ic_password_hide, null)
            )
            revealPasswordButton.contentDescription =
                resources.getString(R.string.saved_login_hide_password)
        } else {
            passwordText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            revealPasswordButton.setImageDrawable(
                resources.getDrawable(R.drawable.mozac_ic_password_reveal, null)
            )
            revealPasswordButton.contentDescription =
                context?.getString(R.string.saved_login_reveal_password)
        }
        // For the new type to take effect you need to reset the text to it's current edited version
        passwordText?.text = currText
    }
}
