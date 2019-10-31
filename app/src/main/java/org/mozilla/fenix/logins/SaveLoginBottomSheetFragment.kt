/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.logins

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_save_login.*
import kotlinx.android.synthetic.main.fragment_save_login.view.*
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components

class SaveLoginBottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) as FrameLayout
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_save_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO set host, username, and password in dialog views
        view.save_message.text =
            getString(R.string.logins_doorhanger_save, getString(R.string.app_name))

        view.save_confirm.setOnClickListener {
            lifecycleScope.launch {
                context?.components?.core?.loginsStorage?.withUnlocked {
                    val userName = username_field.text
                    val password = password_field.text
                    Log.v("SaveLogin", "username $userName password $password")
                    // TODO Save login here
                }
            }.invokeOnCompletion {
                if (!findNavController().popBackStack(R.id.settingsFragment, false)) {
                    dismiss()
                }
            }
        }

        view.save_cancel.setOnClickListener {
            // TODO add logins exception to never save on this website
            dismiss()
        }
    }
}
