/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.TextKeyListener
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_edit_login.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.android.synthetic.main.fragment_edit_login.inputLayoutPassword
import kotlinx.android.synthetic.main.fragment_edit_login.inputLayoutUsername
import kotlinx.android.synthetic.main.fragment_edit_login.hostnameText
import kotlinx.android.synthetic.main.fragment_edit_login.usernameText
import kotlinx.android.synthetic.main.fragment_edit_login.passwordText
import kotlinx.android.synthetic.main.fragment_edit_login.clearUsernameTextButton
import kotlinx.android.synthetic.main.fragment_edit_login.clearPasswordTextButton
import kotlinx.android.synthetic.main.fragment_edit_login.revealPasswordButton
import kotlinx.coroutines.*
import kotlinx.android.synthetic.main.fragment_edit_login.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.Login
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.service.sync.logins.InvalidRecordException
import mozilla.components.service.sync.logins.LoginsStorageException
import mozilla.components.service.sync.logins.NoSuchRecordException
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.redirectToReAuth
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings

/**
 * Displays the editable saved login information for a single website.
 */
@ExperimentalCoroutinesApi
@Suppress("TooManyFunctions", "NestedBlockDepth", "ForbiddenComment")
class EditLoginFragment : Fragment(R.layout.fragment_edit_login) {

    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    private val args by navArgs<EditLoginFragmentArgs>()
    private lateinit var loginsFragmentStore: LoginsFragmentStore
    private lateinit var datastore: LoginsDataStore

    private lateinit var oldLogin: SavedLogin
    private var listOfPossibleDupes: List<SavedLogin>? = null

    private var usernameChanged = false
    private var passwordChanged = false
    private var saveEnabled = false

    private var validPassword = true
    private var validUsername = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        oldLogin = args.savedLoginItem

        loginsFragmentStore = StoreProvider.get(this) {
            LoginsFragmentStore(
                LoginsListState(
                    isLoading = true,
                    loginList = listOf(),
                    filteredItems = listOf(),
                    searchedForText = null,
                    sortingStrategy = requireContext().settings().savedLoginsSortingStrategy,
                    highlightedItem = requireContext().settings().savedLoginsMenuHighlightedItem,
                    duplicateLogins = listOf()
                )
            )
        }

        datastore = LoginsDataStore(this, loginsFragmentStore)

        // ensure hostname isn't editable
        hostnameText.text = args.savedLoginItem.origin.toEditable()
        hostnameText.isClickable = false
        hostnameText.isFocusable = false

        usernameText.text = args.savedLoginItem.username.toEditable()
        passwordText.text = args.savedLoginItem.password.toEditable()

        usernameText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        // TODO: extend PasswordTransformationMethod() to change bullets to asterisks
        passwordText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordText.compoundDrawablePadding =
            requireContext().resources
                .getDimensionPixelOffset(R.dimen.saved_logins_end_icon_drawable_padding)

        saveEnabled = false // don't enable saving until something has been changed
        val saveButton = activity?.findViewById<ActionMenuItemView>(R.id.save_login_button)
        saveButton?.isEnabled = saveEnabled

        usernameChanged = false
        passwordChanged = false

        datastore.findPotentialDuplicates(args.savedLoginItem.guid)

        setUpClickListeners()
        setUpTextListeners()

