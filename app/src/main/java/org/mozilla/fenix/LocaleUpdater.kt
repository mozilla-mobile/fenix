/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import mozilla.components.support.locale.LocaleManager
import java.util.*

/**
 * Listens for locale changes and updates app settings.
 */
class LocaleUpdater(private val context: Context) {
    private val localeChangedReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                updateBaseConfiguration(context, locale)
            }
        }
    }

    fun registerForUpdates() {
        context.registerReceiver(localeChangedReceiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED))
    }

    /**
     * Update the locale for the configuration of the app context's resources
     */
    @Suppress("Deprecation")
    fun updateBaseConfiguration(context: Context, locale: Locale) {
        val resources = context.applicationContext.resources
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}