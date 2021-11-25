/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.onboarding.OnboardingFragmentState
import org.mozilla.fenix.onboarding.interactor.OnboardingInteractor
import org.mozilla.fenix.onboarding.toOnboardingItems

class OnboardingView(
    val containerView: RecyclerView,
    val interactor: OnboardingInteractor,
) {

    private val onboardingAdapter = OnboardingAdapter(interactor)

    init {
        containerView.apply {
            adapter = onboardingAdapter
            layoutManager = LinearLayoutManager(containerView.context)
        }
    }

    fun update(onboardingState: OnboardingFragmentState) {
        onboardingAdapter.submitList(onboardingState.state.toOnboardingItems())
    }
}
