/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_login_info.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.fenix.FeatureFlags
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onPause() {
        // If we pause this fragment, we want to pop users back to reauth
        if (findNavController().currentDestination?.id != R.id.savedLoginsFragment) {
            activity?.let { it.checkAndUpdateScreenshotPermission(it.settings()) }
            findNavController().popBackStack(R.id.loginsFragment, false)
        }
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        siteInfoText.text = args.savedLoginItem.url
        usernameInfoText.text = args.savedLoginItem.userName
        passwordInfoText.text = args.savedLoginItem.password

        val copyInfoArgs = listOf(
            Pair(copyWebAddress, args.savedLoginItem.url),
            Pair(copyUsername, args.savedLoginItem.userName),
            Pair(copyPassword, args.savedLoginItem.password)
        )

        for (item in copyInfoArgs) {
            item.first.setOnClickListener(CopyButtonListener(item.second, R.string.login_copied))
        }

        passwordInfoText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        revealPassword.setOnClickListener {
            togglePasswordReveal(it.context)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (FeatureFlags.loginsEdit) {
            inflater.inflate(R.menu.login_options_menu, menu)
        } else {
            inflater.inflate(R.menu.login_delete, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.delete_login_button -> {
            displayDeleteLoginDialog()
            true
        }
        R.id.edit_login_button -> {
            nav(
                R.id.editLoginFragment,
                SavedLoginSiteInfoFragmentDirections
                    .actionSavedLoginsInfoFragmentToEditLoginFragment(args.savedLoginItem)
            )
            true
        }
        else -> false
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

    private fun togglePasswordReveal(context: Context) {
        if (passwordInfoText.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
            context.components.analytics.metrics.track(Event.ViewLoginPassword)
            passwordInfoText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            revealPassword.setImageDrawable(
                getDrawable(
                    context,
                    R.drawable.mozac_ic_password_hide
                )
            )
            revealPassword.contentDescription =
                context.getString(R.string.saved_login_hide_password)
        } else {
            passwordInfoText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            revealPassword.setImageDrawable(
                getDrawable(
                    context,
                    R.drawable.mozac_ic_password_reveal
                )
            )
            revealPassword.contentDescription =
                context.getString(R.string.saved_login_reveal_password)
        }
        // For the new type to take effect you need to reset the text
        passwordInfoText.text = args.savedLoginItem.password
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        showToolbar(args.savedLoginItem.url)
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
