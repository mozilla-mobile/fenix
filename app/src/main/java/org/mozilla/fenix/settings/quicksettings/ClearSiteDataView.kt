/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import org.mozilla.fenix.databinding.QuicksettingsClearSiteDataBinding
import org.mozilla.fenix.databinding.QuicksettingsPermissionsBinding

/**
 * Contract declaring all possible user interactions with [ClearSiteDataView].
 */
interface ClearSiteDataViewInteractor {
    /**
     * Shows the confirmation dialog to clear site data for current domain.
     */
    fun onClearSiteDataClicked()
}


/**
 * MVI View to access the dialog to clear site cookies and data.
 *
 * @param containerView [ViewGroup] in which this View will inflate itself.
 * @param interactor [TrackingProtectionInteractor] which will have delegated to all user
 * interactions.
 */
class ClearSiteDataView(
    val containerView: ViewGroup,
    val interactor: ClearSiteDataViewInteractor
) {
    private val context = containerView.context

    val binding = QuicksettingsClearSiteDataBinding.inflate(
        LayoutInflater.from(context),
        containerView,
        true
    )

    fun update() {
        // TODO: Hide behind a feature flag by now?
        // TODO: Hide if there are no cookies for current host.
        binding.root.isVisible = true
        binding.clearSiteData.setOnClickListener {
            interactor.onClearSiteDataClicked()
        }
    }
}