/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_pair.*
import mozilla.components.feature.qr.QrFeature
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents

class PairFragment : Fragment(R.layout.fragment_pair), BackHandler {

    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pairInstructions.text = HtmlCompat.fromHtml(getString(R.string.pair_instructions),
            HtmlCompat.FROM_HTML_MODE_LEGACY)

        qrFeature.set(
            QrFeature(
                requireContext(),
                fragmentManager = parentFragmentManager,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_CAMERA_PERMISSIONS)
                },
                onScanResult = { pairingUrl ->
                    requireComponents.services.accountsAuthFeature.beginPairingAuthentication(
                        requireContext(),
                        pairingUrl
                    )
                    findNavController(this@PairFragment)
                        .popBackStack(R.id.turnOnSyncFragment, false)
                }),
            owner = this,
            view = view
        )

        qrFeature.withFeature {
            it.scan(R.id.pair_layout)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).title = getString(R.string.sync_scan_code)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onBackPressed(): Boolean {
        qrFeature.onBackPressed()
        findNavController().popBackStack(R.id.turnOnSyncFragment, false)
        return true
    }

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSIONS -> qrFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
        }
    }
}
