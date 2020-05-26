/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.customtabs

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat.getColor
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.item.BrowserMenuCategory
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuHighlightableItem
import mozilla.components.browser.menu.item.BrowserMenuImageSwitch
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.ToolbarMenu
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getStringWithArgSafe
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.theme.ThemeManager

/**
 * Builds the toolbar object used with the 3-dot menu in the custom tab browser fragment.
 * @param sessionManager Reference to the session manager that contains all tabs.
 * @param sessionId ID of the open custom tab session.
 * @param shouldReverseItems If true, reverse the menu items.
 * @param onItemTapped Called when a menu item is tapped.
 */
class CustomTabToolbarMenu(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val sessionId: String?,
    private val shouldReverseItems: Boolean,
    private val onItemTapped: (ToolbarMenu.Item) -> Unit = {}
) : ToolbarMenu {

    override val menuBuilder by lazy { BrowserMenuBuilder(menuItems) }

    /** Gets the current custom tab session */
    private val session: Session? get() = sessionId?.let { sessionManager.findSessionById(it) }
    private val appName = context.getString(R.string.app_name)

    override val menuToolbar by lazy {
        val back = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_back,
            primaryContentDescription = context.getString(R.string.browser_menu_back),
            primaryImageTintResource = primaryTextColor(),
            isInPrimaryState = {
                session?.canGoBack ?: true
            },
            secondaryImageTintResource = ThemeManager.resolveAttribute(
                R.attr.disabled,
                context
            ),
            disableInSecondaryState = true
        ) {
            onItemTapped.invoke(ToolbarMenu.Item.Back)
        }

        val forward = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
            primaryContentDescription = context.getString(R.string.browser_menu_forward),
            primaryImageTintResource = primaryTextColor(),
            isInPrimaryState = {
                session?.canGoForward ?: true
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
            primaryImageTintResource = primaryTextColor(),
            isInPrimaryState = {
                session?.loading == false
            },
            secondaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_stop,
            secondaryContentDescription = context.getString(R.string.browser_menu_stop),
            secondaryImageTintResource = primaryTextColor(),
            disableInSecondaryState = false
        ) {
            if (session?.loading == true) {
                onItemTapped.invoke(ToolbarMenu.Item.Stop)
            } else {
                onItemTapped.invoke(ToolbarMenu.Item.Reload)
            }
        }

        BrowserMenuItemToolbar(listOf(back, forward, refresh))
    }

    private fun shouldShowOpenInApp(): Boolean = session?.let { session ->
        val appLink = context.components.useCases.appLinksUseCases.appLinkRedirect
        appLink(session.url).hasExternalApp()
    } ?: false

    private val menuItems by lazy {
        val menuItems = listOf(
            poweredBy,
            BrowserMenuDivider(),
            desktopMode,
            findInPage,
            openInApp.apply { visible = ::shouldShowOpenInApp },
            openInFenix,
            BrowserMenuDivider(),
            menuToolbar
        )
        if (shouldReverseItems) { menuItems.reversed() } else { menuItems }
    }

    private val desktopMode = BrowserMenuImageSwitch(
        imageResource = R.drawable.ic_desktop,
        label = context.getString(R.string.browser_menu_desktop_site),
        initialState = { session?.desktopMode ?: false }
    ) { checked ->
        onItemTapped.invoke(ToolbarMenu.Item.RequestDesktop(checked))
    }

    private val findInPage = BrowserMenuImageText(
        label = context.getString(R.string.browser_menu_find_in_page),
        imageResource = R.drawable.mozac_ic_search,
        iconTintColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.FindInPage)
    }

    private val openInApp = BrowserMenuHighlightableItem(
        label = context.getString(R.string.browser_menu_open_app_link),
        startImageResource = R.drawable.ic_app_links,
        iconTintColorResource = primaryTextColor(),
        highlight = BrowserMenuHighlight.LowPriority(
            label = context.getString(R.string.browser_menu_open_app_link),
            notificationTint = getColor(context, R.color.whats_new_notification_color)
        ),
        isHighlighted = { !context.settings().openInAppOpened }
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.OpenInApp)
    }

    private val openInFenix = SimpleBrowserMenuItem(
        label = context.getString(R.string.browser_menu_open_in_fenix, appName),
        textColorResource = primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.OpenInFenix)
    }

    private val poweredBy = BrowserMenuCategory(
        label = context.getStringWithArgSafe(R.string.browser_menu_powered_by, appName).toUpperCase(),
        textSize = CAPTION_TEXT_SIZE,
        textColorResource = primaryTextColor(),
        textStyle = Typeface.NORMAL
    )

    @ColorRes
    private fun primaryTextColor() = ThemeManager.resolveAttribute(R.attr.primaryText, context)

    companion object {
        private const val CAPTION_TEXT_SIZE = 12f
    }
}
