/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.QuicksettingsClearSiteDataBinding
import org.mozilla.fenix.ext.components

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
    val containerDivider: View,
    val interactor: ClearSiteDataViewInteractor
) {
    private val context = containerView.context

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var baseDomain : String

    val binding = QuicksettingsClearSiteDataBinding.inflate(
        LayoutInflater.from(context),
        containerView,
        true
    )

    fun update(webInfoState: WebsiteInfoState) {
        if (!FeatureFlags.showClearSiteData) {
            setVisibility(false)
            return
        }

        // TODO: Hide if there are no cookies for current host.
        baseDomain = webInfoState.baseDomain(context.components.publicSuffixList)

        setVisibility(true)
        binding.clearSiteData.setOnClickListener {
            askToClear()
        }
    }

    private fun setVisibility(visible : Boolean) {
        binding.root.isVisible = visible
        containerDivider.isVisible = visible
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun askToClear() {
        context?.let {
            AlertDialog.Builder(it).apply {
                setMessage(
                    it.getString(
                        R.string.confirm_clear_site_data,
                        baseDomain
                    )
                )

                setNegativeButton(R.string.delete_browsing_data_prompt_cancel) { it: DialogInterface, _ ->
                    it.cancel()
                }

                setPositiveButton(R.string.delete_browsing_data_prompt_allow) { it: DialogInterface, _ ->
                    it.dismiss()
                    interactor.onClearSiteDataClicked()
                }
                create()
            }.show()
        }
    }
}