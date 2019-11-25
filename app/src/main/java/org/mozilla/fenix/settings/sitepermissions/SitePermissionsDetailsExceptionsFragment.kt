/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.sitepermissions

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import mozilla.components.feature.sitepermissions.SitePermissions
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.PhoneFeature.CAMERA
import org.mozilla.fenix.settings.PhoneFeature.LOCATION
import org.mozilla.fenix.settings.PhoneFeature.MICROPHONE
import org.mozilla.fenix.settings.PhoneFeature.NOTIFICATION

class SitePermissionsDetailsExceptionsFragment : PreferenceFragmentCompat() {
    private lateinit var sitePermissions: SitePermissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sitePermissions = SitePermissionsDetailsExceptionsFragmentArgs
            .fromBundle(requireArguments())
            .sitePermissions
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.site_permissions_details_exceptions_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(sitePermissions.origin)
        lifecycleScope.launch(IO) {
            val context = requireContext()
            sitePermissions =
                requireNotNull(context.components.core.permissionStorage.findSitePermissionsBy(sitePermissions.origin))
            launch(Main) {
                bindCategoryPhoneFeatures()
            }
        }
    }

    private fun bindCategoryPhoneFeatures() {
        val context = requireContext()

        val cameraAction = CAMERA.getActionLabel(context, sitePermissions)
        val locationAction = LOCATION.getActionLabel(context, sitePermissions)
        val microphoneAction = MICROPHONE.getActionLabel(context, sitePermissions)
        val notificationAction = NOTIFICATION.getActionLabel(context, sitePermissions)

        initPhoneFeature(CAMERA, cameraAction)
        initPhoneFeature(LOCATION, locationAction)
        initPhoneFeature(MICROPHONE, microphoneAction)
        initPhoneFeature(NOTIFICATION, notificationAction)
        bindClearPermissionsButton()
    }

    private fun initPhoneFeature(phoneFeature: PhoneFeature, summary: String) {
        val keyPreference = phoneFeature.getPreferenceKey(requireContext())
        val cameraPhoneFeatures: Preference = requireNotNull(findPreference(keyPreference))
        cameraPhoneFeatures.summary = summary

        cameraPhoneFeatures.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            navigateToPhoneFeature(phoneFeature)
            true
        }
    }

    private fun bindClearPermissionsButton() {
        val keyPreference = getPreferenceKey(R.string.pref_key_exceptions_clear_site_permissions)
        val button: Preference = requireNotNull(findPreference(keyPreference))

        button.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AlertDialog.Builder(requireContext()).apply {
                setMessage(R.string.confirm_clear_permissions_site)
                setTitle(R.string.clear_permissions)
                setPositiveButton(android.R.string.yes) { dialog: DialogInterface, _ ->
                    clearSitePermissions()
                    dialog.dismiss()
                }
                setNegativeButton(android.R.string.no) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
            }.show()

            true
        }
    }

    private fun clearSitePermissions() {
        lifecycleScope.launch(IO) {
            requireContext().components.core.permissionStorage.deleteSitePermissions(sitePermissions)
            launch(Main) {
                Navigation.findNavController(requireNotNull(view)).popBackStack()
            }
        }
    }

    private fun navigateToPhoneFeature(phoneFeature: PhoneFeature) {
        val directions =
            SitePermissionsDetailsExceptionsFragmentDirections.actionSitePermissionsToExceptionsToManagePhoneFeature(
                phoneFeatureId = phoneFeature.id,
                sitePermissions = sitePermissions
            )
        Navigation.findNavController(view!!).navigate(directions)
    }
}
