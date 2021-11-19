/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManagerException
import mozilla.components.feature.addons.ui.PermissionsDialogFragment
import mozilla.components.feature.addons.ui.translateName
import io.github.forkmaintainers.iceraven.components.PagedAddonInstallationDialogFragment
import io.github.forkmaintainers.iceraven.components.PagedAddonsManagerAdapter
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.databinding.FragmentAddOnsManagementBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.ext.runIfFragmentIsAttached
import org.mozilla.fenix.theme.ThemeManager
import java.util.Locale
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException

/**
 * Fragment use for managing add-ons.
 */
@Suppress("TooManyFunctions", "LargeClass")
class AddonsManagementFragment : Fragment(R.layout.fragment_add_ons_management) {

    private val args by navArgs<AddonsManagementFragmentArgs>()

    private var binding: FragmentAddOnsManagementBinding? = null

    /**
     * Whether or not an add-on installation is in progress.
     */
    private var isInstallationInProgress = false
    private var adapter: PagedAddonsManagerAdapter? = null
    // We must save the add-on list in the class, or we won't have it
    // downloaded for the non-suspending search function
    private var addons: List<Addon>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private var installExternalAddonComplete: Boolean
        set(value) {
            arguments?.putBoolean(BUNDLE_KEY_INSTALL_EXTERNAL_ADDON_COMPLETE, value)
        }
        get() {
            return arguments?.getBoolean(BUNDLE_KEY_INSTALL_EXTERNAL_ADDON_COMPLETE, false) ?: false
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddOnsManagementBinding.bind(view)
        bindRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.addons_menu, menu)
        val searchItem = menu.findItem(R.id.search)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.queryHint = getString(R.string.addons_search_hint)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return searchAddons(query.trim())
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return searchAddons(newText.trim())
            }
        })
    }

    private fun searchAddons(addonNameSubStr: String): Boolean {
        if (adapter == null) {
            return false
        }

        val searchedAddons = arrayListOf<Addon>()

        addons?.forEach { addon ->
            val names = addon.translatableName
            names["en-US"]?.let { name ->
                if (name.lowercase().contains(addonNameSubStr.lowercase())) {
                    searchedAddons.add(addon)
                }
            }
        }
        updateUI(searchedAddons)

        return true
    }

    private fun updateUI(searchedAddons: List<Addon>) {
        adapter?.updateAddons(searchedAddons)

        if (searchedAddons.isEmpty()) {
            binding?.addOnsEmptyMessage?.visibility = View.VISIBLE
            binding?.addOnsList?.visibility = View.GONE
        } else {
            binding?.addOnsEmptyMessage?.visibility = View.GONE
            binding?.addOnsList?.visibility = View.VISIBLE
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        // letting go of the resources to avoid memory leak.
        adapter = null
        binding = null
    }

    private fun bindRecyclerView() {
        val managementView = AddonsManagementView(
            navController = findNavController(),
            showPermissionDialog = ::showPermissionDialog
        )

        val recyclerView = binding?.addOnsList
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        val shouldRefresh = adapter != null

        // If the fragment was launched to install an "external" add-on from AMO, we deactivate
        // the cache to get the most up-to-date list of add-ons to match against.
        val allowCache = args.installAddonId == null || installExternalAddonComplete
        lifecycleScope.launch(IO) {
            try {
                addons = requireContext().components.addonManager.getAddons(allowCache = allowCache)
                lifecycleScope.launch(Dispatchers.Main) {
                    runIfFragmentIsAttached {
                        if (!shouldRefresh) {
                            adapter = PagedAddonsManagerAdapter(
                                requireContext().components.addonCollectionProvider,
                                managementView,
                                addons!!,
                                style = createAddonStyle(requireContext())
                            )
                        }
                        isInstallationInProgress = false
                        binding?.addOnsProgressBar?.isVisible = false
                        binding?.addOnsEmptyMessage?.isVisible = false

                        recyclerView?.adapter = adapter
                        if (shouldRefresh) {
                            adapter?.updateAddons(addons!!)
                        }

                        args.installAddonId?.let { addonIn ->
                            if (!installExternalAddonComplete) {
                                installExternalAddon(addons!!, addonIn)
                            }
                        }
                    }
                }
            } catch (e: AddonManagerException) {
                lifecycleScope.launch(Dispatchers.Main) {
                    runIfFragmentIsAttached {
                        binding?.let {
                            showSnackBar(it.root, getString(R.string.mozac_feature_addons_failed_to_query_add_ons))
                        }
                        isInstallationInProgress = false
                        binding?.addOnsProgressBar?.isVisible = false
                        binding?.addOnsEmptyMessage?.isVisible = true
                    }
                }
            }
        }
    }

    @VisibleForTesting
    internal fun installExternalAddon(supportedAddons: List<Addon>, installAddonId: String) {
        val addonToInstall = supportedAddons.find { it.downloadId == installAddonId }
        if (addonToInstall == null) {
            showErrorSnackBar(getString(R.string.addon_not_supported_error))
        } else {
            if (addonToInstall.isInstalled()) {
                showErrorSnackBar(getString(R.string.addon_already_installed))
            } else {
                showPermissionDialog(addonToInstall)
            }
        }
        installExternalAddonComplete = true
    }

    @VisibleForTesting
    internal fun showErrorSnackBar(text: String) {
        runIfFragmentIsAttached {
            view?.let {
                showSnackBar(it, text, FenixSnackbar.LENGTH_LONG)
            }
        }
    }

    private fun createAddonStyle(context: Context): PagedAddonsManagerAdapter.Style {
        return PagedAddonsManagerAdapter.Style(
            sectionsTextColor = ThemeManager.resolveAttribute(R.attr.primaryText, context),
            addonNameTextColor = ThemeManager.resolveAttribute(R.attr.primaryText, context),
            addonSummaryTextColor = ThemeManager.resolveAttribute(R.attr.secondaryText, context),
            sectionsTypeFace = ResourcesCompat.getFont(context, R.font.metropolis_semibold),
            addonAllowPrivateBrowsingLabelDrawableRes = R.drawable.ic_add_on_private_browsing_label
        )
    }

    private fun findPreviousDialogFragment(): PermissionsDialogFragment? {
        return parentFragmentManager.findFragmentByTag(PERMISSIONS_DIALOG_FRAGMENT_TAG) as? PermissionsDialogFragment
    }

    private fun hasExistingPermissionDialogFragment(): Boolean {
        return findPreviousDialogFragment() != null
    }

    private fun hasExistingAddonInstallationDialogFragment(): Boolean {
        return parentFragmentManager.findFragmentByTag(INSTALLATION_DIALOG_FRAGMENT_TAG)
                as? PagedAddonInstallationDialogFragment != null
    }

    @VisibleForTesting
    internal fun showPermissionDialog(addon: Addon) {
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

    private fun showInstallationDialog(addon: Addon) {
        if (!isInstallationInProgress && !hasExistingAddonInstallationDialogFragment()) {
            requireComponents.analytics.metrics.track(Event.AddonInstalled(addon.id))
            val context = requireContext()
            val addonCollectionProvider = context.components.addonCollectionProvider

            // Fragment may not be attached to the context anymore during onConfirmButtonClicked handling,
            // but we still want to be able to process user selection of the 'allowInPrivateBrowsing' pref.
            // This is a best-effort attempt to do so - retain a weak reference to the application context
            // (to avoid a leak), which we attempt to use to access addonManager.
            // See https://github.com/mozilla-mobile/fenix/issues/15816
            val weakApplicationContext: WeakReference<Context> = WeakReference(context)

            val dialog = PagedAddonInstallationDialogFragment.newInstance(
                addon = addon,
                addonCollectionProvider = addonCollectionProvider,
                promptsStyling = PagedAddonInstallationDialogFragment.PromptsStyling(
                    gravity = Gravity.BOTTOM,
                    shouldWidthMatchParent = true,
                    confirmButtonBackgroundColor = ThemeManager.resolveAttribute(
                        R.attr.accent,
                        requireContext()
                    ),
                    confirmButtonTextColor = ThemeManager.resolveAttribute(
                        R.attr.contrastText,
                        requireContext()
                    ),
                    confirmButtonRadius = (resources.getDimensionPixelSize(R.dimen.tab_corner_radius)).toFloat()
                ),
                onConfirmButtonClicked = { _, allowInPrivateBrowsing ->
                    if (allowInPrivateBrowsing) {
                        weakApplicationContext.get()?.components?.addonManager?.setAddonAllowedInPrivateBrowsing(
                            addon,
                            allowInPrivateBrowsing,
                            onSuccess = {
                                runIfFragmentIsAttached {
                                    adapter?.updateAddon(it)
                                }
                            }
                        )
                    }
                }
            )

            dialog.show(parentFragmentManager, INSTALLATION_DIALOG_FRAGMENT_TAG)
        }
    }

    private val onPositiveButtonClicked: ((Addon) -> Unit) = { addon ->
        binding?.addonProgressOverlay?.overlayCardView?.visibility = View.VISIBLE

        if (requireContext().settings().accessibilityServicesEnabled) {
            binding?.let { announceForAccessibility(it.addonProgressOverlay.addOnsOverlayText.text) }
        }

        isInstallationInProgress = true

        val installOperation = requireContext().components.addonManager.installAddon(
            addon,
            onSuccess = {
                runIfFragmentIsAttached {
                    isInstallationInProgress = false
                    adapter?.updateAddon(it)
                    binding?.addonProgressOverlay?.overlayCardView?.visibility = View.GONE
                    showInstallationDialog(it)
                }
            },
            onError = { _, e ->
                this@AddonsManagementFragment.view?.let { view ->
                    // No need to display an error message if installation was cancelled by the user.
                    if (e !is CancellationException) {
                        val rootView = activity?.getRootView() ?: view
                        context?.let {
                            showSnackBar(
                                rootView,
                                getString(
                                    R.string.mozac_feature_addons_failed_to_install,
                                    addon.translateName(it)
                                )
                            )
                        }
                    }
                    binding?.addonProgressOverlay?.overlayCardView?.visibility = View.GONE
                    isInstallationInProgress = false
                }
            }
        )

        binding?.addonProgressOverlay?.cancelButton?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                val safeBinding = binding
                // Hide the installation progress overlay once cancellation is successful.
                if (installOperation.cancel().await()) {
                    safeBinding?.addonProgressOverlay?.overlayCardView?.visibility = View.GONE
                }
            }
        }
    }

    private fun announceForAccessibility(announcementText: CharSequence) {
        val event = AccessibilityEvent.obtain(
            AccessibilityEvent.TYPE_ANNOUNCEMENT
        )

        binding?.addonProgressOverlay?.overlayCardView?.onInitializeAccessibilityEvent(event)
        event.text.add(announcementText)
        event.contentDescription = null
        binding?.addonProgressOverlay?.overlayCardView?.parent?.requestSendAccessibilityEvent(
            binding?.addonProgressOverlay?.overlayCardView,
            event
        )
    }

    companion object {
        private const val PERMISSIONS_DIALOG_FRAGMENT_TAG = "ADDONS_PERMISSIONS_DIALOG_FRAGMENT"
        private const val INSTALLATION_DIALOG_FRAGMENT_TAG = "ADDONS_INSTALLATION_DIALOG_FRAGMENT"
        private const val BUNDLE_KEY_INSTALL_EXTERNAL_ADDON_COMPLETE = "INSTALL_EXTERNAL_ADDON_COMPLETE"
    }
}
