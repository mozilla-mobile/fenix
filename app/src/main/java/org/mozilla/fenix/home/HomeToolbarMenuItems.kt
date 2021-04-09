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
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.state.store.BrowserStore
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.ToolbarMenu
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.theme.ThemeManager
import org.mozilla.fenix.whatsnew.WhatsNew

/**
 * Defines commonly used items for use in the main toolbar menu.
 * @param context The [Context].
 * @param store Reference to the application's [BrowserStore].
 * @param hasAccountProblem If true, there was a problem signing into the Firefox account.
 * @param onItemTapped Called when a menu item is tapped.
 * @param primaryTextColor The text color used for the items.
 * @param accentBrightTextColor The accent color used for the items.
 */
@Suppress("LargeClass", "LongParameterList")
open class HomeToolbarMenuItems(
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {},
    @ColorRes val primaryTextColor: Int
) {
    val backNavButton = BrowserMenuItemToolbar.TwoStateButton(
        primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_back,
        primaryContentDescription = context.getString(R.string.browser_menu_back),
        primaryImageTintResource = primaryTextColor,
        isInPrimaryState = { false },
        secondaryImageTintResource = ThemeManager.resolveAttribute(R.attr.disabled, context),
        disableInSecondaryState = true,
        longClickListener = {
            onItemTapped.invoke(Item.Back(viewHistory = true))
        }
    ) {
        onItemTapped.invoke(Item.Back(viewHistory = false))
    }

    val forwardNavButton = BrowserMenuItemToolbar.TwoStateButton(
        primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
        primaryContentDescription = context.getString(R.string.browser_menu_forward),
        primaryImageTintResource = primaryTextColor,
        isInPrimaryState = { false },
        secondaryImageTintResource = ThemeManager.resolveAttribute(R.attr.disabled, context),
        disableInSecondaryState = true,
        longClickListener = {
            onItemTapped.invoke(Item.Forward(viewHistory = true))
        }
    ) {
        onItemTapped.invoke(Item.Forward(viewHistory = false))
    }

    val downloadsItem = BrowserMenuImageText(
        context.getString(R.string.library_downloads),
        R.drawable.ic_download,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.Downloads)
    }

    val accountManager = context.components.backgroundServices.accountManager
    val account = accountManager.authenticatedAccount()
    val syncItemTitle = if (account != null && accountManager.accountProfile()?.email != null) {
        context.getString(R.string.sync_signed_as, accountManager.accountProfile()?.email)
    } else {
        context.getString(R.string.sync_menu_sign_in)
    }

    val syncedTabsItem = BrowserMenuImageText(
        context.getString(R.string.synced_tabs),
        R.drawable.ic_synced_tabs,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.SyncTabs)
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
