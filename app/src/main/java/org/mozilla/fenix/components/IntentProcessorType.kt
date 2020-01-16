/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Intent
import mozilla.components.feature.intent.processing.IntentProcessor
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.migration.MigrationProgressActivity
import org.mozilla.fenix.customtabs.ExternalAppBrowserActivity

enum class IntentProcessorType {
    EXTERNAL_APP, NEW_TAB, MIGRATION, OTHER;

    /**
     * The destination activity based on this intent
     */
    val activityClassName: String
        get() = when (this) {
            EXTERNAL_APP -> ExternalAppBrowserActivity::class.java.name
            NEW_TAB, OTHER -> HomeActivity::class.java.name
            MIGRATION -> MigrationProgressActivity::class.java.name
        }

    /**
     * Should this intent automatically navigate to the browser?
     */
    fun shouldOpenToBrowser(intent: Intent): Boolean = when (this) {
        EXTERNAL_APP -> true
        NEW_TAB -> intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
        MIGRATION, OTHER -> false
    }
}

/**
 * Classifies the [IntentProcessorType] based on the [IntentProcessor] that handled the [Intent].
 */
fun IntentProcessors.getType(processor: IntentProcessor?) = when {
    migrationIntentProcessor == processor -> IntentProcessorType.MIGRATION
    externalAppIntentProcessors.contains(processor) ||
            customTabIntentProcessor == processor ||
            privateCustomTabIntentProcessor == processor -> IntentProcessorType.EXTERNAL_APP
    intentProcessor == processor ||
            privateIntentProcessor == processor -> IntentProcessorType.NEW_TAB
    else -> IntentProcessorType.OTHER
}
