/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.customtabs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BADGE_ICON_NONE
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.state.ExternalAppType
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.base.ids.cancel
import mozilla.components.support.base.ids.notify
import org.mozilla.fenix.R

/**
 * Displays a "Powered by Firefox Preview" notification when a Trusted Web Activity is running.
 */
class PoweredByNotification(
    private val applicationContext: Context,
    private val store: BrowserStore,
    private val customTabId: String
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        if (store.state.findCustomTab(customTabId)?.config?.externalAppType === ExternalAppType.TRUSTED_WEB_ACTIVITY) {
            NotificationManagerCompat.from(applicationContext)
                .notify(applicationContext, NOTIFICATION_TAG, buildNotification())
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        NotificationManagerCompat.from(applicationContext)
            .cancel(applicationContext, NOTIFICATION_TAG)
    }

    /**
     * Build the notification with site controls to be displayed while the web app is active.
     */
    private fun buildNotification(): Notification {
        val channelId = ensureChannelExists()

        with(applicationContext) {
            val appName = getString(R.string.app_name)
            return NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_status_logo)
                .setContentTitle(getString(R.string.browser_menu_powered_by2, appName))
                .setBadgeIconType(BADGE_ICON_NONE)
                .setColor(ContextCompat.getColor(this, R.color.primary_text_light_theme))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setShowWhen(false)
                .setOngoing(true)
                .build()
        }
    }

    /**
     * Make sure a notification channel for the powered by notifications exists.
     *
     * Returns the channel id to be used for notifications.
     */
    private fun ensureChannelExists(): String {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = applicationContext.getSystemService()!!

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                applicationContext.getString(R.string.mozac_feature_pwa_site_controls_notification_channel),
                NotificationManager.IMPORTANCE_MIN
            )

            notificationManager.createNotificationChannel(channel)
        }

        return NOTIFICATION_CHANNEL_ID
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "Powered By"
        private const val NOTIFICATION_TAG = "PoweredBy"
    }
}
