/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observer
import org.mozilla.fenix.home.OnboardingState
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingAutomaticSignInViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingFinishViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingHeaderViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingManualSignInViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingPrivacyNoticeViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingPrivateBrowsingViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingSectionHeaderViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingThemePickerViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingTrackingProtectionViewHolder

sealed class OnboardingItem(@LayoutRes val viewType: Int) {
    object OnboardingHeader : OnboardingItem(OnboardingHeaderViewHolder.LAYOUT_ID)
    data class OnboardingSectionHeader(
        val labelBuilder: (Context) -> String
    ) : OnboardingItem(OnboardingSectionHeaderViewHolder.LAYOUT_ID)
    object OnboardingManualSignIn : OnboardingItem(OnboardingManualSignInViewHolder.LAYOUT_ID)
    data class OnboardingAutomaticSignIn(
        val state: OnboardingState.SignedOutCanAutoSignIn
    ) : OnboardingItem(OnboardingAutomaticSignInViewHolder.LAYOUT_ID)
    object OnboardingThemePicker : OnboardingItem(OnboardingThemePickerViewHolder.LAYOUT_ID)
    object OnboardingTrackingProtection : OnboardingItem(OnboardingTrackingProtectionViewHolder.LAYOUT_ID)
    object OnboardingPrivateBrowsing : OnboardingItem(OnboardingPrivateBrowsingViewHolder.LAYOUT_ID)
    object OnboardingPrivacyNotice : OnboardingItem(OnboardingPrivacyNoticeViewHolder.LAYOUT_ID)
    object OnboardingFinish : OnboardingItem(OnboardingFinishViewHolder.LAYOUT_ID)
}

/**
 * Adapter to display onboarding views.
 */
class OnboardingAdapter(
    private val actionEmitter: Observer<SessionControlAction>
) : ListAdapter<OnboardingItem, RecyclerView.ViewHolder>(OnboardingItemDiffCallback) {

    // This method triggers the ComplexMethod lint error when in fact it's quite simple.
    @SuppressWarnings("ComplexMethod")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            OnboardingHeaderViewHolder.LAYOUT_ID -> OnboardingHeaderViewHolder(view)
            OnboardingSectionHeaderViewHolder.LAYOUT_ID -> OnboardingSectionHeaderViewHolder(view)
            OnboardingManualSignInViewHolder.LAYOUT_ID -> OnboardingManualSignInViewHolder(view)
            OnboardingAutomaticSignInViewHolder.LAYOUT_ID -> OnboardingAutomaticSignInViewHolder(view)
            OnboardingThemePickerViewHolder.LAYOUT_ID -> OnboardingThemePickerViewHolder(view)
            OnboardingTrackingProtectionViewHolder.LAYOUT_ID -> OnboardingTrackingProtectionViewHolder(view)
            OnboardingPrivateBrowsingViewHolder.LAYOUT_ID -> OnboardingPrivateBrowsingViewHolder(view)
            OnboardingPrivacyNoticeViewHolder.LAYOUT_ID -> OnboardingPrivacyNoticeViewHolder(view)
            OnboardingFinishViewHolder.LAYOUT_ID -> OnboardingFinishViewHolder(view, actionEmitter)
            else -> throw IllegalStateException()
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).viewType

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is OnboardingSectionHeaderViewHolder -> holder.bind(
                (item as OnboardingItem.OnboardingSectionHeader).labelBuilder
            )
            is OnboardingManualSignInViewHolder -> holder.bind()
            is OnboardingAutomaticSignInViewHolder -> holder.bind(
                (item as OnboardingItem.OnboardingAutomaticSignIn).state.withAccount
            )
        }
    }

    private object OnboardingItemDiffCallback : DiffUtil.ItemCallback<OnboardingItem>() {
        override fun areItemsTheSame(oldItem: OnboardingItem, newItem: OnboardingItem) =
            oldItem::class == newItem::class

        @Suppress("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: OnboardingItem, newItem: OnboardingItem) =
            oldItem == newItem
    }
}
