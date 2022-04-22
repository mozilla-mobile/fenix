/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.address

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import org.mozilla.fenix.R
import org.mozilla.fenix.SecureFragment
import org.mozilla.fenix.databinding.FragmentAddressEditorBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.address.controller.DefaultAddressEditorController
import org.mozilla.fenix.settings.address.interactor.AddressEditorInteractor
import org.mozilla.fenix.settings.address.interactor.DefaultAddressEditorInteractor
import org.mozilla.fenix.settings.address.view.AddressEditorView

/**
 * Displays an address editor for adding and editing an address.
 */
class AddressEditorFragment : SecureFragment(R.layout.fragment_address_editor) {

    private lateinit var addressEditorView: AddressEditorView
    private lateinit var interactor: AddressEditorInteractor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val storage = requireContext().components.core.autofillStorage

        interactor = DefaultAddressEditorInteractor(
            controller = DefaultAddressEditorController(
                storage = storage,
                lifecycleScope = lifecycleScope,
                navController = findNavController()
            )
        )

        val binding = FragmentAddressEditorBinding.bind(view)

        addressEditorView = AddressEditorView(binding, interactor)
        addressEditorView.bind()
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.addresses_add_address))
    }
}
