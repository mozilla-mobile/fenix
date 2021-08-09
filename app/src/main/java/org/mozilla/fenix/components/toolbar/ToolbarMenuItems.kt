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
import org.mozilla.fenix.R
import org.mozilla.fenix.components.accounts.FenixAccountManager
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.theme.ThemeManager

/**
 * Defines commonly used items for use in the main toolbar menu.
 *
 * @param context The [Context].
 * @param store Reference to the application's [BrowserStore].
 * @param hasAccountProblem If true, there was a problem signing into the Firefox account.
 * @param onItemTapped Called when a menu item is tapped.
 * @param primaryTextColor The text color used for the items.
 * @param accentTextColor The accent color used for the items.
 */
@Suppress("LargeClass", "LongParameterList")
open class ToolbarMenuItems(
    private val context: Context,
    private val store: BrowserStore,
    private val accountManager: FenixAccountManager,
    hasAccountProblem: Boolean = false,
    private val onItemTapped: (ToolbarMenu.Item) -> Unit = {},
    @ColorRes val primaryTextColor: Int,
    @ColorRes val accentTextColor: Int
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
        longClickListener = { onItemTapped.invoke(ToolbarMenu.Item.Back(viewHistory = true)) }
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.Back(viewHistory = false))
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
            onItemTapped.invoke(ToolbarMenu.Item.Forward(viewHistory = true))
        }
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.Forward(viewHistory = false))
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
        longClickListener = { onItemTapped.invoke(ToolbarMenu.Item.Reload(bypassCache = true)) }
    ) {
        if (selectedSession?.content?.loading == true) {
            onItemTapped.invoke(ToolbarMenu.Item.Stop)
        } else {
            onItemTapped.invoke(ToolbarMenu.Item.Reload(bypassCache = false))
        }
    }

    val shareItem = BrowserMenuItemToolbar.Button(
        imageResource = R.drawable.ic_share,
        contentDescription = context.getString(R.string.browser_menu_share),
        iconTintColorResource = primaryTextColor,
        listener = {
            onItemTapped.invoke(ToolbarMenu.Item.Share)
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
        onItemTapped.invoke(ToolbarMenu.Item.NewTab)
    }

    val historyItem = BrowserMenuImageText(
        context.getString(R.string.library_history),
        R.drawable.ic_history,
        primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.History)
    }

    val downloadsItem = BrowserMenuImageText(
        context.getString(R.string.library_downloads),
        R.drawable.ic_download,
        primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.Downloads)
    }

    private fun getSyncItemTitle(): String {
        val authenticatedAccount = accountManager.authenticatedAccount
        val email = accountManager.accountProfileEmail

        return if (authenticatedAccount && !email.isNullOrEmpty()) {
            email
        } else {
            context.getString(R.string.sync_menu_sign_in)
        }
    }

    val syncMenuItem = BrowserMenuImageText(
        getSyncItemTitle(),
        R.drawable.ic_signed_out,
        primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.SyncAccount(accountManager.signedInToFxa()))
    }

    val findInPageItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_find_in_page),
        imageResource = R.drawable.mozac_ic_search,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.FindInPage)
    }

    val requestDesktopSiteItem = BrowserMenuImageSwitch(
        imageResource = R.drawable.ic_desktop,
        label = context.getString(R.string.browser_menu_desktop_site),
        initialState = {
            selectedSession?.content?.desktopMode ?: false
        }
    ) { checked ->
        onItemTapped.invoke(ToolbarMenu.Item.RequestDesktop(checked))
    }

    var customizeReaderView = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_customize_reader_view),
        imageResource = R.drawable.ic_readermode_appearance,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.CustomizeReaderView)
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
        onItemTapped.invoke(ToolbarMenu.Item.OpenInApp)
    }

    val addToHomeScreenItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_add_to_homescreen),
        imageResource = R.drawable.ic_add_to_homescreen,
        iconTintColorResource = primaryTextColor,
        isCollapsingMenuLimit = true
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.AddToHomeScreen)
    }

    val saveToCollectionItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_save_to_collection_2),
        imageResource = R.drawable.ic_tab_collection,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.SaveToCollection)
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
        onItemTapped.invoke(ToolbarMenu.Item.Settings)
    }

    val addToTopSitesItem = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_add_to_top_sites),
        imageResource = R.drawable.ic_top_sites,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.AddToTopSites)
    }

    val deleteDataOnQuitItem = BrowserMenuImageText(
        label = context.getString(R.string.delete_browsing_data_on_quit_action),
        imageResource = R.drawable.ic_exit,
        iconTintColorResource = primaryTextColor
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.Quit)
    }
}
