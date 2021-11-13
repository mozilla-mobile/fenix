/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.google.common.net.InternetDomainName
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.QuicksettingsClearSiteDataBinding

/**
 * Contract declaring all possible user interactions with [ClearSiteDataView].
 */
interface ClearSiteDataViewInteractor {
    /**
     * Shows the confirmation dialog to clear site data for current domain.
     */
    fun onClearSiteDataClicked(domain : String)
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

    fun update(webInfoState: WebsiteInfoState) {
        // TODO: Hide behind a feature flag by now?
        val domain = baseDomain(webInfoState.websiteUrl.toUri().host.toString())
        if (domain.isNullOrEmpty()) {
            binding.root.isVisible = false
            return
        }

        // TODO: Hide if there are no cookies for current host.
        binding.root.isVisible = true
        binding.clearSiteData.setOnClickListener {
            askToClear(domain)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun baseDomain(host : String) : String? {
        if (!InternetDomainName.isValid(host)) {
            return host
        }
        return InternetDomainName.from(host).topPrivateDomain().toString()
    }

    private fun askToClear(domain : String) {
        context?.let {
            AlertDialog.Builder(it).apply {
                setMessage(
                    it.getString(
                        R.string.confirm_clear_site_data,
                        domain
                    )
                )

                setNegativeButton(R.string.delete_browsing_data_prompt_cancel) { it: DialogInterface, _ ->
                    it.cancel()
                }

                setPositiveButton(R.string.delete_browsing_data_prompt_allow) { it: DialogInterface, _ ->
                    it.dismiss()
                    interactor.onClearSiteDataClicked(domain)
                }
                create()
            }.show()
        }
    }
}