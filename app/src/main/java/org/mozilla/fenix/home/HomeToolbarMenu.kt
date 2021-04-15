/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.content.Context
import androidx.annotation.ColorRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.BrowserMenuItem
import mozilla.components.browser.menu.ext.getHighlight
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuHighlightableItem
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.components.accounts.FenixAccountManager
import org.mozilla.fenix.experiments.ExperimentBranch
import org.mozilla.fenix.experiments.Experiments
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.withExperiment
import org.mozilla.fenix.theme.ThemeManager

@Suppress("LongMethod")
class HomeToolbarMenu(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {},
    private val onMenuBuilderChanged: (BrowserMenuBuilder) -> Unit = {},
    private val onHighlightPresent: (BrowserMenuHighlight) -> Unit = {}
) {

    @ColorRes
    private val primaryTextColor =
        ThemeManager.resolveAttribute(R.attr.primaryText, context)

    @ColorRes
    private val syncDisconnectedColor =
        ThemeManager.resolveAttribute(R.attr.syncDisconnected, context)

    private val syncDisconnectedBackgroundColor =
        context.getColorFromAttr(R.attr.syncDisconnectedBackground)

    private val shouldUseBottomToolbar = context.settings().shouldUseBottomToolbar
    private val shouldDeleteOnQuit = context.settings().shouldDeleteBrowsingDataOnQuit
    private val experiments = context.components.analytics.experiments

    private val toolbarMenuItems = HomeToolbarMenuItems(
        context,
        onItemTapped,
        primaryTextColor
    )

    // 'Reconnect' and 'Quit' items aren't needed most of the time, so we'll only create the if necessary.
    private val reconnectToSyncItem by lazy {
        BrowserMenuHighlightableItem(
            context.getString(R.string.sync_reconnect),
            R.drawable.ic_sync_disconnected,
            iconTintColorResource = syncDisconnectedColor,
            textColorResource = primaryTextColor,
            highlight = BrowserMenuHighlight.HighPriority(
                backgroundTint = syncDisconnectedBackgroundColor,
                canPropagate = false
            ),
            isHighlighted = { true }
        ) {
            onItemTapped.invoke(Item.ReconnectSync)
        }
    }

    private val quitItem by lazy {
        BrowserMenuImageText(
            context.getString(R.string.delete_browsing_data_on_quit_action),
            R.drawable.ic_exit,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Quit)
        }
    }

    // We want to validate that the Nimbus experiments library is working, from the android UI
    // all the way back to the data science backend. We're not testing the user's preference
    // or response, we're end-to-end testing the experiments platform.
    // So here, we're running multiple identical branches with the same treatment, and if the
    // user isn't targeted, then we get still get the same treatment.
    // The `let` block is degenerate here, but left here so as to document the form of how experiments
    // are implemented here.
    val historyIcon = experiments.withExperiment(Experiments.A_A_NIMBUS_VALIDATION) {
        when (it) {
            ExperimentBranch.A1 -> R.drawable.ic_history
            ExperimentBranch.A2 -> R.drawable.ic_history
            else -> R.drawable.ic_history
        }
    }
    val historyItem = BrowserMenuImageText(
        context.getString(R.string.library_history),
        historyIcon,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.History)
    }

    val addons = BrowserMenuImageText(
        context.getString(R.string.browser_menu_add_ons),
        R.drawable.ic_addons_extensions,
        primaryTextColor
    ) {
        onItemTapped.invoke(Item.Extensions)
    }

    val settingsItem = toolbarMenuItems.settingsItem
    val syncedTabsItem = toolbarMenuItems.syncedTabsItem
    val helpItem = toolbarMenuItems.helpItem
    val downloadsItem = toolbarMenuItems.downloadsItem
    val requestDesktopModeItem = toolbarMenuItems.requestDesktopSiteItem
    val extensionsItem = toolbarMenuItems.extensionsItem
    val whatsNewItem = toolbarMenuItems.oldWhatsNewItem

    // Only query account manager if it has been initialized.
    // We don't want to cause its initialization just for this check.
    val accountAuthItem =
        if (context.components.backgroundServices.accountManagerAvailableQueue.isReady()) {
            if (context.components.backgroundServices.accountManager.accountNeedsReauth()) {
                reconnectToSyncItem
            } else {
                null
            }
        } else {
            null
        }

    private val oldCoreMenuItems by lazy {
        val bookmarksIcon = experiments.withExperiment(Experiments.BOOKMARK_ICON) {
            when (it) {
                ExperimentBranch.TREATMENT -> R.drawable.ic_bookmark_list
                else -> R.drawable.ic_bookmark_filled
            }
        }
        val bookmarksItem = BrowserMenuImageText(
            context.getString(R.string.library_bookmarks),
            bookmarksIcon,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Bookmarks)
        }


        val menuItems = listOfNotNull(
            if (shouldDeleteOnQuit) quitItem else null,
            settingsItem,
            BrowserMenuDivider(),
            syncedTabsItem,
            bookmarksItem,
            historyItem,
            downloadsItem,
            BrowserMenuDivider(),
            addons,
            BrowserMenuDivider(),
            whatsNewItem,
            helpItem,
            accountAuthItem
        ).also { items ->
            items.getHighlight()?.let { onHighlightPresent(it) }
        }

        if (shouldUseBottomToolbar) {
            menuItems.reversed()
        } else {
            menuItems
        }
    }

    @Suppress("ComplexMethod")
    private fun newCoreMenuItems(): List<BrowserMenuItem> {
        val bookmarksIcon = experiments.withExperiment(Experiments.BOOKMARK_ICON) {
            when (it) {
                ExperimentBranch.TREATMENT -> R.drawable.ic_bookmark_list
                else -> R.drawable.ic_bookmark_filled
            }
        }
        val bookmarksItem = BrowserMenuImageText(
            context.getString(R.string.library_bookmarks),
            bookmarksIcon,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Bookmarks)
        }

        // We want to validate that the Nimbus experiments library is working, from the android UI
        // all the way back to the data science backend. We're not testing the user's preference
        // or response, we're end-to-end testing the experiments platform.
        // So here, we're running multiple identical branches with the same treatment, and if the
        // user isn't targeted, then we get still get the same treatment.
        // The `let` block is degenerate here, but left here so as to document the form of how experiments
        // are implemented here.
        val historyIcon = experiments.withExperiment(Experiments.A_A_NIMBUS_VALIDATION) {
            when (it) {
                ExperimentBranch.A1 -> R.drawable.ic_history
                ExperimentBranch.A2 -> R.drawable.ic_history
                else -> R.drawable.ic_history
            }
        }
        val historyItem = BrowserMenuImageText(
            context.getString(R.string.library_history),
            historyIcon,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.History)
        }

        val syncSignInItem = BrowserMenuImageText(
            context.getString(R.string.library_synced_tabs),
            R.drawable.ic_synced_tabs,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.SyncedTabs)
        }

        val settingsItem = BrowserMenuImageText(
            context.getString(R.string.browser_menu_settings),
            R.drawable.ic_settings,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Settings)
        }

        // Only query account manager if it has been initialized.
        // We don't want to cause its initialization just for this check.
        val accountAuthItem =
            if (context.components.backgroundServices.accountManagerAvailableQueue.isReady() &&
                context.components.backgroundServices.accountManager.accountNeedsReauth()) {
                reconnectToSyncItem
            } else {
                null
            }

        val menuItems = listOfNotNull(
            bookmarksItem,
            historyItem,
            downloadsItem,
            extensionsItem,
            syncSignInItem,
            accountAuthItem,
            BrowserMenuDivider(),
            requestDesktopModeItem,
            BrowserMenuDivider(),
            whatsNewItem,
            helpItem,
            settingsItem,
            if (shouldDeleteOnQuit) quitItem else null
        ).also { items ->
            items.getHighlight()?.let { onHighlightPresent(it) }
        }

        return menuItems
    }

    init {
        val accountManager = FenixAccountManager(context, lifecycleOwner)

        val menuItems = if (FeatureFlags.toolbarMenuFeature) {
            newCoreMenuItems()
        } else {
            oldCoreMenuItems
        }

        // Report initial state.
        onMenuBuilderChanged(BrowserMenuBuilder(menuItems))

        val menuItemsWithReconnectItem = if (FeatureFlags.toolbarMenuFeature) {
            menuItems
        } else {
            // reconnect item is manually added to the beginning of the list
            listOf(reconnectToSyncItem) + menuItems
        }

        // Observe account state changes, and update menu item builder with a new set of items.
        accountManager.observeAccountState(
            menuItemsWithReconnectItem,
            menuItems,
            onMenuBuilderChanged
        )
    }
}

sealed class Item {
    object Bookmarks : Item()
    object History : Item()
    object Downloads : Item()
    object Extensions : Item()
    object SyncedTabs : Item()
    object SyncAccount : Item()
    object WhatsNew : Item()
    object Help : Item()
    object Settings : Item()
    object Quit : Item()
    object ReconnectSync : Item()
    data class DesktopMode(val checked: Boolean) : Item()
}
