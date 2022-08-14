/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.plus
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.content.PermissionHighlightsState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.permission.SitePermissions
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.R
import org.mozilla.fenix.android.FenixDialogFragment
import org.mozilla.fenix.databinding.FragmentQuickSettingsDialogSheetBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.navigation.NavRouteInfo
import org.mozilla.fenix.navigation.ScreenArgsInfo
import org.mozilla.fenix.settings.PhoneFeature

/**
 * Dialog that presents the user with information about
 * - the current website and whether the connection is secured or not.
 * - website tracking protection.
 * - website permission.
 */
@Suppress("TooManyFunctions")
class QuickSettingsSheetDialogFragment : FenixDialogFragment() {

    private lateinit var quickSettingsStore: QuickSettingsFragmentStore
    private lateinit var quickSettingsController: QuickSettingsController
    private lateinit var websiteInfoView: WebsiteInfoView
    private lateinit var websitePermissionsView: WebsitePermissionsView
    private lateinit var clearSiteDataView: ClearSiteDataView

    @VisibleForTesting
    internal lateinit var trackingProtectionView: TrackingProtectionView

    private lateinit var interactor: QuickSettingsInteractor

    private var tryToRequestPermissions: Boolean = false
    private val args by navArgs<QuickSettingsSheetDialogFragmentArgs>()

    private var _binding: FragmentQuickSettingsDialogSheetBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    override val gravity: Int get() = args.gravity
    override val layoutId: Int = R.layout.fragment_quick_settings_dialog_sheet

    @Suppress("DEPRECATION")
    // https://github.com/mozilla-mobile/fenix/issues/19920
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val components = context.components

        val rootView = inflateRootView(container)
        _binding = FragmentQuickSettingsDialogSheetBinding.bind(rootView)

        val navController = findNavController()
        quickSettingsStore = QuickSettingsFragmentStore.createStore(
            context = context,
            websiteUrl = args.url,
            websiteTitle = args.title,
            isSecured = args.isSecured,
            permissions = args.sitePermissions,
            settings = components.settings,
            certificateName = args.certificateName,
            permissionHighlights = args.permissionHighlights,
            sessionId = args.sessionId,
            isTrackingProtectionEnabled = args.isTrackingProtectionEnabled
        )

        quickSettingsController = DefaultQuickSettingsController(
            context = context,
            quickSettingsStore = quickSettingsStore,
            browserStore = components.core.store,
            ioScope = viewLifecycleOwner.lifecycleScope + Dispatchers.IO,
            navController = navController,
            sessionId = args.sessionId,
            sitePermissions = args.sitePermissions,
            settings = components.settings,
            permissionStorage = components.core.permissionStorage,
            reload = components.useCases.sessionUseCases.reload,
            requestRuntimePermissions = { permissions ->
                requestPermissions(permissions, REQUEST_CODE_QUICK_SETTINGS_PERMISSIONS)
                tryToRequestPermissions = true
            },
            displayPermissions = ::showPermissionsView
        )

        interactor = QuickSettingsInteractor(quickSettingsController)
        websiteInfoView = WebsiteInfoView(binding.websiteInfoLayout, interactor = interactor)
        websitePermissionsView =
            WebsitePermissionsView(binding.websitePermissionsLayout, interactor)
        trackingProtectionView =
            TrackingProtectionView(binding.trackingProtectionLayout, interactor, context.settings())
        clearSiteDataView = ClearSiteDataView(
            context = context,
            ioScope = viewLifecycleOwner.lifecycleScope + Dispatchers.IO,
            containerView = binding.clearSiteDataLayout,
            containerDivider = binding.clearSiteDataDivider,
            interactor = interactor,
            navController = navController
        )

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeTrackersChange(requireComponents.core.store)
        consumeFrom(quickSettingsStore) {
            websiteInfoView.update(it.webInfoState)
            websitePermissionsView.update(it.websitePermissionsState)
            trackingProtectionView.update(it.trackingProtectionState)
            clearSiteDataView.update(it.webInfoState)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (arePermissionsGranted(requestCode, grantResults)) {
            PhoneFeature.findFeatureBy(permissions)?.let {
                quickSettingsController.handleAndroidPermissionGranted(it)
            }
        } else {
            val shouldShowRequestPermissionRationale =
                permissions.all { shouldShowRequestPermissionRationale(it) }

            if (!shouldShowRequestPermissionRationale && tryToRequestPermissions) {
                // The user has permanently blocked these permissions and he/she is trying to enabling them.
                // at this point, we are not able to request these permissions, the only way to allow
                // them, it is to take the user to the system app setting page, and there the user
                // can allow the permissions.
                openSystemSettings()
            }
        }
        tryToRequestPermissions = false
    }

