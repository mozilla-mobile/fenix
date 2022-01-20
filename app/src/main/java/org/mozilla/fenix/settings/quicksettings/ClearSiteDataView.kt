/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.QuicksettingsClearSiteDataBinding
import org.mozilla.fenix.ext.components

/**
 * Contract declaring all possible user interactions with [ClearSiteDataView].
 */
interface ClearSiteDataViewInteractor {
    /**
     * Shows the confirmation dialog to clear site data for [baseDomain].
     */
    fun onClearSiteDataClicked(baseDomain: String)
}

/**
 * MVI View to access the dialog to clear site cookies and data.
 *
 * @param containerView [ViewGroup] in which this View will inflate itself.
 * @param interactor [TrackingProtectionInteractor] which will have delegated to all user
 * interactions.
 */
class ClearSiteDataView(
    val context: Context,
    private val ioScope: CoroutineScope,
    val containerView: ViewGroup,
    val containerDivider: View,
    val interactor: ClearSiteDataViewInteractor,
    val navController: NavController
) {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var websiteUrl: String

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

        websiteUrl = webInfoState.websiteUrl

        setVisibility(true)
        binding.clearSiteData.setOnClickListener {
            askToClear()
            navController.popBackStack()
        }
    }

    private fun setVisibility(visible: Boolean) {
        binding.root.isVisible = visible
        containerDivider.isVisible = visible
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun askToClear() {

        ioScope.launch {
            val publicSuffixList = context.components.publicSuffixList
            val host = websiteUrl.toUri().host.orEmpty()
            val domain = publicSuffixList.getPublicSuffixPlusOne(host).await()

            domain?.let { baseDomain ->
                launch(Dispatchers.Main) {
                    showConfirmationDialog(baseDomain)
                }
            }
        }
    }

    private fun showConfirmationDialog(baseDomain: String) {
        AlertDialog.Builder(context).apply {
            setMessage(
                HtmlCompat.fromHtml(
                    context.getString(
                        R.string.confirm_clear_site_data,
                        baseDomain
                    ),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            )

            setNegativeButton(R.string.delete_browsing_data_prompt_cancel) { it: DialogInterface, _ ->
                it.cancel()
            }

            setPositiveButton(R.string.delete_browsing_data_prompt_allow) { it: DialogInterface, _ ->
                it.dismiss()
                interactor.onClearSiteDataClicked(baseDomain)
            }
            create()
        }.show()
    }
}
