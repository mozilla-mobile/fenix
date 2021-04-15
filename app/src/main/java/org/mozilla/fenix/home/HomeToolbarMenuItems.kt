/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat.getColor
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.item.BrowserMenuHighlightableItem
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import mozilla.components.browser.menu.item.BrowserMenuImageText
import org.mozilla.fenix.R
import org.mozilla.fenix.components.accounts.FenixAccountManager
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.home.HomeMenu.Item
import org.mozilla.fenix.whatsnew.WhatsNew

/**
 * Defines commonly used items for use in the homescreen toolbar menu.
 * @param context The [Context].
 * @param onItemTapped Called when a menu item is tapped.
 * @param accountManager Used to manage sync account state.
 * @param primaryTextColor The text color used for the items.
 */
open class HomeToolbarMenuItems(
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {},
    private val accountManager: FenixAccountManager,
    @ColorRes val primaryTextColor: Int
) {
    val downloadsItem = BrowserMenuImageText(
        context.getString(R.string.library_downloads),
        R.drawable.ic_download,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.Downloads)
    }

    private fun getSyncItemTitle(): String {
        return accountManager.getAuthAccountEmail() ?: context.getString(R.string.sync_menu_sign_in)
    }

    val syncSignInItem = BrowserMenuImageText(
        getSyncItemTitle(),
        R.drawable.ic_synced_tabs,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.SyncAccount)
    }

    val oldSyncedTabsItem = BrowserMenuImageText(
        label = context.getString(R.string.synced_tabs),
        imageResource = R.drawable.ic_synced_tabs,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(Item.SyncedTabs)
    }

    val helpItem = BrowserMenuImageText(
        context.getString(R.string.browser_menu_help),
        R.drawable.ic_help,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.Help)
    }

    val requestDesktopSiteItem = BrowserMenuImageSwitch(
        imageResource = R.drawable.ic_desktop,
        label = context.getString(R.string.browser_menu_desktop_site),
        initialState = { context.settings().openNextTabInDesktopMode }
    ) { checked ->
        onItemTapped.invoke(Item.DesktopMode(checked))
    }

    val extensionsItem = BrowserMenuImageText(
        context.getString(R.string.browser_menu_add_ons),
        R.drawable.ic_addons_extensions,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.Extensions)
    }

    val settingsItem = BrowserMenuImageText(
        context.getString(R.string.browser_menu_settings),
        R.drawable.ic_settings,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.Settings)
    }

    val oldWhatsNewItem = BrowserMenuHighlightableItem(
        context.getString(R.string.browser_menu_whats_new),
        R.drawable.ic_whats_new,
        iconTintColorResource = primaryTextColor,
        highlight = BrowserMenuHighlight.LowPriority(
            notificationTint = getColor(context, R.color.whats_new_notification_color)
        ),
        isHighlighted = { WhatsNew.shouldHighlightWhatsNew(context) }
    ) {
        onItemTapped.invoke(Item.WhatsNew)
    }
}
