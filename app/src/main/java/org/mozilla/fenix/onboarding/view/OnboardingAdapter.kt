/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.home.BottomSpacerViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingFinishViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingHeaderViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingManualSignInViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingPrivacyNoticeViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingSectionHeaderViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingThemePickerViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingToolbarPositionPickerViewHolder
import org.mozilla.fenix.home.sessioncontrol.viewholders.onboarding.OnboardingTrackingProtectionViewHolder
import org.mozilla.fenix.onboarding.interactor.OnboardingInteractor

class OnboardingAdapter(
    private val interactor: OnboardingInteractor,
) : ListAdapter<OnboardingAdapterItem, RecyclerView.ViewHolder>(OnboardingAdapterItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            OnboardingHeaderViewHolder.LAYOUT_ID -> OnboardingHeaderViewHolder(view)
            OnboardingSectionHeaderViewHolder.LAYOUT_ID -> OnboardingSectionHeaderViewHolder(view)
            OnboardingManualSignInViewHolder.LAYOUT_ID -> OnboardingManualSignInViewHolder(view)
            OnboardingThemePickerViewHolder.LAYOUT_ID -> OnboardingThemePickerViewHolder(view)
            OnboardingTrackingProtectionViewHolder.LAYOUT_ID -> OnboardingTrackingProtectionViewHolder(
                view
            )
            OnboardingPrivacyNoticeViewHolder.LAYOUT_ID -> OnboardingPrivacyNoticeViewHolder(
                view,
                interactor
            )
            OnboardingFinishViewHolder.LAYOUT_ID -> OnboardingFinishViewHolder(view, interactor)
            OnboardingToolbarPositionPickerViewHolder.LAYOUT_ID -> OnboardingToolbarPositionPickerViewHolder(
                view)
            BottomSpacerViewHolder.LAYOUT_ID -> BottomSpacerViewHolder(view)
            else -> throw IllegalStateException("ViewType $viewType does not match a ViewHolder")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        when (holder) {
            is OnboardingSectionHeaderViewHolder -> holder.bind(
                (item as OnboardingAdapterItem.OnboardingSectionHeader).labelBuilder
            )
            is OnboardingManualSignInViewHolder -> holder.bind()
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).viewType
}

sealed class OnboardingAdapterItem(@LayoutRes val viewType: Int) {
    object OnboardingHeader : OnboardingAdapterItem(OnboardingHeaderViewHolder.LAYOUT_ID)

    data class OnboardingSectionHeader(
        val labelBuilder: (Context) -> String,
    ) : OnboardingAdapterItem(OnboardingSectionHeaderViewHolder.LAYOUT_ID) {
        override fun sameAs(other: OnboardingAdapterItem) =
            other is OnboardingSectionHeader && labelBuilder == other.labelBuilder
    }

    object OnboardingManualSignIn :
        OnboardingAdapterItem(OnboardingManualSignInViewHolder.LAYOUT_ID)

    object OnboardingThemePicker : OnboardingAdapterItem(OnboardingThemePickerViewHolder.LAYOUT_ID)
    object OnboardingTrackingProtection :
        OnboardingAdapterItem(OnboardingTrackingProtectionViewHolder.LAYOUT_ID)

    object OnboardingPrivacyNotice :
        OnboardingAdapterItem(OnboardingPrivacyNoticeViewHolder.LAYOUT_ID)

    object OnboardingFinish : OnboardingAdapterItem(OnboardingFinishViewHolder.LAYOUT_ID)
    object OnboardingToolbarPositionPicker :
        OnboardingAdapterItem(OnboardingToolbarPositionPickerViewHolder.LAYOUT_ID)

    object BottomSpacer : OnboardingAdapterItem(BottomSpacerViewHolder.LAYOUT_ID)

    /**
     * True if this item represents the same value as other.
     */
    open fun sameAs(other: OnboardingAdapterItem) = this::class == other::class

    open fun contentsSameAs(other: OnboardingAdapterItem) = this::class == other::class
}

class OnboardingAdapterItemDiffCallback : DiffUtil.ItemCallback<OnboardingAdapterItem>() {
    override fun areItemsTheSame(oldItem: OnboardingAdapterItem, newItem: OnboardingAdapterItem) =
        oldItem.sameAs(newItem)

    @Suppress("DiffUtilEquals")
    override fun areContentsTheSame(
        oldItem: OnboardingAdapterItem,
        newItem: OnboardingAdapterItem,
    ) = oldItem.contentsSameAs(newItem)
}
