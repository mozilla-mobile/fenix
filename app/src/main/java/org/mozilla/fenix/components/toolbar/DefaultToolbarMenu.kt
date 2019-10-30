/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuHighlightableItem
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.asActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.theme.ThemeManager
import org.mozilla.fenix.utils.Settings

class DefaultToolbarMenu(
    private val context: Context,
    private val hasAccountProblem: Boolean = false,
    private val requestDesktopStateProvider: () -> Boolean = { false },
    private val onItemTapped: (ToolbarMenu.Item) -> Unit = {},
    readerModeStateProvider: () -> Boolean = { false }
) : ToolbarMenu {

    override val menuBuilder by lazy { BrowserMenuBuilder(menuItems, endOfMenuAlwaysVisible = true) }

    override val menuToolbar by lazy {
        val forward = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
            primaryContentDescription = context.getString(R.string.browser_menu_forward),
            primaryImageTintResource = ThemeManager.resolveAttribute(
                R.attr.primaryText,
                context
            ),
            isInPrimaryState = {
                context.components.core.sessionManager.selectedSession?.canGoForward ?: true
            },
            secondaryImageTintResource = ThemeManager.resolveAttribute(
                R.attr.disabled,
                context
            ),
            disableInSecondaryState = true
        ) {
            onItemTapped.invoke(ToolbarMenu.Item.Forward)
        }

        val refresh = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_refresh,
            primaryContentDescription = context.getString(R.string.browser_menu_refresh),
            primaryImageTintResource = ThemeManager.resolveAttribute(
                R.attr.primaryText,
                context
            ),
            isInPrimaryState = {
                val loading = context.components.core.sessionManager.selectedSession?.loading
                loading == false
            },
            secondaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_stop,
            secondaryContentDescription = context.getString(R.string.browser_menu_stop),
            secondaryImageTintResource = ThemeManager.resolveAttribute(
                R.attr.primaryText,
                context
            ),
            disableInSecondaryState = false
        ) {
            if (context.components.core.sessionManager.selectedSession?.loading == true) {
                onItemTapped.invoke(ToolbarMenu.Item.Stop)
            } else {
                onItemTapped.invoke(ToolbarMenu.Item.Reload)
            }
        }

        val share = BrowserMenuItemToolbar.Button(
            imageResource = R.drawable.mozac_ic_share,
            contentDescription = context.getString(R.string.browser_menu_share),
            iconTintColorResource = primaryTextColor(),
            listener = {
                onItemTapped.invoke(ToolbarMenu.Item.Share)
            }
        )

        val bookmark = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = R.drawable.ic_bookmark_filled,
            primaryContentDescription = context.getString(R.string.browser_menu_edit_bookmark),
            primaryImageTintResource = ThemeManager.resolveAttribute(
                R.attr.primaryText,
                context
            ),
            isInPrimaryState = {
                // TODO true if bookmarked
                /*
                How am I going to get this to work? `bookmarksStorage.getBookmarksWithUrl(currentUrl ?: "")`
                is taking 10-50 MS to complete, there's no way we can run that blocking
                 */
                true
            },
            secondaryImageResource = R.drawable.ic_bookmark_outline,
            secondaryContentDescription = context.getString(R.string.browser_menu_bookmark),
            secondaryImageTintResource = ThemeManager.resolveAttribute(
                R.attr.disabled,
                context
            ),
            disableInSecondaryState = false
        ) {
            // TODO send current state?
            onItemTapped.invoke(ToolbarMenu.Item.Bookmark)
        }

        BrowserMenuItemToolbar(listOf(forward, bookmark, share, refresh))
    }

    private val menuItems by lazy {
        val browsingModeIsNormal = (context.asActivity() as? HomeActivity)
            ?.browsingModeManager?.mode == BrowsingMode.Normal
        val shouldDeleteDataOnQuit = Settings.getInstance(context)
            .shouldDeleteBrowsingDataOnQuit

        listOfNotNull(
            help,
            settings,
            library,
            desktopMode,
            addToHomescreen,
            findInPage,
            privateTab,
            newTab,
            reportIssue,
            if (browsingModeIsNormal) saveToCollection else null,
            if (shouldDeleteDataOnQuit) deleteDataOnQuit else null,
            readerMode, // TODO only sometimes add
            openInApp, // TODO only sometimes add
            BrowserMenuDivider(),
            menuToolbar
        )
    }

    private val help = BrowserMenuImageText(
        context.getString(R.string.browser_menu_help),
        R.drawable.ic_help,
        primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.Help)
    }

    private val settings = BrowserMenuHighlightableItem(
        label = context.getString(R.string.browser_menu_settings),
        imageResource = R.drawable.ic_settings,
        iconTintColorResource = if (hasAccountProblem)
            R.color.sync_error_text_color else
            primaryTextColor(),
        textColorResource = if (hasAccountProblem)
            R.color.sync_error_text_color else
            primaryTextColor(),
        highlight = if (hasAccountProblem) {
            BrowserMenuHighlightableItem.Highlight(
                endImageResource = R.drawable.ic_alert,
                backgroundResource = R.drawable.sync_error_background_with_ripple,
                colorResource = R.color.sync_error_background_color
            )
        } else null
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.Settings)
    }

    private val library = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_your_library),
        imageResource = R.drawable.ic_library,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.Library)
    }

    private val desktopMode = BrowserMenuImageSwitch(
        imageResource = R.drawable.ic_desktop,
        label = context.getString(R.string.browser_menu_desktop_site),
        initialState = requestDesktopStateProvider
    ) { checked ->
        onItemTapped.invoke(ToolbarMenu.Item.RequestDesktop(checked))
    }

    private val addToHomescreen = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_add_to_homescreen),
        imageResource = R.drawable.ic_add_to_homescreen,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.AddToHomeScreen)
    }.apply {
        visible = ::shouldShowAddToHomescreen
    }

    private fun shouldShowAddToHomescreen(): Boolean {
        return context.components.useCases.webAppUseCases.isPinningSupported() &&
                context.components.core.sessionManager.selectedSession != null
    }

    private val findInPage = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_find_in_page),
        imageResource = R.drawable.mozac_ic_search,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.FindInPage)
    }

    private val privateTab = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_private_tab),
        imageResource = R.drawable.ic_private_browsing,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.NewPrivateTab)
    }

    private val newTab = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_new_tab),
        imageResource = R.drawable.ic_new,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.NewTab)
    }

    private val reportIssue = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_report_issue),
        imageResource = R.drawable.ic_report_issues,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.ReportIssue)
    }

    private val saveToCollection = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_save_to_collection),
        imageResource = R.drawable.ic_tab_collection,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.SaveToCollection)
    }

    private val deleteDataOnQuit = BrowserMenuImageText(
        label = context.getString(R.string.delete_browsing_data_on_quit_action),
        imageResource = R.drawable.ic_exit,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.Quit)
    }

    private val readerMode = BrowserMenuImageSwitch(
        label = context.getString(R.string.quick_action_read),
        imageResource = R.drawable.ic_readermode,
        initialState = readerModeStateProvider
    ) { checked ->
        onItemTapped.invoke(ToolbarMenu.Item.ReaderMode(checked))
    }

    private val openInApp = BrowserMenuImageText(
        label = context.getString(R.string.quick_action_open_app_link),
        imageResource = R.drawable.ic_library,
        iconTintColorResource = primaryTextColor()
        ) {
        onItemTapped.invoke(ToolbarMenu.Item.OpenInApp)
    }

    private fun primaryTextColor() = ThemeManager.resolveAttribute(R.attr.primaryText, context)
}
