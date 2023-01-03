/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Model containing data for the [OnboardingPage].
 *
 * @param image [DrawableRes] displayed on the page.
 * @param title [StringRes] of the onboarding headline text.
 * @param description [StringRes] of the onboarding body text.
 * @param primaryButtonText [StringRes] of the primary button text.
 * @param secondaryButtonText [StringRes] of the secondary button text.
 * @param onRecordImpressionEvent Callback for recording impression event.
 */
data class OnboardingPageState(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val primaryButtonText: Int,
    @StringRes val secondaryButtonText: Int? = null,
    val onRecordImpressionEvent: () -> Unit,
)
