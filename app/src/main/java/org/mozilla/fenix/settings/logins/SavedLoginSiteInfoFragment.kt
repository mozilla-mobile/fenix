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
import kotlinx.android.synthetic.main.fragment_login_info.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.checkAndUpdateScreenshotPermission
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.showToolbar

/**
 * Displays saved login information for a single website.
 */
class SavedLoginSiteInfoFragment : Fragment(R.layout.fragment_login_info) {

    private val args by navArgs<SavedLoginSiteInfoFragmentArgs>()
    private val savedLoginHelper = SavedLoginsHelper(view)

    override fun onResume() {
        super.onResume()
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        showToolbar(args.savedLoginItem.title ?: args.savedLoginItem.url)
        setHasOptionsMenu(true)
    }

    override fun onPause() {
        // If we pause this fragment, we want to pop users back to reauth
        if (findNavController().currentDestination?.id != R.id.savedLoginsFragment &&
            findNavController().currentDestination?.id != R.id.editLoginFragment
        ) {
            activity?.let { it.checkAndUpdateScreenshotPermission(it.settings()) }
            findNavController().popBackStack(R.id.loginsFragment, false)
        }
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webAddressText.text = args.savedLoginItem.url
        usernameText.text = args.savedLoginItem.userName
        passwordInfoText.text = args.savedLoginItem.password

        val copyInfoArgs = listOf(
            Pair(copyWebAddress, args.savedLoginItem.url),
            Pair(copyUsername, args.savedLoginItem.userName),
            Pair(copyPassword, args.savedLoginItem.password)
        )

        for (copyButtons in copyInfoArgs) {
            copyButtons.first.setOnClickListener(CopyButtonListener(copyButtons.second, R.string.login_copied))
        }

        passwordInfoText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        revealPassword.setOnClickListener {
            savedLoginHelper.togglePasswordReveal(it.context, args.savedLoginItem)
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
            SavedLoginSiteInfoFragmentDirections
                .actionSavedLoginsInfoFragmentToEditLoginFragment(args.savedLoginItem)
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

    private fun deleteLogin() {
        var deleteLoginJob: Deferred<Boolean>? = null
        val deleteJob = viewLifecycleOwner.lifecycleScope.launch(IO) {
            deleteLoginJob = async {
                requireContext().components.core.passwordsStorage.delete(args.savedLoginItem.id)
            }
            deleteLoginJob?.await()
            withContext(Main) {
                findNavController().popBackStack(R.id.savedLoginsFragment, false)
            }
        }
        deleteJob.invokeOnCompletion {
            if (it is CancellationException) {
                deleteLoginJob?.cancel()
            }
        }
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
