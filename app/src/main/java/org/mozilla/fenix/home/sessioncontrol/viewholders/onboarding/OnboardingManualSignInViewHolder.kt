/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding

import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.onboarding_manual_signin.view.*
import mozilla.components.support.ktx.android.content.getDrawableWithTint
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelativeWithIntrinsicBounds
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.onboarding.OnboardingController
import org.mozilla.fenix.onboarding.OnboardingInteractor

class OnboardingManualSignInViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val headerText = view.header_text
    private var interactor: OnboardingInteractor

    init {
        interactor = OnboardingInteractor(OnboardingController(itemView.context as HomeActivity))

        view.turn_on_sync_button.setOnClickListener {
            it.context.components.analytics.metrics.track(Event.OnboardingManualSignIn)

            val directions = HomeFragmentDirections.actionGlobalTurnOnSync()
            Navigation.findNavController(view).navigate(directions)
        }

        view.learn_more.setOnClickListener {
            interactor.onLearnMoreClicked()
        }
    }

    fun bind() {
        val context = itemView.context

        headerText.text = context.getString(R.string.onboarding_firefox_account_header)
        val icon = context.getDrawableWithTint(
            R.drawable.ic_account,
            ContextCompat.getColor(context, R.color.white_color)
        )
        val iconSize =
            context.resources.getDimensionPixelSize(R.dimen.onboarding_header_icon_height_width)

        icon?.setBounds(0, 0, iconSize, iconSize)
        headerText.putCompoundDrawablesRelativeWithIntrinsicBounds(start = icon)


    }

    private fun openWebsite(addonSiteUrl: Uri) {
        (itemView.context as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = addonSiteUrl.toString(),
            newTab = true,
            from = BrowserDirection.FromAddonDetailsFragment
        )
    }

    companion object {
        const val LAYOUT_ID = R.layout.onboarding_manual_signin
    }
}
