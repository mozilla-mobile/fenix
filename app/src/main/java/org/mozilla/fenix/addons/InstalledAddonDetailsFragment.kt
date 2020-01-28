/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.translate
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar
import mozilla.components.feature.addons.ui.translatedName

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
        bind(view)
    }

    private fun bind(view: View) {
        val title = addon.translatableName.translate()
        showToolbar(title)

        bindEnableSwitch(view)
        bindSettings(view)
        bindDetails(view)
        bindPermissions(view)
        bindRemoveButton(view)
    }

    private fun bindEnableSwitch(view: View) {
        val switch = view.findViewById<Switch>(R.id.enable_switch)
        switch.setState(addon.isEnabled())
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requireContext().components.addonManager.enableAddon(
                    addon,
                    onSuccess = {
                        showSnackBar(
                            view,
                            getString(R.string.mozac_feature_addons_successfully_enabled, addon.translatedName)
                        )
                    },
                    onError = {
                        showSnackBar(
                            view,
                            getString(R.string.mozac_feature_addons_failed_to_enable, addon.translatedName)
                        )
                    }
                )
            } else {
                requireContext().components.addonManager.disableAddon(
                    addon,
                    onSuccess = {
                        showSnackBar(
                            view,
                            getString(R.string.mozac_feature_addons_successfully_disabled, addon.translatedName)
                        )
                    },
                    onError = {
                        showSnackBar(
                            view,
                            getString(R.string.mozac_feature_addons_failed_to_disable, addon.translatedName)
                        )
                    }
                )
            }
        }
    }

    private fun bindSettings(view: View) {
        val settingsView = view.findViewById<View>(R.id.settings)
        settingsView.isEnabled = addon.installedState?.optionsPageUrl != null
        settingsView.setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonInternalSettingsFragment(
                    addon
                )
            Navigation.findNavController(settingsView!!).navigate(directions)
        }
    }

    private fun bindDetails(view: View) {
        view.findViewById<View>(R.id.details).setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonDetailsFragment(
                    addon
                )
            Navigation.findNavController(view).navigate(directions)
        }
    }

    private fun bindPermissions(view: View) {
        view.findViewById<View>(R.id.permissions).setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonPermissionsDetailsFragment(
                    addon
                )
            Navigation.findNavController(view).navigate(directions)
        }
    }

    private fun bindRemoveButton(view: View) {
        view.findViewById<View>(R.id.remove_add_on).setOnClickListener {
            requireContext().components.addonManager.uninstallAddon(
                addon,
                onSuccess = {
                    showSnackBar(
                        view,
                        getString(R.string.mozac_feature_addons_successfully_uninstalled, addon.translatedName)
                    )
                    view.findNavController().popBackStack()
                },
                onError = { _, _ ->
                    showSnackBar(
                        view,
                        getString(
                            R.string.mozac_feature_addons_failed_to_uninstall,
                            addon.translatedName
                        )
                    )
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
