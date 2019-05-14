/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import mozilla.components.feature.qr.QrFeature
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents

class PairFragment : Fragment(), BackHandler {
    private val qrFeature = ViewBoundFeatureWrapper<QrFeature>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pair, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val instructionsText = view.findViewById(R.id.pair_instructions) as TextView
        instructionsText.setText(HtmlCompat.fromHtml(getString(R.string.pair_instructions),
            HtmlCompat.FROM_HTML_MODE_LEGACY))

        qrFeature.set(
            QrFeature(
                requireContext(),
                fragmentManager = requireFragmentManager(),
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_CAMERA_PERMISSIONS)
                },
                onScanResult = { pairingUrl ->
                    requireComponents.services.accountsAuthFeature.beginPairingAuthentication(pairingUrl)
                    view?.let {
                        (activity as HomeActivity).openToBrowser(BrowserDirection.FromSettings)
                    }
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
        (activity as AppCompatActivity).title = getString(R.string.preferences_sync)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onBackPressed(): Boolean {
        return when {
            qrFeature.onBackPressed() -> true
            else -> false
        }
    }

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_CAMERA_PERMISSIONS -> qrFeature.withFeature {
                it.onPermissionsResult(permissions, grantResults)
            }
        }
    }
}
