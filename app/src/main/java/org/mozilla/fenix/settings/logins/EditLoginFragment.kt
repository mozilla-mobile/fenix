/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.view.*
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_edit_login.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_login_info.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar
import org.webrtc.Logging
import org.webrtc.Logging.log

/**
 * Displays the editable saved login information for a single website.
 */
class EditLoginFragment : Fragment(R.layout.fragment_edit_login) {

    private val args by navArgs<EditLoginFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        showToolbar(args.savedLoginItem.title ?: args.savedLoginItem.url)
    }

    override fun onPause() {
        // If we pause this fragment, do we want users to:
        // reauth? keep editing without reauth? go back to login detail?
        if (findNavController().currentDestination?.id != R.id.savedLoginsFragment) {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            findNavController().popBackStack(R.id.savedLoginSiteInfoFragment, false)
        }
        super.onPause()
    }

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeIcon = activity?.resources?.getDrawable(R.drawable.ic_close, null)
        view.toolbar.setCompoundDrawables(closeIcon, null, null, null)

        // ensure hostname isn't editable
        hostnameText.text = args.savedLoginItem.url.toEditable()
        hostnameText.isClickable = false
        hostnameText.isFocusable = false

        usernameText.text = args.savedLoginItem.userName?.toEditable() ?: "".toEditable()
        passwordText.text = args.savedLoginItem.password!!.toEditable()

//        clearUsernameTextButton.setOnClickListener {
//            usernameText.text = "".toEditable()
//        }
//        clearPasswordTextButton.setOnClickListener {
//            passwordText.text = "".toEditable()
//        }
//        revealPassword.setOnClickListener {
//            togglePasswordReveal(it.context)
//        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_save, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.save_login_button -> {
            attemptSaveAndExit()
            true
        }
        R.id.toolbar -> {
            discardChangesAndExit()
            true
        }
        else -> false
    }

    private fun discardChangesAndExit() {
        log(Logging.Severity.LS_INFO, "ELISE: DISCARD CHANGES", "")
    }

    private fun attemptSaveAndExit() {
        log(Logging.Severity.LS_INFO, "ELISE: SAVE", "")
    }

    private fun deleteLogin() {
        var deleteLoginJob: Deferred<Boolean>? = null
        val deleteJob = lifecycleScope.launch(IO) {
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
}