    private fun arePermissionsGranted(requestCode: Int, grantResults: IntArray) =
        requestCode == REQUEST_CODE_QUICK_SETTINGS_PERMISSIONS && grantResults.all { it == PERMISSION_GRANTED }

    private fun showPermissionsView() {
        binding.websitePermissionsGroup.isVisible = true
    }

    private fun openSystemSettings() {
        startActivity(
            Intent().apply {
                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            }
        )
    }

    @VisibleForTesting
    internal fun provideTabId(): String = args.sessionId

    @VisibleForTesting
    internal fun observeTrackersChange(store: BrowserStore) {
        consumeFlow(store) { flow ->
            flow.mapNotNull { state ->
                state.findTabOrCustomTab(provideTabId())
            }.ifAnyChanged { tab ->
                arrayOf(
                    tab.trackingProtection.blockedTrackers,
                    tab.trackingProtection.loadedTrackers
                )
            }.collect {
                updateTrackers(it)
            }
        }
    }

    @VisibleForTesting
    internal fun updateTrackers(tab: SessionState) {
        provideTrackingProtectionUseCases().fetchTrackingLogs(
            tab.id,
            onSuccess = { trackers ->
                trackingProtectionView.updateDetailsSection(trackers.isNotEmpty())
            },
            onError = {
                Logger.error("QuickSettingsSheetDialogFragment - fetchTrackingLogs onError", it)
            }
        )
    }

    @VisibleForTesting
    internal fun provideTrackingProtectionUseCases() = requireComponents.useCases.trackingProtectionUseCases

    companion object {
        const val REQUEST_CODE_QUICK_SETTINGS_PERMISSIONS = 4
        const val ARG_SESSION_ID = "sessionId"
        const val ARG_TITLE = "title"
        const val ARG_URL = "url"
        const val ARG_IS_SECURED = "isSecured"
        const val ARG_SITE_PERMISSIONS = "sitePermissions"
        const val ARG_GRAVITY = "gravity"
        const val ARG_CERTIFICATE_NAME = "certificateName"
        const val ARG_PERMISSION_HIGHLIGHTS = "permissionHighlights"
        const val ARG_IS_TRACKING_PROTECTION_ENABLED = "isTrackingProtectionEnabled"

        val NAV_ROUTE_INFO = NavRouteInfo(
            baseRoute = "quick_settings_sheet_dialog",
            screenArgs = listOf(
                ScreenArgsInfo(ARG_SESSION_ID, NavType.StringType),
                ScreenArgsInfo(ARG_TITLE, NavType.StringType),
                ScreenArgsInfo(ARG_URL, NavType.StringType),
                ScreenArgsInfo(ARG_IS_SECURED, NavType.BoolType, true),
                ScreenArgsInfo(ARG_SITE_PERMISSIONS, NavType.ParcelableType(type = SitePermissions::class.java)),
                ScreenArgsInfo(ARG_GRAVITY, NavType.IntType, 80),
                ScreenArgsInfo(ARG_CERTIFICATE_NAME, NavType.StringType),
                ScreenArgsInfo(ARG_PERMISSION_HIGHLIGHTS, NavType.ParcelableType(type = PermissionHighlightsState::class.java)),
                ScreenArgsInfo(ARG_IS_TRACKING_PROTECTION_ENABLED, NavType.BoolType, true)
            )
        )
    }
}
