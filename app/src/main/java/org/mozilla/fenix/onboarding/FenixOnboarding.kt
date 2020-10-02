/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.intPreference
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components

class FenixOnboarding(context: Context) : PreferencesHolder {

    private val metrics = context.components.analytics.metrics
    override val preferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME_ONBOARDING_KEY,
        Context.MODE_PRIVATE
    )

    private var onboardedVersion by intPreference(LAST_VERSION_ONBOARDING_KEY, default = 0)

    fun finish() {
        onboardedVersion = CURRENT_ONBOARDING_VERSION
        metrics.track(Event.DismissedOnboarding)
    }

    fun userHasBeenOnboarded() = onboardedVersion == CURRENT_ONBOARDING_VERSION

    companion object {
        /**
         * The current onboarding version. When incremented,
         * users who were previously onboarded will be show the onboarding again.
         */
        @VisibleForTesting
        internal const val CURRENT_ONBOARDING_VERSION = 1

        /**
         * Name of the shared preferences file.
         */
        private const val PREF_NAME_ONBOARDING_KEY = "fenix.onboarding"

        /**
         * Key for [onboardedVersion].
         */
        @VisibleForTesting
        internal const val LAST_VERSION_ONBOARDING_KEY = "fenix.onboarding.last_version"
    }
}
