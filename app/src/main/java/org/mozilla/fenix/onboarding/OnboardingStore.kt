/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

class OnboardingStore(
    initialState: OnboardingFragmentState = OnboardingFragmentState(),
) : Store<OnboardingFragmentState, OnboardingFragmentAction>(
    initialState = initialState,
    reducer = ::onboardingFragmentStateReducer
)

/**
 * The state for [OnboardingFragment].
 *
 * @property state
 */
data class OnboardingFragmentState(
    val state: OnboardingState = OnboardingState.SignedOutNoAutoSignIn,
) : State

/**
 * TODO
 */
sealed class OnboardingFragmentAction : Action {
    /**
     * TODO
     */
    data class UpdateState(val state: OnboardingState) : OnboardingFragmentAction()
}

/**
 * TODO
 */
private fun onboardingFragmentStateReducer(
    state: OnboardingFragmentState,
    action: OnboardingFragmentAction,
): OnboardingFragmentState {
    return when (action) {
        is OnboardingFragmentAction.UpdateState -> state.copy(state = action.state)
    }
}
