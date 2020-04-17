/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_add_on_internal_settings.*
import mozilla.components.feature.addons.ui.translatedName
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.showToolbar

/**
 * A fragment to show the internal settings of an add-on.
 */
class AddonInternalSettingsFragment : AddonPopupBaseFragment() {

    private val args by navArgs<AddonInternalSettingsFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        initializeSession()
        return inflater.inflate(R.layout.fragment_add_on_internal_settings, container, false)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(args.addon.translatedName)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args.addon.installedState?.optionsPageUrl?.let {
            engineSession?.let { engineSession ->
                addonSettingsEngineView.render(engineSession)
                engineSession.loadUrl(it)
            }
        } ?: findNavController().navigateUp()
    }
}
