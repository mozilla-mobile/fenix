/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import org.mozilla.fenix.onboarding.view.OnboardingAdapterItem

/**
 * Describes various onboarding states.
 */
sealed class OnboardingState {
    // Signed out, without an option to auto-login using a shared FxA account.
    object SignedOutNoAutoSignIn : OnboardingState()
    // Signed in.
    object SignedIn : OnboardingState()
}

fun OnboardingState.toOnboardingItems(): List<OnboardingAdapterItem> {
    val items: MutableList<OnboardingAdapterItem> =
        mutableListOf(OnboardingAdapterItem.OnboardingHeader)

    items.addAll(
        listOf(
            OnboardingAdapterItem.OnboardingThemePicker,
            OnboardingAdapterItem.OnboardingToolbarPositionPicker,
            OnboardingAdapterItem.OnboardingTrackingProtection
        )
    )

    // Customize FxA items based on where we are with the account state:
    items.addAll(
        when (this) {
            OnboardingState.SignedOutNoAutoSignIn -> {
                listOf(
                    OnboardingAdapterItem.OnboardingManualSignIn
                )
            }
            OnboardingState.SignedIn -> listOf()
        }
    )

    items.addAll(
        listOf(
            OnboardingAdapterItem.OnboardingPrivacyNotice,
            OnboardingAdapterItem.OnboardingFinish,
            OnboardingAdapterItem.BottomSpacer
        )
    )

    return items
}
