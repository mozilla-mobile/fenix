/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_login_detail.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import mozilla.components.concept.storage.Login
import mozilla.components.service.sync.logins.LoginsStorageException
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.checkAndUpdateScreenshotPermission
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar

/**
 * Displays saved login information for a single website.
 */
class LoginDetailFragment : Fragment(R.layout.fragment_login_detail) {

    private val args by navArgs<LoginDetailFragmentArgs>()
    private lateinit var login: SavedLogin

    override fun onResume() {
        super.onResume()
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    override fun onPause() {
        // If we pause this fragment, we want to pop users back to reauth
        if (findNavController().currentDestination?.id != R.id.loginsListFragment &&
            findNavController().currentDestination?.id != R.id.editLoginFragment
        ) {
            activity?.let { it.checkAndUpdateScreenshotPermission(it.settings()) }
            findNavController().popBackStack(R.id.loginsFragment, false)
        }
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchLoginDetails()

        showToolbar(login.origin)
        setHasOptionsMenu(true)

        webAddressText.text = login.origin
        usernameText.text = login.username
        passwordInfoText.text = login.password

        val copyInfoArgs = listOf(
            Pair(copyWebAddress, login.origin),
            Pair(copyUsername, login.username),
            Pair(copyPassword, login.password)
        )

        for (copyButtons in copyInfoArgs) {
            copyButtons.first.setOnClickListener(CopyButtonListener(copyButtons.second, R.string.login_copied))
        }

        passwordInfoText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        revealPasswordButton.setOnClickListener {
            togglePasswordReveal()
        }
    }

    private fun fetchLoginDetails() {
        var fetchedLogin: Deferred<Login?>? = null
        val fetchLoginJob = lifecycleScope.launch(IO) {
            fetchedLogin = async {
                requireContext().components.core.passwordsStorage.get(args.savedLoginId)
            }
            login = fetchedLogin?.await()?.mapToSavedLogin()
                ?: throw LoginsStorageException(
                    "Login with id ${args.savedLoginId} not able to be fetched."
                )
        }
        fetchLoginJob.invokeOnCompletion {
            if (it is CancellationException) {
                fetchedLogin?.cancel()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_options_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.delete_login_button -> {
            displayDeleteLoginDialog()
            true
        }
        R.id.edit_login_button -> {
            editLogin()
            true
        }
        else -> false
    }

    private fun editLogin() {
        val directions =
            LoginDetailFragmentDirections
                .actionLoginDetailFragmentToEditLoginFragment(login)
        requireView().findNavController().navigate(directions)
    }

    private fun displayDeleteLoginDialog() {
        activity?.let { activity ->
            AlertDialog.Builder(activity).apply {
                setMessage(R.string.login_deletion_confirmation)
                setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
                setPositiveButton(R.string.dialog_delete_positive) { dialog: DialogInterface, _ ->
                    deleteLogin()
                    dialog.dismiss()
                }
                create()
            }.show()
        }
    }

    // TODO: Move interactions with the component's password storage into a separate datastore
    // This includes Delete, Update/Edit, Create
    private fun deleteLogin() {
        var deleteLoginJob: Deferred<Boolean>? = null
        val deleteJob = viewLifecycleOwner.lifecycleScope.launch(IO) {
            deleteLoginJob = async {
                requireContext().components.core.passwordsStorage.delete(args.savedLoginId)
            }
            deleteLoginJob?.await()
            withContext(Main) {
                findNavController().popBackStack(R.id.loginsListFragment, false)
            }
        }
        deleteJob.invokeOnCompletion {
            if (it is CancellationException) {
                deleteLoginJob?.cancel()
            }
        }
    }

    // TODO: create helper class for toggling passwords. Used in login info and edit fragments.
    private fun togglePasswordReveal() {
        if (passwordInfoText.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
            context?.components?.analytics?.metrics?.track(Event.ViewLoginPassword)
            passwordInfoText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            revealPasswordButton.setImageDrawable(
                resources.getDrawable(R.drawable.mozac_ic_password_hide, null)
            )
            revealPasswordButton.contentDescription =
                resources.getString(R.string.saved_login_hide_password)
        } else {
            passwordInfoText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            revealPasswordButton.setImageDrawable(
                resources.getDrawable(R.drawable.mozac_ic_password_reveal, null)
            )
            revealPasswordButton.contentDescription =
                context?.getString(R.string.saved_login_reveal_password)
        }
        // For the new type to take effect you need to reset the text
        passwordInfoText.text = login.password
    }

    /**
     * Click listener for a textview's copy button.
     * @param value Value to be copied
     * @param snackbarText Text to display in snackbar after copying.
     */
    private inner class CopyButtonListener(
        private val value: String?,
        @StringRes private val snackbarText: Int
    ) : View.OnClickListener {
        override fun onClick(view: View) {
            val clipboard = view.context.components.clipboardHandler
            clipboard.text = value
            showCopiedSnackbar(view.context.getString(snackbarText))
            view.context.components.analytics.metrics.track(Event.CopyLogin)
        }

        private fun showCopiedSnackbar(copiedItem: String) {
            view?.let {
                FenixSnackbar.make(
                    view = it,
                    duration = Snackbar.LENGTH_SHORT,
                    isDisplayedWithBrowserToolbar = false
                ).setText(copiedItem).show()
            }
        }
    }
}
