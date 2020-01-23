/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_add_ons_management.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManagerException
import mozilla.components.feature.addons.ui.AddonsManagerAdapter
import mozilla.components.feature.addons.ui.AddonsManagerAdapterDelegate
import mozilla.components.feature.addons.ui.PermissionsDialogFragment
import mozilla.components.feature.addons.ui.translate
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.showToolbar

/**
 * Fragment use for managing add-ons.
 */
@Suppress("TooManyFunctions")
class AddonsManagementFragment : Fragment(), AddonsManagerAdapterDelegate {
    private lateinit var recyclerView: RecyclerView
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_ons_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindRecyclerView(view)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_addons))
    }

    override fun onStart() {
        super.onStart()
        findPreviousDialogFragment()?.let { dialog ->
            dialog.onPositiveButtonClicked = onPositiveButtonClicked
        }
    }

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

    private fun bindRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.add_ons_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        scope.launch {
            try {
                val addons = requireContext().components.addonManager.getAddons()

                scope.launch(Dispatchers.Main) {
                    val adapter = AddonsManagerAdapter(
                        requireContext().components.addonCollectionProvider,
                        this@AddonsManagementFragment,
                        addons
                    )
                    recyclerView.adapter = adapter
                }
            } catch (e: AddonManagerException) {
                scope.launch(Dispatchers.Main) {
                    showSnackBar(view, "Failed to query Add-ons!")
                }
            }
        }
    }

    private fun showInstalledAddonDetailsFragment(addon: Addon) {
        val directions =
            AddonsManagementFragmentDirections.actionAddonsManagementFragmentToInstalledAddonDetails(
                addon
            )
        Navigation.findNavController(requireView()).navigate(directions)
    }

    private fun showDetailsFragment(addon: Addon) {
        val directions =
            AddonsManagementFragmentDirections.actionAddonsManagementFragmentToAddonDetailsFragment(
                addon
            )
        Navigation.findNavController(requireView()).navigate(directions)
    }

    private fun isAlreadyADialogCreated(): Boolean {
        return findPreviousDialogFragment() != null
    }

    private fun findPreviousDialogFragment(): PermissionsDialogFragment? {
        return parentFragmentManager.findFragmentByTag(PERMISSIONS_DIALOG_FRAGMENT_TAG) as? PermissionsDialogFragment
    }

    private fun showPermissionDialog(addon: Addon) {
        val dialog = PermissionsDialogFragment.newInstance(
            addon = addon,
            onPositiveButtonClicked = onPositiveButtonClicked
        )

        if (!isAlreadyADialogCreated()) {
            dialog.show(parentFragmentManager, PERMISSIONS_DIALOG_FRAGMENT_TAG)
        }
    }

    private val onPositiveButtonClicked: ((Addon) -> Unit) = { addon ->
        addonProgressOverlay.visibility = View.VISIBLE
        requireContext().components.addonManager.installAddon(
            addon,
            onSuccess = {
                this@AddonsManagementFragment.view?.let { view ->
                    showSnackBar(view, "Successfully installed: ${it.translatableName.translate()}")
                    bindRecyclerView(view)
                }

                addonProgressOverlay.visibility = View.GONE
            },
            onError = { _, _ ->
                this@AddonsManagementFragment.view?.let { view ->
                    showSnackBar(view, "Failed to install: ${addon.translatableName.translate()}")
                }
                addonProgressOverlay.visibility = View.GONE
            }
        )
    }

    companion object {
        private const val PERMISSIONS_DIALOG_FRAGMENT_TAG = "ADDONS_PERMISSIONS_DIALOG_FRAGMENT"
    }

    private inner class Section(@StringRes val title: Int)
}
