/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import mozilla.components.feature.addons.Addon
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar

/**
 * An activity to show the details of a installed add-on.
 */
class InstalledAddonDetailsFragment : Fragment() {
    private val addon: Addon by lazy {
        AddonDetailsFragmentArgs.fromBundle(requireNotNull(arguments)).addon
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_installed_add_on_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(addon, view)
    }

    private fun bind(addon: Addon, view: View) {
        val title = addon.translatableName.translate()
        showToolbar(title)

        bindEnableSwitch(addon, view)
        bindSettings(addon, view)
        bindDetails(addon, view)
        bindPermissions(addon, view)
        bindRemoveButton(addon, view)
    }

    private fun bindEnableSwitch(addon: Addon, view: View) {
        val switch = view.findViewById<Switch>(R.id.enable_switch)
        switch.setState(addon.isEnabled())
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requireContext().components.addonManager.enableAddon(
                    addon,
                    onSuccess = {
                        Toast.makeText(
                            requireContext(),
                            "Successfully enabled ${addon.translatableName.translate()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = {
                        Toast.makeText(
                            requireContext(),
                            "Failed to enabled ${addon.translatableName.translate()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } else {
                requireContext().components.addonManager.disableAddon(
                    addon,
                    onSuccess = {
                        Toast.makeText(
                            requireContext(),
                            "Successfully disabled ${addon.translatableName.translate()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = {
                        Toast.makeText(
                            requireContext(),
                            "Failed to disabled ${addon.translatableName.translate()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun bindSettings(addOn: Addon, view: View) {
        val settingsView = view.findViewById<View>(R.id.settings)
        settingsView.isEnabled = addOn.installedState?.optionsPageUrl != null
        settingsView.setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonInternalSettingsFragment(
                    addOn
                )
            Navigation.findNavController(settingsView!!).navigate(directions)
        }
    }

    private fun bindDetails(addon: Addon, view: View) {
        view.findViewById<View>(R.id.details).setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonDetailsFragment(
                    addon
                )
            Navigation.findNavController(view).navigate(directions)
        }
    }

    private fun bindPermissions(addon: Addon, view: View) {
        view.findViewById<View>(R.id.permissions).setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonPermissionsDetailsFragment(
                    addon
                )
            Navigation.findNavController(view).navigate(directions)
        }
    }

    private fun bindRemoveButton(addon: Addon, view: View) {
        view.findViewById<View>(R.id.remove_add_on).setOnClickListener {
            requireContext().components.addonManager.uninstallAddon(
                addon,
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        "Successfully uninstalled ${addon.translatableName.translate()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    view.findNavController().popBackStack()
                },
                onError = { _, _ ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to uninstall ${addon.translatableName.translate()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun Switch.setState(checked: Boolean) {
        val text = if (checked) {
            R.string.mozac_feature_addons_settings_on
        } else {
            R.string.mozac_feature_addons_settings_off
        }
        setText(text)
        isChecked = checked
    }
}
