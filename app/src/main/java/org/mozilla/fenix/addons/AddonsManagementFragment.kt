/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_add_ons_management.*
import kotlinx.android.synthetic.main.fragment_add_ons_management.view.*
import kotlinx.android.synthetic.main.overlay_add_on_progress.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManagerException
import mozilla.components.feature.addons.ui.AddonsManagerAdapter
import mozilla.components.feature.addons.ui.AddonsManagerAdapterDelegate
import mozilla.components.feature.addons.ui.PermissionsDialogFragment
import mozilla.components.feature.addons.ui.translatedName
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.ThemeManager

/**
 * Fragment use for managing add-ons.
 */
class AddonsManagementFragment : Fragment(R.layout.fragment_add_ons_management) {
    /**
     * Whether or not an add-on installation is in progress.
     */
    private var isInstallationInProgress = false
    private var adapter: AddonsManagerAdapter? = null

    private val addonsManagerDelegate = object : AddonsManagerAdapterDelegate {
        override fun onAddonItemClicked(addon: Addon) {
            val directions = if (addon.isInstalled()) {
                AddonsManagementFragmentDirections
                    .actionAddonsManagementFragmentToInstalledAddonDetails(addon)
            } else {
                AddonsManagementFragmentDirections
                    .actionAddonsManagementFragmentToAddonDetailsFragment(addon)
            }

            findNavController().navigate(directions)
        }

        override fun onInstallAddonButtonClicked(addon: Addon) {
            showPermissionDialog(addon)
        }

        override fun onNotYetSupportedSectionClicked(unsupportedAddons: List<Addon>) {
            val directions =
                AddonsManagementFragmentDirections.actionAddonsManagementFragmentToNotYetSupportedAddonFragment(
                    unsupportedAddons.toTypedArray()
                )
            findNavController().navigate(directions)
        }
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

    private fun bindRecyclerView(view: View) {
        val context = view.context
        val recyclerView = view.add_ons_list
        recyclerView.layoutManager = LinearLayoutManager(context)
        val shouldRefresh = adapter != null

        viewLifecycleOwner.lifecycleScope.launch(Main) {
            try {
                val addons = withContext(IO) { context.components.addonManager.getAddons() }

                if (!shouldRefresh) {
                    adapter = AddonsManagerAdapter(
                        context.components.addonCollectionProvider,
                        addonsManagerDelegate,
                        addons,
                        style = createAddonStyle(context)
                    )
                }
                isInstallationInProgress = false
                view.add_ons_progress_bar.isVisible = false
                view.add_ons_empty_message.isVisible = false

                recyclerView.adapter = adapter
                if (shouldRefresh) {
                    adapter?.updateAddons(addons)
                }
            } catch (e: AddonManagerException) {
                showSnackBar(
                    view,
                    getString(R.string.mozac_feature_addons_failed_to_query_add_ons)
                )
                isInstallationInProgress = false
                view.add_ons_progress_bar.isVisible = false
                view.add_ons_empty_message.isVisible = true
            }
        }
    }

    private fun createAddonStyle(context: Context): AddonsManagerAdapter.Style {
        return AddonsManagerAdapter.Style(
            sectionsTextColor = ThemeManager.resolveAttribute(R.attr.primaryText, context),
            addonNameTextColor = ThemeManager.resolveAttribute(R.attr.primaryText, context),
            addonSummaryTextColor = ThemeManager.resolveAttribute(R.attr.secondaryText, context),
            sectionsTypeFace = ResourcesCompat.getFont(context, R.font.metropolis_semibold),
            addonBackgroundIconColor = ThemeManager.resolveAttribute(R.attr.inset, requireContext()),
            addonAllowPrivateBrowsingLabelDrawableRes = R.drawable.ic_add_on_private_browsing_label
        )
    }

    private fun findPreviousDialogFragment(): PermissionsDialogFragment? {
        return parentFragmentManager.findFragmentByTag(PERMISSIONS_DIALOG_FRAGMENT_TAG) as? PermissionsDialogFragment
    }

    private fun hasExistingPermissionDialogFragment(): Boolean {
        return findPreviousDialogFragment() != null
    }

    private fun showPermissionDialog(addon: Addon) {
        if (!isInstallationInProgress && !hasExistingPermissionDialogFragment()) {
            val dialog = PermissionsDialogFragment.newInstance(
                addon = addon,
                promptsStyling = PermissionsDialogFragment.PromptsStyling(
                    gravity = Gravity.BOTTOM,
                    shouldWidthMatchParent = true,
                    positiveButtonBackgroundColor = ThemeManager.resolveAttribute(
                        R.attr.accent,
                        requireContext()
                    ),
                    positiveButtonTextColor = ThemeManager.resolveAttribute(
                        R.attr.contrastText,
                        requireContext()
                    ),
                    positiveButtonRadius = (resources.getDimensionPixelSize(R.dimen.tab_corner_radius)).toFloat()
                ),
                onPositiveButtonClicked = onPositiveButtonClicked
            )
            dialog.show(parentFragmentManager, PERMISSIONS_DIALOG_FRAGMENT_TAG)
        }
    }

    /**
     * Click handler for addons permission dialog.
     */
    private val onPositiveButtonClicked: ((Addon) -> Unit) = { addon ->
        addonProgressOverlay?.visibility = View.VISIBLE

        if (requireContext().settings().accessibilityServicesEnabled) {
            announceForAccessibility(addonProgressOverlay.add_ons_overlay_text.text)
        }

        isInstallationInProgress = true

        requireContext().components.addonManager.installAddon(
            addon,
            onSuccess = {
                getRootView()?.let { rootView ->
                    showSnackBar(
                        rootView,
                        getString(
                            R.string.mozac_feature_addons_successfully_installed,
                            it.translatedName
                        )
                    )
                    adapter?.updateAddon(it)
                    addonProgressOverlay?.visibility = View.GONE
                    isInstallationInProgress = false
                }
            },
            onError = { _, _ ->
                getRootView()?.let { rootView ->
                    showSnackBar(
                        rootView,
                        getString(
                            R.string.mozac_feature_addons_failed_to_install,
                            addon.translatedName
                        )
                    )
                    addonProgressOverlay?.visibility = View.GONE
                    isInstallationInProgress = false
                }
            }
        )
    }

    private fun announceForAccessibility(announcementText: CharSequence) {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        addonProgressOverlay.onInitializeAccessibilityEvent(event)
        event.text.add(announcementText)
        event.contentDescription = null
        addonProgressOverlay.parent.requestSendAccessibilityEvent(addonProgressOverlay, event)
    }

    private fun getRootView() = activity?.getRootView() ?: view

    companion object {
        private const val PERMISSIONS_DIALOG_FRAGMENT_TAG = "ADDONS_PERMISSIONS_DIALOG_FRAGMENT"
    }
}
