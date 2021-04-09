/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat.getColor
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.item.BrowserMenuHighlightableItem
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.FeatureFlags.tabsTrayRewrite
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.theme.ThemeManager

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
open class ToolbarMenuItems(
    private val context: Context,
    private val store: BrowserStore,
    hasAccountProblem: Boolean = false,
    private val onItemTapped: (ToolbarMenu.DefaultItem) -> Unit = {},
    @ColorRes val primaryTextColor: Int,
    @ColorRes val accentBrightTextColor: Int
) {
    private val selectedSession: TabSessionState?
        get() = store.state.selectedTab

    val backNavButton = BrowserMenuItemToolbar.TwoStateButton(
        primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_back,
        primaryContentDescription = context.getString(R.string.browser_menu_back),
        primaryImageTintResource = primaryTextColor,
        isInPrimaryState = {
            selectedSession?.content?.canGoBack ?: true
        },
        secondaryImageTintResource = ThemeManager.resolveAttribute(R.attr.disabled, context),
        disableInSecondaryState = true,
        longClickListener = { onItemTapped.invoke(ToolbarMenu.DefaultItem.Back(viewHistory = true)) }
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.Back(viewHistory = false))
    }

    val forwardNavButton = BrowserMenuItemToolbar.TwoStateButton(
        primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
        primaryContentDescription = context.getString(R.string.browser_menu_forward),
        primaryImageTintResource = primaryTextColor,
        isInPrimaryState = {
            selectedSession?.content?.canGoForward ?: true
        },
        secondaryImageTintResource = ThemeManager.resolveAttribute(R.attr.disabled, context),
        disableInSecondaryState = true,
        longClickListener = {
            onItemTapped.invoke(ToolbarMenu.DefaultItem.Forward(viewHistory = true))
        }
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.Forward(viewHistory = false))
    }

    val refreshNavButton = BrowserMenuItemToolbar.TwoStateButton(
        primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_refresh,
        primaryContentDescription = context.getString(R.string.browser_menu_refresh),
        primaryImageTintResource = primaryTextColor,
        isInPrimaryState = {
            selectedSession?.content?.loading == false
        },
        secondaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_stop,
        secondaryContentDescription = context.getString(R.string.browser_menu_stop),
        secondaryImageTintResource = primaryTextColor,
        disableInSecondaryState = false,
        longClickListener = { onItemTapped.invoke(ToolbarMenu.DefaultItem.Reload(bypassCache = true)) }
    ) {
        if (selectedSession?.content?.loading == true) {
            onItemTapped.invoke(ToolbarMenu.DefaultItem.Stop)
        } else {
            onItemTapped.invoke(ToolbarMenu.DefaultItem.Reload(bypassCache = false))
        }
    }

    val shareItem = BrowserMenuItemToolbar.Button(
        imageResource = R.drawable.ic_share_filled,
        contentDescription = context.getString(R.string.browser_menu_share),
        iconTintColorResource = primaryTextColor,
        listener = {
            onItemTapped.invoke(ToolbarMenu.DefaultItem.Share)
        }
    )

    val installPwaToHomescreen = BrowserMenuHighlightableItem(
            label = context.getString(R.string.browser_menu_install_on_homescreen),
            startImageResource = R.drawable.ic_add_to_homescreen,
            iconTintColorResource = primaryTextColor,
            highlight = BrowserMenuHighlight.LowPriority(
                label = context.getString(R.string.browser_menu_install_on_homescreen),
                notificationTint = getColor(context, R.color.whats_new_notification_color)
            ),
            isHighlighted = {
                !context.settings().installPwaOpened
            }
        )

    val newTabItem = BrowserMenuImageText(
        context.getString(R.string.library_new_tab),
        R.drawable.ic_new,
        primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.NewTab)
    }

    val historyItem = BrowserMenuImageText(
        context.getString(R.string.library_history),
        R.drawable.ic_history,
        primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.History)
    }

    val downloadsItem = BrowserMenuImageText(
        context.getString(R.string.library_downloads),
        R.drawable.ic_download,
        primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.Downloads)
    }

    val accountManager = context.components.backgroundServices.accountManager
    val account = accountManager.authenticatedAccount()
    val syncItemTitle = if (account != null && accountManager.accountProfile()?.email != null) {
        context.getString(R.string.sync_signed_as, accountManager.accountProfile()?.email)
    } else {
        context.getString(R.string.sync_menu_sign_in)
    }

    val syncedTabsOrSignInItem = if (tabsTrayRewrite) {
        // if synced tabs are being shown in tabs tray, show sync sign in here.
        BrowserMenuImageText(
            syncItemTitle,
            R.drawable.ic_synced_tabs,
            primaryTextColor
        ) {
            onItemTapped.invoke(ToolbarMenu.DefaultItem.SyncAccount)
        }
    } else {
        // if synced tabs are not shown in tabs tray, they should be shown here.
        BrowserMenuImageText(
            context.getString(R.string.synced_tabs),
            R.drawable.ic_synced_tabs,
            primaryTextColor
        ) {
            onItemTapped.invoke(ToolbarMenu.DefaultItem.SyncedTabs)
        }
    }

    val oldSyncedTabsItem = BrowserMenuImageText(
        label = context.getString(R.string.synced_tabs),
        imageResource = R.drawable.ic_synced_tabs,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.SyncedTabs)
    }

    val findInPageItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_find_in_page),
        imageResource = R.drawable.mozac_ic_search,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.FindInPage)
    }

    val requestDesktopSiteItem = BrowserMenuImageSwitch(
        imageResource = R.drawable.ic_desktop,
        label = context.getString(R.string.browser_menu_desktop_site),
        initialState = {
            selectedSession?.content?.desktopMode ?: false
        }
    ) { checked ->
        onItemTapped.invoke(ToolbarMenu.DefaultItem.RequestDesktop(checked))
    }

    var customizeReaderView = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_customize_reader_view),
        imageResource = R.drawable.ic_readermode_appearance,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.CustomizeReaderView)
    }

    val openInAppItem = BrowserMenuHighlightableItem(
        label = context.getString(R.string.browser_menu_open_app_link),
        startImageResource = R.drawable.ic_open_in_app,
        iconTintColorResource = primaryTextColor,
        highlight = BrowserMenuHighlight.LowPriority(
            label = context.getString(R.string.browser_menu_open_app_link),
            notificationTint = getColor(context, R.color.whats_new_notification_color)
        ),
        isHighlighted = { !context.settings().openInAppOpened }
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.OpenInApp)
    }

    val addToHomeScreenItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_add_to_homescreen),
        imageResource = R.drawable.ic_add_to_homescreen,
        iconTintColorResource = primaryTextColor,
        isCollapsingMenuLimit = true
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.AddToHomeScreen)
    }

    val saveToCollectionItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_save_to_collection_2),
        imageResource = R.drawable.ic_tab_collection,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.SaveToCollection)
    }

    val settingsItem = BrowserMenuHighlightableItem(
        label = context.getString(R.string.browser_menu_settings),
        startImageResource = R.drawable.ic_settings,
        iconTintColorResource = if (hasAccountProblem)
            ThemeManager.resolveAttribute(R.attr.syncDisconnected, context) else
            primaryTextColor,
        textColorResource = if (hasAccountProblem)
            ThemeManager.resolveAttribute(R.attr.primaryText, context) else
            primaryTextColor,
        highlight = BrowserMenuHighlight.HighPriority(
            endImageResource = R.drawable.ic_sync_disconnected,
            backgroundTint = context.getColorFromAttr(R.attr.syncDisconnectedBackground),
            canPropagate = false
        ),
        isHighlighted = { hasAccountProblem }
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.Settings)
    }

    val addToTopSitesItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_add_to_top_sites),
        imageResource = R.drawable.ic_top_sites,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.AddToTopSites)
    }

    val deleteDataOnQuitItem = BrowserMenuImageText(
        label = context.getString(R.string.delete_browsing_data_on_quit_action),
        imageResource = R.drawable.ic_exit,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.Quit)
    }

    val oldAddToHomescreenItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_add_to_homescreen),
        imageResource = R.drawable.ic_add_to_homescreen,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.AddToHomeScreen)
    }

    val oldReaderViewAppearanceItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_read_appearance),
        imageResource = R.drawable.ic_readermode_appearance,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.DefaultItem.CustomizeReaderView)
    }
}
