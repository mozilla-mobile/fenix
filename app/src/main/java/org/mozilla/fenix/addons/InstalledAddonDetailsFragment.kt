/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_installed_add_on_details.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.AddonManagerException
import mozilla.components.feature.addons.ui.translatedName
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.utils.Settings

/**
 * An activity to show the details of a installed add-on.
 */
@Suppress("LargeClass", "TooManyFunctions")
class InstalledAddonDetailsFragment : Fragment() {

    private lateinit var addonManager: AddonManager
    private lateinit var addon: Addon

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::addon.isInitialized) {
            addon = AddonDetailsFragmentArgs.fromBundle(requireArguments()).addon
        }

        return inflater.inflate(R.layout.fragment_installed_add_on_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        addonManager = view.context.components.addonManager

        bindUI()
        bindAddon()
    }

    private fun bindAddon() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            try {
                withContext(IO) {
                    val addons = addonManager.getAddons()
                    addons.find { it.id == addon.id }
                }.let {
                    if (it != null) {
                        addon = it
                        bindUI()
                    } else {
                        throw AddonManagerException(Exception("Addon ${addon.id} not found"))
                    }

                    add_on_progress_bar.isVisible = false
                    addon_container.isVisible = true
                }
            } catch (e: AddonManagerException) {
                showSnackBar(
                    requireView(),
                    getString(R.string.mozac_feature_addons_failed_to_query_add_ons)
                )
                findNavController().popBackStack()
            }
        }
    }

    private fun bindUI() {
        showToolbar(title = addon.translatedName)

        bindEnableSwitch()
        bindSettings()
        bindDetails()
        bindPermissions()
        bindAllowInPrivateBrowsingSwitch()
        bindRemoveButton()
    }

    @SuppressWarnings("LongMethod")
    private fun bindEnableSwitch() {
        val switch = enable_switch
        val privateBrowsingSwitch = allow_in_private_browsing_switch
        switch.setState(addon.isEnabled())
        switch.setOnCheckedChangeListener { v, isChecked ->
            val addonManager = v.context.components.addonManager
            switch.isClickable = false
            remove_add_on.isEnabled = false
            if (isChecked) {
                addonManager.enableAddon(
                    addon,
                    onSuccess = {
                        runIfFragmentIsAttached {
                            this.addon = it
                            switch.isClickable = true
                            privateBrowsingSwitch.isVisible = it.isEnabled()
                            privateBrowsingSwitch.isChecked = it.isAllowedInPrivateBrowsing()
                            switch.setText(R.string.mozac_feature_addons_enabled)
                            settings.isVisible = shouldSettingsBeVisible()
                            remove_add_on.isEnabled = true
                            showSnackBar(
                                requireView(),
                                getString(
                                    R.string.mozac_feature_addons_successfully_enabled,
                                    addon.translatedName
                                )
                            )
                        }
                    },
                    onError = {
                        runIfFragmentIsAttached {
                            switch.isClickable = true
                            remove_add_on.isEnabled = true
                            switch.setState(addon.isEnabled())
                            showSnackBar(
                                requireView(),
                                getString(
                                    R.string.mozac_feature_addons_failed_to_enable,
                                    addon.translatedName
                                )
                            )
                        }
                    }
                )
            } else {
                settings.isVisible = false
                addonManager.disableAddon(
                    addon,
                    onSuccess = {
                        runIfFragmentIsAttached {
                            this.addon = it
                            switch.isClickable = true
                            privateBrowsingSwitch.isVisible = it.isEnabled()
                            switch.setText(R.string.mozac_feature_addons_disabled)
                            remove_add_on.isEnabled = true
                            showSnackBar(
                                requireView(),
                                getString(
                                    R.string.mozac_feature_addons_successfully_disabled,
                                    addon.translatedName
                                )
                            )
                        }
                    },
                    onError = {
                        runIfFragmentIsAttached {
                            switch.isClickable = true
                            privateBrowsingSwitch.isClickable = true
                            remove_add_on.isEnabled = true
                            switch.setState(addon.isEnabled())
                            showSnackBar(
                                requireView(),
                                getString(
                                    R.string.mozac_feature_addons_failed_to_disable,
                                    addon.translatedName
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    private fun bindSettings() {
        settings.isVisible = shouldSettingsBeVisible()
        settings.setOnClickListener {
            val settingUrl = addon.installedState?.optionsPageUrl ?: return@setOnClickListener
            val directions = if (addon.installedState?.openOptionsPageInTab == true) {
                val components = it.context.components

                val shouldCreatePrivateSession =
                    components.core.sessionManager.selectedSession?.private
                        ?: it.context.settings().openLinksInAPrivateTab

                if (shouldCreatePrivateSession) {
                    components.useCases.tabsUseCases.addPrivateTab(settingUrl)
                } else {
                    components.useCases.tabsUseCases.addTab(settingUrl)
                }

                InstalledAddonDetailsFragmentDirections
                    .actionGlobalBrowser(null, false)
            } else {
                InstalledAddonDetailsFragmentDirections
                    .actionInstalledAddonFragmentToAddonInternalSettingsFragment(addon)
            }
            findNavController().navigate(directions)
        }
    }

    private fun bindDetails() {
        details.setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonDetailsFragment(
                    addon
                )
            findNavController().navigate(directions)
        }
    }

    private fun bindPermissions() {
        permissions.setOnClickListener {
            val directions =
                InstalledAddonDetailsFragmentDirections.actionInstalledAddonFragmentToAddonPermissionsDetailsFragment(
                    addon
                )
            findNavController().navigate(directions)
        }
    }

    private fun bindAllowInPrivateBrowsingSwitch() {
        val switch = allow_in_private_browsing_switch
        switch.isChecked = addon.isAllowedInPrivateBrowsing()
        switch.isVisible = addon.isEnabled()
        switch.setOnCheckedChangeListener { _, isChecked ->
            switch.isClickable = false
            remove_add_on.isEnabled = false
            addonManager.setAddonAllowedInPrivateBrowsing(
                addon,
                isChecked,
                onSuccess = {
                    runIfFragmentIsAttached {
                        this.addon = it
                        switch.isClickable = true
                        remove_add_on.isEnabled = true
                    }
                },
                onError = {
                    runIfFragmentIsAttached {
                        switch.isChecked = addon.isAllowedInPrivateBrowsing()
                        switch.isClickable = true
                        remove_add_on.isEnabled = true
                    }
                }
            )
        }
    }
    private fun bindRemoveButton() {
        remove_add_on.setOnClickListener {
            setAllInteractiveViewsClickable(false)
            requireContext().components.addonManager.uninstallAddon(
                addon,
                onSuccess = {
                    runIfFragmentIsAttached {
                        setAllInteractiveViewsClickable(true)
                        showSnackBar(
                            requireView(),
                            getString(
                                R.string.mozac_feature_addons_successfully_uninstalled,
                                addon.translatedName
                            )
                        )
                        findNavController().popBackStack()
                    }
                },
                onError = { _, _ ->
                    runIfFragmentIsAttached {
                        setAllInteractiveViewsClickable(true)
                        showSnackBar(
                            requireView(),
                            getString(
                                R.string.mozac_feature_addons_failed_to_uninstall,
                                addon.translatedName
                            )
                        )
                    }
                }
            )
        }
    }

    private fun setAllInteractiveViewsClickable(clickable: Boolean) {
        enable_switch.isClickable = clickable
        settings.isClickable = clickable
        details.isClickable = clickable
        permissions.isClickable = clickable
        remove_add_on.isClickable = clickable
    }

    private fun Switch.setState(checked: Boolean) {
        val text = if (checked) {
            R.string.mozac_feature_addons_enabled
        } else {
            R.string.mozac_feature_addons_disabled
        }
        setText(text)
        isChecked = checked
    }

    private fun shouldSettingsBeVisible() = !addon.installedState?.optionsPageUrl.isNullOrEmpty()
}
