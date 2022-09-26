/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.Manifest.permission.CAMERA
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import mozilla.components.feature.qr.QrFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.account.DefaultSyncController
import org.mozilla.fenix.settings.account.DefaultSyncInteractor
import org.mozilla.fenix.utils.CameraPermissionHandler

/**
 * Let the user scan a QRCode to associate a Firefox Account and sync devices
 */
class PairFragment : Fragment(R.layout.fragment_pair), UserInteractionHandler, CameraPermissionHandler {

    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()
    private lateinit var interactor: DefaultSyncInteractor

    private val cameraPermissionLauncher = registerCameraLauncher(
        permissionGranted = { qrFeature.get()?.onPermissionsResult(arrayOf(CAMERA), intArrayOf(0)) },
        permissionDenied = { forever: Boolean ->
            if (forever) interactor.onCameraPermissionsNeeded()
            findNavController().popBackStack(R.id.turnOnSyncFragment, false)
        },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        interactor = DefaultSyncInteractor(
            DefaultSyncController(activity = activity as HomeActivity),
        )

        qrFeature.set(
            QrFeature(
                requireContext(),
                fragmentManager = parentFragmentManager,
                onNeedToRequestPermissions = { permissions ->
                    assert(permissions.size == 1 && permissions[0] == CAMERA)
                    cameraPermissionLauncher.launch(CAMERA)
                },
                onScanResult = { pairingUrl ->
                    // By the time we get a scan result, we may not be attached to the context anymore.
                    // See https://github.com/mozilla-mobile/fenix/issues/15812
                    if (context == null) {
                        findNavController().popBackStack(
                            R.id.turnOnSyncFragment,
                            false,
                        )
                        return@QrFeature
                    }
                    requireComponents.services.accountsAuthFeature.beginPairingAuthentication(
                        requireContext(),
                        pairingUrl,
                    )
                    val vibrator = requireContext().getSystemService<Vibrator>()!!
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                VIBRATE_LENGTH,
                                VibrationEffect.DEFAULT_AMPLITUDE,
                            ),
                        )
                    } else {
                        @Suppress("Deprecation")
                        vibrator.vibrate(VIBRATE_LENGTH)
                    }
                    findNavController().popBackStack(
                        R.id.turnOnSyncFragment,
                        false,
                    )
                },
                scanMessage =
                if (requireContext().settings().allowDomesticChinaFxaServer &&
                    org.mozilla.fenix.Config.channel.isMozillaOnline
                ) {
                    R.string.pair_instructions_2_cn
                } else {
                    R.string.pair_instructions_2
                },
            ),
            owner = this,
            view = view,
        )

        qrFeature.withFeature {
            it.scan(R.id.pair_layout)
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.sync_scan_code))
    }

    override fun onBackPressed(): Boolean {
        qrFeature.onBackPressed()
        findNavController().popBackStack(R.id.turnOnSyncFragment, false)
        return true
    }

    companion object {
        private const val VIBRATE_LENGTH = 200L
    }
}
