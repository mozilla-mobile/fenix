/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor
import java.util.UUID

/**
 * Handles the creation of pinned shortcuts.
 */
object PrivateShortcutCreateManager {

    fun createPrivateShortcut(context: Context) {

        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return
        val intent = getHomeScreenActivityIntent(context)
        val shortcut = getShortcut(context, intent)
        val intentSender = getIntentSender(context)
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, intentSender)

    }

    fun getShortcut(context: Context, intent: Intent): ShortcutInfoCompat {

        val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher_private_round)
        val shortcut = ShortcutInfoCompat.Builder(context, UUID.randomUUID().toString())
            .setShortLabel(
                context.getString(
                    R.string.app_name_private_5,
                    context.getString(R.string.app_name)
                )
            )
            .setLongLabel(
                context.getString(
                    R.string.app_name_private_5,
                    context.getString(R.string.app_name)
                )
            )
            .setIcon(icon)
            .setIntent(intent)
            .build()


        return shortcut
    }

    fun getHomeScreenActivityIntent(context: Context): Intent {

        return Intent(context, HomeActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(HomeActivity.PRIVATE_BROWSING_MODE, true)
            .putExtra(
                HomeActivity.OPEN_TO_SEARCH,
                StartSearchIntentProcessor.PRIVATE_BROWSING_PINNED_SHORTCUT
            )

    }

    fun getIntentSender(context: Context): IntentSender {

        val homeScreenIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val intentSender = PendingIntent
            .getActivity(context, 0, homeScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            .intentSender

        return intentSender

    }
}