        consumeFrom(loginsFragmentStore) {
            listOfPossibleDupes = loginsFragmentStore.state.duplicateLogins
        }
    }

    private fun setUpClickListeners() {
        clearUsernameTextButton.setOnClickListener {
            usernameText.text?.clear()
            usernameText.isCursorVisible = true
            usernameText.hasFocus()
            inputLayoutUsername.hasFocus()
            it.isEnabled = false
        }
        clearPasswordTextButton.setOnClickListener {
            passwordText.text?.clear()
            passwordText.isCursorVisible = true
            passwordText.hasFocus()
            inputLayoutPassword.hasFocus()
            it.isEnabled = false
        }
        revealPasswordButton.setOnClickListener {
            togglePasswordReveal()
        }

        var firstClick = true
        passwordText.setOnClickListener {
            if (firstClick) {
                togglePasswordReveal()
                firstClick = false
            }
        }
    }

    private fun setUpTextListeners() {
        val frag = view?.findViewById<View>(R.id.editLoginFragment)
        frag?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view?.hideKeyboard()
            }
        }

        editLoginLayout.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                view?.hideKeyboard()
            }
        }

        usernameText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(u: Editable?) {
                if (u.toString() == oldLogin.username) {
                    inputLayoutUsername.error = null
                    inputLayoutUsername.errorIconDrawable = null
                } else {
                    clearUsernameTextButton.isEnabled = true
                    // setDupeError() TODO in #10173
                }
            }

            override fun beforeTextChanged(u: CharSequence?, start: Int, count: Int, after: Int) {
                // NOOP
            }

            override fun onTextChanged(u: CharSequence?, start: Int, before: Int, count: Int) {
                // NOOP
            }
        })

        passwordText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p: Editable?) {
                when {
                    p.toString().isEmpty() -> {
                        clearPasswordTextButton.isEnabled = false
                        setPasswordError()
                    }
                    p.toString() == oldLogin.password -> {
                        inputLayoutPassword.error = null
                        inputLayoutPassword.errorIconDrawable = null
                        clearPasswordTextButton.isEnabled = true
                    }
                    else -> {
                        inputLayoutPassword.error = null
                        inputLayoutPassword.errorIconDrawable = null
                        clearPasswordTextButton.isEnabled = true
                    }
                }
            }

            override fun beforeTextChanged(p: CharSequence?, start: Int, count: Int, after: Int) {
                // NOOP
            }

            override fun onTextChanged(p: CharSequence?, start: Int, before: Int, count: Int) {
                // NOOP
            }
        })
    }

    private fun setPasswordError() {
        inputLayoutPassword?.let { layout ->
            layout.error = context?.getString(R.string.saved_login_password_required)
            layout.setErrorIconDrawable(R.drawable.mozac_ic_warning)

            layout.errorIconDrawable?.setTint(
                ContextCompat.getColor(requireContext(), R.color.design_default_color_error)
            )
        }
    }

    private fun setUpTextListeners() {
        val frag = view?.findViewById<View>(R.id.editLoginFragment)
        frag?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view?.hideKeyboard()
            }
        }

        usernameText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (usernameText.text?.toString().equals(oldLogin.username)) {
                    usernameChanged = false
                    validUsername = true
                    inputLayoutUsername.error = null
                } else if (!(usernameText.text?.toString().equals(oldLogin.username))) {
                    usernameChanged = true
                    setDupeError()
                }
                setSaveButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // NOOP
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // NOOP
            }
        })

        passwordText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (passwordText.text?.toString() == "") {
                    passwordChanged = true
                    setPasswordError()
                } else if (passwordText.text?.toString().equals(oldLogin.password)) {
                    passwordChanged = false
                    inputLayoutPassword.error = null
                    validPassword = true
                } else {
                    passwordChanged = true
                    inputLayoutPassword.error = null
                    validPassword = true
                }
                setSaveButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // NOOP
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // NOOP
            }
        })
    }

    private fun isDupe(username: String): Boolean =
        loginsFragmentStore.state.duplicateLogins.filter { it.username == username }.any()

    private fun setDupeError() {
        if (isDupe(usernameText.text.toString())) {
            inputLayoutUsername?.let {
                it.setErrorIconDrawable(R.drawable.mozac_ic_warning)
                it.error = context?.getString(R.string.saved_login_duplicate)
                validUsername = false
            }
        } else {
            inputLayoutUsername.error = null
            passwordChanged = true
            validUsername = true
        }
    }

    private fun setPasswordError() {
        inputLayoutPassword?.let {
            it.setErrorIconDrawable(R.drawable.mozac_ic_warning)
            it.error = context?.getString(R.string.saved_login_password_required)
            validPassword = false
        }
    }

    private fun setSaveButtonState() {
        val saveButton = activity?.findViewById<ActionMenuItemView>(R.id.save_login_button)
        val changesMadeWithNoErrors =
            validUsername && validPassword && (usernameChanged || passwordChanged)

        changesMadeWithNoErrors.let {
            saveButton?.isEnabled = it
            saveEnabled = it
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_save, menu)
    }

    override fun onPause() {
        redirectToReAuth(
            listOf(R.id.loginDetailFragment),
            findNavController().currentDestination?.id
        )
        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save_login_button -> {
            view?.hideKeyboard()
            if (saveEnabled) {
                try {
                    datastore.save(
                        args.savedLoginItem.guid,
                        usernameText.text.toString(),
                        passwordText.text.toString()
                    )
                    requireComponents.analytics.metrics.track(Event.EditLoginSave)
                } catch (exception: Exception) {
                    when (exception) {
                        is NoSuchRecordException,
                        is InvalidRecordException -> {
                            Log.e("Edit login",
                                "Failed to save edited login.", exception)
                        }
                        else -> Log.e("Edit login",
                            "Failed to save edited login with non-LoginStorageException error.", exception)
                    }
                }
            }
            true
        }
        else -> false
    }

    // TODO: create helper class for toggling passwords. Used in login info and edit fragments.
    private fun togglePasswordReveal() {
        val currText = passwordText.text
        if (passwordText.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD
            or InputType.TYPE_CLASS_TEXT
        ) {
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
