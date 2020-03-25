/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.tips

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.asActivity
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.whatsnew.WhatsNew

enum class TipPriority {
    HIGH, // Not dismissable, colored background
    MEDIUM,
    LOW
}

open class Tip(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val button: Int,
    val shouldColorIcon: Boolean = true,
    val priority: TipPriority,
    val action: () -> Unit
)

class TipManager(val context: Context) {
    private var tipList = mutableListOf<Tip>()

    private val defaultBrowserTip =
        Tip(
            icon = R.drawable.ic_firefox,
            title = R.string.tip_default_browser_header,
            description = R.string.tip_default_browser_description,
            button = R.string.tip_default_browser_button,
            shouldColorIcon = false,
            priority = TipPriority.LOW
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                context.startActivity(intent)
            } else {
                (context.asActivity() as HomeActivity).openToBrowserAndLoad(
                    searchTermOrURL = SupportUtils.getSumoURLForTopic(
                        context,
                        SupportUtils.SumoTopic.SET_AS_DEFAULT_BROWSER
                    ),
                    newTab = true,
                    from = BrowserDirection.FromHome
                )
            }
        }

    private val openLinksInPrivateBrowsingTip =
        // TODO: Perhaps we should have a toggle here?
        Tip(
            icon = R.drawable.ic_private_browsing,
            title = R.string.tip_always_private_tab_header,
            description = R.string.tip_always_private_tab_description,
            button = R.string.tip_always_private_tab_buton,
            priority = TipPriority.LOW
        ) { context.settings().openLinksInAPrivateTab = true }

    private val whatsNewTip =
        Tip(
            icon = R.drawable.ic_whats_new,
            title = R.string.tip_whats_new_header,
            description = R.string.tip_whats_new_description,
            button = R.string.tip_whats_new_button,
            priority = TipPriority.MEDIUM
        ) {
            (context.asActivity() as HomeActivity).openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getWhatsNewUrl(context),
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }

    private val fenixMovingTip =
        Tip(
            icon = R.drawable.mozac_ic_warning,
            title = R.string.tip_fenix_moving_header,
            description = R.string.tip_fenix_moving_description,
            button = R.string.tip_fenix_moving_button,
            priority = TipPriority.HIGH
        ) {
            (context.asActivity() as HomeActivity).openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.FENIX_PLAY_STORE_URL,
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }

    init { populateTipList() }

    // Returns a tip, critical message, or nothing if there are no tips available
    fun getTipOrCriticalMessage(): List<Tip> {
        if (tipList.isEmpty() || !context.settings().shouldDisplayTips()) { return listOf() }

        tipList.forEach {
            Log.d("Sawyer", "element: $it")
            if (it.priority == TipPriority.HIGH) { return listOf(it) }
        }

        tipList.forEach {
            if (it.priority == TipPriority.MEDIUM) { return listOf(it) }
        }

        return listOf(tipList.random())
    }

    fun getAllTips(): List<Tip> = tipList

    // In an ideal world we could populate tips from a server
    private fun populateTipList() {
        if (context.settings().isDefaultBrowser()) {
            tipList.add(defaultBrowserTip)
        }

        if (WhatsNew.shouldHighlightWhatsNew(context)) {
            tipList.add(whatsNewTip)
        }

        tipList.add(openLinksInPrivateBrowsingTip)
    }
}