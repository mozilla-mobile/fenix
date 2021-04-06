/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.WebExtensionBrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuImageTextCheckboxButton
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.menu.item.WebExtensionPlaceholderMenuItem
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.storage.BookmarksStorage
import mozilla.components.feature.webcompat.reporter.WebCompatReporterFeature
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.FeatureFlags.tabsTrayRewrite
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.experiments.ExperimentBranch
import org.mozilla.fenix.experiments.Experiments
import org.mozilla.fenix.ext.asActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.withExperiment
import org.mozilla.fenix.theme.ThemeManager
import org.mozilla.fenix.utils.BrowsersCache

/**
 * Builds the toolbar object used with the 3-dot menu in the browser fragment.
 * @param context the [Context].
 * @param store reference to the application's [BrowserStore].
 * @param hasAccountProblem If true, there was a problem signing into the Firefox account.
 * @param shouldReverseItems If true, reverse the menu items.
 * @param onItemTapped Called when a menu item is tapped.
 * @param lifecycleOwner View lifecycle owner used to determine when to cancel UI jobs.
 * @param bookmarksStorage Used to check if a page is bookmarked.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@ExperimentalCoroutinesApi
open class DefaultToolbarMenu(
    private val context: Context,
    private val store: BrowserStore,
    hasAccountProblem: Boolean = false,
    private val onItemTapped: (ToolbarMenu.Item) -> Unit = {},
    private val lifecycleOwner: LifecycleOwner,
    private val bookmarksStorage: BookmarksStorage,
    val isPinningSupported: Boolean
) : ToolbarMenu {

    private var isCurrentUrlBookmarked = false
    private var isBookmarkedJob: Job? = null

    private val shouldDeleteDataOnQuit = context.settings().shouldDeleteBrowsingDataOnQuit
    private val shouldUseBottomToolbar = context.settings().shouldUseBottomToolbar

    private val selectedSession: TabSessionState?
        get() = store.state.selectedTab

    private val accountManager = context.components.backgroundServices.accountManager

    @ColorRes
    @VisibleForTesting
    private val primaryTextColor = ThemeManager.resolveAttribute(R.attr.primaryText, context)

    @ColorRes
    @VisibleForTesting
    private val accentBrightTextColor = ThemeManager.resolveAttribute(R.attr.accentBright, context)

    private val toolbarMenuItems = ToolbarMenuItems(
        context,
        store,
        hasAccountProblem,
        onItemTapped,
        primaryTextColor,
        accentBrightTextColor
    )

    override val menuBuilder by lazy {
        WebExtensionBrowserMenuBuilder(
            items =
                if (FeatureFlags.toolbarMenuFeature) {
                    newCoreMenuItems
                } else {
                    oldCoreMenuItems
                },
            endOfMenuAlwaysVisible = shouldUseBottomToolbar,
            store = store,
            webExtIconTintColorResource = primaryTextColor(),
            onAddonsManagerTapped = {
                onItemTapped.invoke(ToolbarMenu.Item.AddonsManager)
            },
            appendExtensionSubMenuAtStart = shouldUseBottomToolbar
        )
    }

    override val menuToolbarNavigation by lazy {
        val back = toolbarMenuItems.backNavButton
        val forward = toolbarMenuItems.forwardNavButton
        val refresh = toolbarMenuItems.refreshNavButton
        val share = toolbarMenuItems.shareItem

        registerForIsBookmarkedUpdates()

        if (FeatureFlags.toolbarMenuFeature) {
            BrowserMenuItemToolbar(listOf(back, forward, share, refresh))
        } else {
            val bookmark = BrowserMenuItemToolbar.TwoStateButton(
                primaryImageResource = R.drawable.ic_bookmark_filled,
                primaryContentDescription = context.getString(R.string.browser_menu_edit_bookmark),
                primaryImageTintResource = primaryTextColor(),
                // TwoStateButton.isInPrimaryState must be synchronous, and checking bookmark state is
                // relatively slow. The best we can do here is periodically compute and cache a new "is
                // bookmarked" state, and use that whenever the menu has been opened.
                isInPrimaryState = { isCurrentUrlBookmarked },
                secondaryImageResource = R.drawable.ic_bookmark_outline,
                secondaryContentDescription = context.getString(R.string.browser_menu_bookmark),
                secondaryImageTintResource = primaryTextColor(),
                disableInSecondaryState = false
            ) {
                if (!isCurrentUrlBookmarked) {
                    isCurrentUrlBookmarked = true
                }
                onItemTapped.invoke(ToolbarMenu.Item.Bookmark)
            }

            BrowserMenuItemToolbar(listOf(back, forward, bookmark, share, refresh))
        }
    }

    // Predicates that need to be repeatedly called as the session changes
    @VisibleForTesting(otherwise = PRIVATE)
    fun canAddToHomescreen(): Boolean =
        selectedSession != null && isPinningSupported &&
                !context.components.useCases.webAppUseCases.isInstallable()

    @VisibleForTesting(otherwise = PRIVATE)
    fun canInstall(): Boolean =
        selectedSession != null && isPinningSupported &&
                context.components.useCases.webAppUseCases.isInstallable()

    @VisibleForTesting(otherwise = PRIVATE)
    fun shouldShowOpenInApp(): Boolean = selectedSession?.let { session ->
        val appLink = context.components.useCases.appLinksUseCases.appLinkRedirect
        appLink(session.content.url).hasExternalApp()
    } ?: false

    @VisibleForTesting(otherwise = PRIVATE)
    fun shouldShowReaderViewCustomization(): Boolean = selectedSession?.let {
        store.state.findTab(it.id)?.readerState?.active
    } ?: false
    // End of predicates //

    private val installPwaToHomescreen = toolbarMenuItems.installPwaToHomescreen
    private val newTabItem = toolbarMenuItems.newTabItem
    private val historyItem = toolbarMenuItems.historyItem
    private val downloadsItem = toolbarMenuItems.downloadsItem
    private var findInPageItem = toolbarMenuItems.findInPageItem
    private var desktopSiteItem = toolbarMenuItems.requestDesktopSiteItem
    private var customizeReaderView = toolbarMenuItems.customizeReaderView
    private var openInApp = toolbarMenuItems.openInAppItem
    private var addToHomeScreenItem = toolbarMenuItems.addToHomeScreenItem
    private var addToTopSitesItem = toolbarMenuItems.addToTopSitesItem
    private var saveToCollectionItem = toolbarMenuItems.saveToCollectionItem
    private var settingsItem = toolbarMenuItems.settingsItem
    private var deleteDataOnQuit = toolbarMenuItems.deleteDataOnQuitItem

    private val extensionsItem = WebExtensionPlaceholderMenuItem(
        id = WebExtensionPlaceholderMenuItem.MAIN_EXTENSIONS_MENU_ID
    )

    private val reportSiteIssuePlaceholder = WebExtensionPlaceholderMenuItem(
        id = WebCompatReporterFeature.WEBCOMPAT_REPORTER_EXTENSION_ID
    )

    private var addEditBookmarksItem = BrowserMenuImageTextCheckboxButton(
        imageResource = R.drawable.ic_bookmarks_menu,
        iconTintColorResource = primaryTextColor,
        label = context.getString(R.string.library_bookmarks),
        labelListener = {
            onItemTapped.invoke(ToolbarMenu.Item.Bookmarks)
        },
        primaryStateIconResource = R.drawable.ic_bookmark_outline,
        secondaryStateIconResource = R.drawable.ic_bookmark_filled,
        tintColorResource = accentBrightTextColor,
        primaryLabel = context.getString(R.string.browser_menu_add),
        secondaryLabel = context.getString(R.string.browser_menu_edit),
        isInPrimaryState = { !isCurrentUrlBookmarked }
    ) {
        if (!isCurrentUrlBookmarked) {
            isCurrentUrlBookmarked = true
        }
        onItemTapped.invoke(ToolbarMenu.Item.Bookmark)
    }

    private val oldCoreMenuItems by lazy {
        val syncedTabs = toolbarMenuItems.oldSyncedTabsItem
        val addToHomescreen = toolbarMenuItems.oldAddToHomescreenItem
        val readerAppearance = toolbarMenuItems.oldReaderViewAppearanceItem
        val bookmarksItem = BrowserMenuImageText(
            context.getString(R.string.library_bookmarks),
            R.drawable.ic_bookmark_filled,
            primaryTextColor()
        ) {
            onItemTapped.invoke(ToolbarMenu.Item.Bookmarks)
        }

        // Predicates that are called once, during screen init
        val shouldShowSaveToCollection = (context.asActivity() as? HomeActivity)
            ?.browsingModeManager?.mode == BrowsingMode.Normal
        val shouldDeleteDataOnQuit = context.components.settings
            .shouldDeleteBrowsingDataOnQuit

        val menuItems = listOfNotNull(
            downloadsItem,
            historyItem,
            bookmarksItem,
            syncedTabs,
            settingsItem,
            if (shouldDeleteDataOnQuit) deleteDataOnQuit else null,
            BrowserMenuDivider(),
            reportSiteIssuePlaceholder,
            findInPageItem,
            getSetDefaultBrowserItem(),
            addToTopSitesItem,
            addToHomescreen.apply { visible = ::canAddToHomescreen },
            installPwaToHomescreen.apply { visible = ::canInstall },
            if (shouldShowSaveToCollection) saveToCollectionItem else null,
            desktopSiteItem,
            openInApp.apply { visible = ::shouldShowOpenInApp },
            readerAppearance.apply { visible = ::shouldShowReaderViewCustomization },
            BrowserMenuDivider(),
            menuToolbarNavigation
        )

        if (shouldUseBottomToolbar) {
            menuItems
        } else {
            menuItems.reversed()
        }
    }

    val syncedTabsItem = BrowserMenuImageText(
        context.getString(R.string.synced_tabs),
        R.drawable.ic_synced_tabs,
        primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.SyncedTabs)
    }

    private fun getSyncItemTitle(): String {
        val authenticatedAccount = accountManager.authenticatedAccount() != null
        val email = accountManager.accountProfile()?.email

        return if (authenticatedAccount && email != null) {
            email
        } else {
            context.getString(R.string.sync_menu_sign_in)
        }
    }

    val syncMenuItem = BrowserMenuImageText(
        getSyncItemTitle(),
        R.drawable.ic_synced_tabs,
        primaryTextColor()
    ) {
        onItemTapped.invoke(ToolbarMenu.Item.SyncAccount)
    }

    @VisibleForTesting(otherwise = PRIVATE)
    val newCoreMenuItems by lazy {
        // Predicates that are called once, during screen init
        val shouldShowSaveToCollection = (context.asActivity() as? HomeActivity)
            ?.browsingModeManager?.mode == BrowsingMode.Normal
        val shouldDeleteDataOnQuit = context.components.settings
            .shouldDeleteBrowsingDataOnQuit

        val menuItems =
            listOfNotNull(
                if (shouldUseBottomToolbar) null else menuToolbarNavigation,
                newTabItem,
                BrowserMenuDivider(),
                addEditBookmarksItem,
                historyItem,
                downloadsItem,
                extensionsItem,
                if (tabsTrayRewrite) syncMenuItem else syncedTabsItem,
                BrowserMenuDivider(),
                findInPageItem,
                desktopSiteItem,
                customizeReaderView.apply { visible = ::shouldShowReaderViewCustomization },
                openInApp.apply { visible = ::shouldShowOpenInApp },
                reportSiteIssuePlaceholder,
                BrowserMenuDivider(),
                addToHomeScreenItem.apply { visible = ::canAddToHomescreen },
                installPwaToHomescreen.apply { visible = ::canInstall },
                addToTopSitesItem,
                if (shouldShowSaveToCollection) saveToCollectionItem else null,
                BrowserMenuDivider(),
                settingsItem,
                if (shouldDeleteDataOnQuit) deleteDataOnQuit else null,
                if (shouldUseBottomToolbar) BrowserMenuDivider() else null,
                if (shouldUseBottomToolbar) menuToolbarNavigation else null
            )

        menuItems
    }

    @VisibleForTesting
    internal fun registerForIsBookmarkedUpdates() {
        store.flowScoped(lifecycleOwner) { flow ->
            flow.mapNotNull { state -> state.selectedTab }
                .ifAnyChanged { tab ->
                    arrayOf(
                        tab.id,
                        tab.content.url
                    )
                }
                .collect {
                    isCurrentUrlBookmarked = false
                    updateCurrentUrlIsBookmarked(it.content.url)
                }
        }
    }

    @VisibleForTesting
    internal fun updateCurrentUrlIsBookmarked(newUrl: String) {
        isBookmarkedJob?.cancel()
        isBookmarkedJob = lifecycleOwner.lifecycleScope.launch {
            isCurrentUrlBookmarked = bookmarksStorage
                .getBookmarksWithUrl(newUrl)
                .any { it.url == newUrl }
        }
    }

    private fun getSetDefaultBrowserItem(): BrowserMenuImageText? {
        val experiments = context.components.analytics.experiments
        val browsers = BrowsersCache.all(context)

        return experiments.withExperiment(Experiments.DEFAULT_BROWSER) { experimentBranch ->
            if (experimentBranch == ExperimentBranch.DEFAULT_BROWSER_TOOLBAR_MENU &&
                !browsers.isFirefoxDefaultBrowser
            ) {
                return@withExperiment BrowserMenuImageText(
                    label = context.getString(R.string.preferences_set_as_default_browser),
                    imageResource = R.mipmap.ic_launcher
                ) {
                    onItemTapped.invoke(ToolbarMenu.Item.SetDefaultBrowser)
                }
            } else {
                null
            }
        }
    }
}
