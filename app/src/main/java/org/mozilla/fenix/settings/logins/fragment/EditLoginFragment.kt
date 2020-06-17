/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_edit_login.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.service.sync.logins.InvalidRecordException
import mozilla.components.service.sync.logins.NoSuchRecordException
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.redirectToReAuth
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.settings.logins.LoginsFragmentStore
import org.mozilla.fenix.settings.logins.LoginsListState
import org.mozilla.fenix.settings.logins.SavedLogin
import org.mozilla.fenix.settings.logins.controller.SavedLoginsStorageController
import org.mozilla.fenix.settings.logins.interactor.EditLoginInteractor
import org.mozilla.fenix.settings.logins.view.EditLoginView

/**
 * Displays the editable saved login information for a single website
 */
@ExperimentalCoroutinesApi
@Suppress("TooManyFunctions", "NestedBlockDepth", "ForbiddenComment")
class EditLoginFragment : Fragment() {

    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    private val args by navArgs<EditLoginFragmentArgs>()
    private lateinit var loginsFragmentStore: LoginsFragmentStore
    private lateinit var interactor: EditLoginInteractor
    private lateinit var editLoginView: EditLoginView

    private lateinit var oldLogin: SavedLogin
    private var listOfPossibleDupes: List<SavedLogin>? = null

    private var usernameChanged = false
    private var passwordChanged = false
    private var saveEnabled = false

    private var validPassword = true
    private var validUsername = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        oldLogin = args.savedLoginItem
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_login, container, false)

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

        interactor = EditLoginInteractor(
            SavedLoginsStorageController(
                context = requireContext(),
                navController = findNavController(),
                loginsFragmentStore = loginsFragmentStore
            )
        )
        editLoginView = EditLoginView(view.editLoginLayout, interactor)

        view.hostnameText.text = args.savedLoginItem.origin.toEditable()
        view.usernameText.text = args.savedLoginItem.username.toEditable()
        view.passwordText.text = args.savedLoginItem.password.toEditable()

        saveEnabled = false // don't enable saving until something has been changed
        val saveButton =
            activity?.findViewById<ActionMenuItemView>(R.id.save_login_button)
        saveButton?.isEnabled = saveEnabled

        usernameChanged = false
        passwordChanged = false

        interactor.findPotentialDuplicates(args.savedLoginItem.guid)

        setUpTextListeners()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        consumeFrom(loginsFragmentStore) {
            listOfPossibleDupes = loginsFragmentStore.state.duplicateLogins
        }
    }

    override fun onPause() {
        redirectToReAuth(
            listOf(R.id.loginDetailFragment),
            findNavController().currentDestination?.id
        )
        super.onPause()
    }

    private fun setUpTextListeners() {
        val frag = view?.findViewById<View>(R.id.editLoginFragment)
        frag?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view?.hideKeyboard()
            }
        }
        view?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                view?.hideKeyboard()
            }
        }

        view?.usernameText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(u: Editable?) {
                when {
                    u.toString() == oldLogin.username -> {
                        usernameChanged = false
                        validUsername = true
                        view?.inputLayoutUsername?.error = null
                        view?.inputLayoutUsername?.errorIconDrawable = null
                    }
                    else -> {
                        usernameChanged = true
                        view?.clearUsernameTextButton?.isEnabled = true
                        setDupeError()
                    }
                }
                setSaveButtonState()
            }

            override fun beforeTextChanged(u: CharSequence?, start: Int, count: Int, after: Int) {
                // NOOP
            }

            override fun onTextChanged(u: CharSequence?, start: Int, before: Int, count: Int) {
                // NOOP
            }
        })

        view?.passwordText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p: Editable?) {
                when {
                    p.toString().isEmpty() -> {
                        passwordChanged = true
                        view?.clearPasswordTextButton?.isEnabled = false
                        setPasswordError()
                    }
                    p.toString() == oldLogin.password -> {
                        passwordChanged = false
                        validPassword = true
                        view?.inputLayoutPassword?.error = null
                        view?.inputLayoutPassword?.errorIconDrawable = null
                        view?.clearPasswordTextButton?.isEnabled = true
                    }
                    else -> {
                        passwordChanged = true
                        validPassword = true
                        view?.inputLayoutPassword?.error = null
                        view?.inputLayoutPassword?.errorIconDrawable = null
                        view?.clearPasswordTextButton?.isEnabled = true
                    }
                }
                setSaveButtonState()
            }

            override fun beforeTextChanged(p: CharSequence?, start: Int, count: Int, after: Int) {
                // NOOP
            }

            override fun onTextChanged(p: CharSequence?, start: Int, before: Int, count: Int) {
                // NOOP
            }
        })
    }


    private fun isDupe(username: String): Boolean =
        loginsFragmentStore.state.duplicateLogins.filter { it.username == username }.any()

    private fun setDupeError() {
        if (isDupe(view?.usernameText?.text.toString())) {
            view?.inputLayoutUsername?.let {
                usernameChanged = true
                validUsername = false
                it.setErrorIconDrawable(R.drawable.mozac_ic_warning)
                it.error = context?.getString(R.string.saved_login_duplicate)
            }
        } else {
            usernameChanged = true
            validUsername = true
            view?.inputLayoutUsername?.error = null
        }
    }

    private fun setPasswordError() {
        view?.inputLayoutPassword?.let { layout ->
            validPassword = false
            layout.error = context?.getString(R.string.saved_login_password_required)
            layout.setErrorIconDrawable(R.drawable.mozac_ic_warning)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save_login_button -> {
            view?.hideKeyboard()
            if (saveEnabled) {
                try {
                    interactor.saveLogin(
                        args.savedLoginItem.guid,
                        view?.usernameText?.text.toString(),
                        view?.passwordText?.text.toString()
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


}
