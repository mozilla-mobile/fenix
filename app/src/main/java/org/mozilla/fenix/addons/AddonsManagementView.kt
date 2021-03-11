/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import androidx.navigation.NavController
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.AddonsManagerAdapterDelegate
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.navigateBlockingForAsyncNavGraph
import org.mozilla.fenix.ext.navigateSafe

/**
 * View used for managing add-ons.
 */
class AddonsManagementView(
    private val navController: NavController,
    private val showPermissionDialog: (Addon) -> Unit
) : AddonsManagerAdapterDelegate {

    override fun onAddonItemClicked(addon: Addon) {
        if (addon.isInstalled()) {
            showInstalledAddonDetailsFragment(addon)
        } else {
            showDetailsFragment(addon)
        }
    }

    override fun onInstallAddonButtonClicked(addon: Addon) {
        showPermissionDialog(addon)
    }

    override fun onNotYetSupportedSectionClicked(unsupportedAddons: List<Addon>) {
        showNotYetSupportedAddonFragment(unsupportedAddons)
    }

    private fun showInstalledAddonDetailsFragment(addon: Addon) {
        val directions =
            AddonsManagementFragmentDirections.actionAddonsManagementFragmentToInstalledAddonDetails(
                addon
            )
        navController.navigateSafe(R.id.addonsManagementFragment, directions)
    }

    private fun showDetailsFragment(addon: Addon) {
        val directions =
            AddonsManagementFragmentDirections.actionAddonsManagementFragmentToAddonDetailsFragment(
                addon
            )
        navController.navigateSafe(R.id.addonsManagementFragment, directions)
    }

    private fun showNotYetSupportedAddonFragment(unsupportedAddons: List<Addon>) {
        val directions =
            AddonsManagementFragmentDirections.actionAddonsManagementFragmentToNotYetSupportedAddonFragment(
                unsupportedAddons.toTypedArray()
            )
        navController.navigateBlockingForAsyncNavGraph(directions)
    }
}
