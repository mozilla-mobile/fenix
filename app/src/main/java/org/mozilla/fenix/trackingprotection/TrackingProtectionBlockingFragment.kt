/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.trackingprotection

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_tracking_protection_blocking.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.showToolbar

class TrackingProtectionBlockingFragment :
    Fragment(R.layout.fragment_tracking_protection_blocking) {

    private val args: TrackingProtectionBlockingFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        category_fingerprinters.isVisible = args.strictMode
        category_tracking_content.isVisible = args.strictMode
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getTitle())
    }

    private fun getTitle(): String {
        return if (args.strictMode) {
            getString(R.string.preference_enhanced_tracking_protection_strict_default)
        } else {
            getString(R.string.preference_enhanced_tracking_protection_standard_option)
        }
    }
}
