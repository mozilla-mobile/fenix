/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.databinding.FragmentAddNewLoginBinding
import org.mozilla.fenix.ext.*
import org.mozilla.fenix.settings.logins.*
import org.mozilla.fenix.settings.logins.controller.SavedLoginsStorageController
import org.mozilla.fenix.settings.logins.interactor.AddLoginInteractor

/**
 * Displays the editable new login information for a single website
 */
@ExperimentalCoroutinesApi
class AddNewLoginFragment : Fragment(R.layout.fragment_add_new_login) {

    private lateinit var loginsFragmentStore: LoginsFragmentStore
    private lateinit var interactor: AddLoginInteractor

    private var listOfPossibleDupes: List<SavedLogin>? = null

    private var validPassword = true
    private var validUsername = true

    private var _binding: FragmentAddNewLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        _binding = FragmentAddNewLoginBinding.bind(view)

        // initialize editable values
        binding.hostnameText.text = "".toEditable()
        binding.usernameText.text = "".toEditable()
        binding.passwordText.text = "".toEditable()

        loginsFragmentStore = StoreProvider.get(this) {
            LoginsFragmentStore(
                createInitialLoginsListState(requireContext().settings())
            )
        }

        interactor = AddLoginInteractor(
            SavedLoginsStorageController(
                passwordsStorage = requireContext().components.core.passwordsStorage,
                lifecycleScope = lifecycleScope,
                navController = findNavController(),
                loginsFragmentStore = loginsFragmentStore
            )
        )

        formatEditableValues()
        setUpClickListeners()
        setUpTextListeners()

        consumeFrom(loginsFragmentStore) {
            listOfPossibleDupes = loginsFragmentStore.state.duplicateLogins
        }
    }

    private fun formatEditableValues() {
        binding.usernameText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        // TODO: extend PasswordTransformationMethod() to change bullets to asterisks
        binding.passwordText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.passwordText.compoundDrawablePadding =
            requireContext().resources
                .getDimensionPixelOffset(R.dimen.saved_logins_end_icon_drawable_padding)
    }

    private fun setUpClickListeners() {
        binding.clearUsernameTextButton.setOnClickListener {
            binding.usernameText.text?.clear()
            binding.usernameText.isCursorVisible = true
            binding.usernameText.hasFocus()
            binding.inputLayoutUsername.hasFocus()
            it.isEnabled = false
        }
        binding.clearPasswordTextButton.setOnClickListener {
            binding.passwordText.text?.clear()
            binding.passwordText.isCursorVisible = true
            binding.passwordText.hasFocus()
            binding.inputLayoutPassword.hasFocus()
        }
    }

    private fun setUpTextListeners() {
        val frag = view?.findViewById<View>(R.id.addNewLoginFragment)
        frag?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view?.hideKeyboard()
            }
        }
        binding.addNewLoginLayout.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                view?.hideKeyboard()
            }
        }

        binding.usernameText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(u: Editable?) {
                setDupeError()

                binding.clearUsernameTextButton.isEnabled = u.toString().isNotEmpty()
                setSaveButtonState()
            }

            override fun beforeTextChanged(u: CharSequence?, start: Int, count: Int, after: Int) {
                // NOOP
            }

            override fun onTextChanged(u: CharSequence?, start: Int, before: Int, count: Int) {
                // NOOP
            }
        })

        binding.passwordText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p: Editable?) {
                when {
                    p.toString().isEmpty() -> {
                        binding.clearPasswordTextButton.isVisible = false
                        setPasswordError()
                    }
                    else -> {
                        validPassword = true
                        binding.inputLayoutPassword.error = null
                        binding.inputLayoutPassword.errorIconDrawable = null
                        binding.clearPasswordTextButton.isVisible = true
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
        if (isDupe(binding.usernameText.text.toString())) {
            binding.inputLayoutUsername.let {
                validUsername = false
                it.error = context?.getString(R.string.saved_login_duplicate)
                it.setErrorIconDrawable(R.drawable.mozac_ic_warning_with_bottom_padding)
                it.setErrorIconTintList(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.design_error)
                    )
                )
                binding.clearUsernameTextButton.isVisible = false
            }
        } else {
            validUsername = true
            binding.inputLayoutUsername.error = null
            binding.inputLayoutUsername.errorIconDrawable = null
            binding.clearUsernameTextButton.isVisible = true
        }
    }

    private fun setPasswordError() {
        binding.inputLayoutPassword.let { layout ->
            validPassword = false
            layout.error = context?.getString(R.string.saved_login_password_required)
            layout.setErrorIconDrawable(R.drawable.mozac_ic_warning_with_bottom_padding)
            layout.setErrorIconTintList(
                ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.design_error)
                )
            )
        }
    }

    private fun setSaveButtonState() {
        activity?.invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_save, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val saveButton = menu.findItem(R.id.save_login_button)
        val changesMadeWithNoErrors = validUsername && validPassword
        saveButton.isEnabled = changesMadeWithNoErrors
    }

    override fun onPause() {
        redirectToReAuth(
            listOf(R.id.loginDetailFragment, R.id.savedLoginsFragment),
            findNavController().currentDestination?.id,
            R.id.editLoginFragment
        )
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.add_login))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save_login_button -> {
            view?.hideKeyboard()
            interactor.onAddNewLogin(
                binding.hostnameText.text.toString(),
                binding.usernameText.text.toString(),
                binding.passwordText.text.toString()
            )
            // requireComponents.analytics.metrics.track(Event.AddLoginSave)
            true
        }
        else -> false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
