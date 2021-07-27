/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.fragment

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.databinding.FragmentAddNewLoginBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.ext.toEditable
import org.mozilla.fenix.settings.logins.LoginsFragmentStore
import org.mozilla.fenix.settings.logins.controller.SavedLoginsStorageController
import org.mozilla.fenix.settings.logins.createInitialLoginsListState
import org.mozilla.fenix.settings.logins.interactor.EditLoginInteractor

/**
 * Displays the editable new login information for a single website
 */
class AddNewLoginFragment : Fragment(R.layout.fragment_add_new_login) {

    private lateinit var loginsFragmentStore: LoginsFragmentStore
    private lateinit var interactor: EditLoginInteractor

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

        interactor = EditLoginInteractor(
            SavedLoginsStorageController(
                passwordsStorage = requireContext().components.core.passwordsStorage,
                lifecycleScope = lifecycleScope,
                navController = findNavController(),
                loginsFragmentStore = loginsFragmentStore
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_save, menu)
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
            true
        }
        else -> false
    }
}
