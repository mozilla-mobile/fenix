/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import mozilla.components.service.fxa.manager.SyncEnginesStorage
import mozilla.components.service.glean.Glean
import mozilla.components.service.glean.private.NoExtraKeys
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.GleanMetrics.AboutPage
import org.mozilla.fenix.GleanMetrics.Addons
import org.mozilla.fenix.GleanMetrics.AppTheme
import org.mozilla.fenix.GleanMetrics.BookmarksManagement
import org.mozilla.fenix.GleanMetrics.BrowserSearch
import org.mozilla.fenix.GleanMetrics.Collections
import org.mozilla.fenix.GleanMetrics.ContextMenu
import org.mozilla.fenix.GleanMetrics.CrashReporter
import org.mozilla.fenix.GleanMetrics.CustomTab
import org.mozilla.fenix.GleanMetrics.DownloadNotification
import org.mozilla.fenix.GleanMetrics.ErrorPage
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.FindInPage
import org.mozilla.fenix.GleanMetrics.History
import org.mozilla.fenix.GleanMetrics.Logins
import org.mozilla.fenix.GleanMetrics.MediaNotification
import org.mozilla.fenix.GleanMetrics.MediaState
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.Pocket
import org.mozilla.fenix.GleanMetrics.Preferences
import org.mozilla.fenix.GleanMetrics.PrivateBrowsingMode
import org.mozilla.fenix.GleanMetrics.PrivateBrowsingShortcut
import org.mozilla.fenix.GleanMetrics.QrScanner
import org.mozilla.fenix.GleanMetrics.ReaderMode
import org.mozilla.fenix.GleanMetrics.SearchDefaultEngine
import org.mozilla.fenix.GleanMetrics.SearchShortcuts
import org.mozilla.fenix.GleanMetrics.SearchSuggestions
import org.mozilla.fenix.GleanMetrics.SearchWidget
import org.mozilla.fenix.GleanMetrics.SearchWidgetCfr
import org.mozilla.fenix.GleanMetrics.SyncAccount
import org.mozilla.fenix.GleanMetrics.SyncAuth
import org.mozilla.fenix.GleanMetrics.Tab
import org.mozilla.fenix.GleanMetrics.Tip
import org.mozilla.fenix.GleanMetrics.ToolbarSettings
import org.mozilla.fenix.GleanMetrics.TopSites
import org.mozilla.fenix.GleanMetrics.TrackingProtection
import org.mozilla.fenix.GleanMetrics.UserSpecifiedSearchEngines
import org.mozilla.fenix.GleanMetrics.VoiceSearch
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.BrowsersCache

private class EventWrapper<T : Enum<T>>(
    private val recorder: ((Map<T, String>?) -> Unit),
    private val keyMapper: ((String) -> T)? = null
) {

    /**
     * Converts snake_case string to camelCase.
     */
    private fun String.asCamelCase(): String {
        val parts = split("_")
        val builder = StringBuilder()

        for ((index, part) in parts.withIndex()) {
            if (index == 0) {
                builder.append(part)
            } else {
                builder.append(part[0].toUpperCase())
                builder.append(part.substring(1))
            }
        }

        return builder.toString()
    }

    fun track(event: Event) {
        val extras = if (keyMapper != null) {
            event.extras?.mapKeys { (key) ->
                keyMapper.invoke(key.toString().asCamelCase())
            }
        } else {
            null
        }

        this.recorder(extras)
    }
}

private val Event.wrapper: EventWrapper<*>?
    get() = when (this) {
        is Event.OpenedApp -> EventWrapper(
            { Events.appOpened.record(it) },
            { Events.appOpenedKeys.valueOf(it) }
        )
        is Event.SearchBarTapped -> EventWrapper(
            { Events.searchBarTapped.record(it) },
            { Events.searchBarTappedKeys.valueOf(it) }
        )
        is Event.EnteredUrl -> EventWrapper(
            { Events.enteredUrl.record(it) },
            { Events.enteredUrlKeys.valueOf(it) }
        )
        is Event.PerformedSearch -> EventWrapper(
            {
                Metrics.searchCount[this.eventSource.countLabel].add(1)
                Events.performedSearch.record(it)
            },
            { Events.performedSearchKeys.valueOf(it) }
        )
        is Event.SearchWithAds -> EventWrapper<NoExtraKeys>(
            {
                BrowserSearch.withAds[label].add(1)
            }
        )
        is Event.SearchAdClicked -> EventWrapper<NoExtraKeys>(
            {
                BrowserSearch.adClicks[label].add(1)
            }
        )
        is Event.SearchInContent -> EventWrapper<NoExtraKeys>(
            {
                BrowserSearch.inContent[label].add(1)
            }
        )
        is Event.SearchShortcutSelected -> EventWrapper(
            { SearchShortcuts.selected.record(it) },
            { SearchShortcuts.selectedKeys.valueOf(it) }
        )
        is Event.FindInPageOpened -> EventWrapper<NoExtraKeys>(
            { FindInPage.opened.record(it) }
        )
        is Event.FindInPageClosed -> EventWrapper<NoExtraKeys>(
            { FindInPage.closed.record(it) }
        )
        is Event.FindInPageSearchCommitted -> EventWrapper<NoExtraKeys>(
            { FindInPage.searchedPage.record(it) }
        )
        is Event.ContextMenuItemTapped -> EventWrapper(
            { ContextMenu.itemTapped.record(it) },
            { ContextMenu.itemTappedKeys.valueOf(it) }
        )
        is Event.CrashReporterOpened -> EventWrapper<NoExtraKeys>(
            { CrashReporter.opened.record(it) }
        )
        is Event.CrashReporterClosed -> EventWrapper(
            { CrashReporter.closed.record(it) },
            { CrashReporter.closedKeys.valueOf(it) }
        )
        is Event.BrowserMenuItemTapped -> EventWrapper(
            { Events.browserMenuAction.record(it) },
            { Events.browserMenuActionKeys.valueOf(it) }
        )
        is Event.OpenedBookmarkInNewTab -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.openInNewTab.record(it) }
        )
        is Event.OpenedBookmarksInNewTabs -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.openInNewTabs.record(it) }
        )
        is Event.OpenedBookmarkInPrivateTab -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.openInPrivateTab.record(it) }
        )
        is Event.OpenedBookmarksInPrivateTabs -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.openInPrivateTabs.record(it) }
        )
        is Event.EditedBookmark -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.edited.record(it) }
        )
        is Event.MovedBookmark -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.moved.record(it) }
        )
        is Event.RemoveBookmark -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.removed.record(it) }
        )
        is Event.RemoveBookmarks -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.multiRemoved.record(it) }
        )
        is Event.ShareBookmark -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.shared.record(it) }
        )
        is Event.CopyBookmark -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.copied.record(it) }
        )
        is Event.AddBookmarkFolder -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.folderAdd.record(it) }
        )
        is Event.RemoveBookmarkFolder -> EventWrapper<NoExtraKeys>(
            { BookmarksManagement.folderRemove.record(it) }
        )
        is Event.CustomTabsMenuOpened -> EventWrapper<NoExtraKeys>(
            { CustomTab.menu.record(it) }
        )
        is Event.CustomTabsActionTapped -> EventWrapper<NoExtraKeys>(
            { CustomTab.actionButton.record(it) }
        )
        is Event.CustomTabsClosed -> EventWrapper<NoExtraKeys>(
            { CustomTab.closed.record(it) }
        )
        is Event.UriOpened -> EventWrapper<NoExtraKeys>(
            { Events.totalUriCount.add(1) }
        )
        is Event.QRScannerOpened -> EventWrapper<NoExtraKeys>(
            { QrScanner.opened.record(it) }
        )
        is Event.QRScannerPromptDisplayed -> EventWrapper<NoExtraKeys>(
            { QrScanner.promptDisplayed.record(it) }
        )
        is Event.QRScannerNavigationAllowed -> EventWrapper<NoExtraKeys>(
            { QrScanner.navigationAllowed.record(it) }
        )
        is Event.QRScannerNavigationDenied -> EventWrapper<NoExtraKeys>(
            { QrScanner.navigationDenied.record(it) }
        )
        is Event.ErrorPageVisited -> EventWrapper(
            { ErrorPage.visitedError.record(it) },
            { ErrorPage.visitedErrorKeys.valueOf(it) }
        )
        is Event.SyncAuthOpened -> EventWrapper<NoExtraKeys>(
            { SyncAuth.opened.record(it) }
        )
        is Event.SyncAuthClosed -> EventWrapper<NoExtraKeys>(
            { SyncAuth.closed.record(it) }
        )
        is Event.SyncAuthUseEmail -> EventWrapper<NoExtraKeys>(
            { SyncAuth.useEmail.record(it) }
        )
        is Event.SyncAuthUseEmailProblem -> EventWrapper<NoExtraKeys>(
            { SyncAuth.useEmailProblem.record(it) }
        )
        is Event.SyncAuthSignIn -> EventWrapper<NoExtraKeys>(
            { SyncAuth.signIn.record(it) }
        )
        is Event.SyncAuthSignUp -> EventWrapper<NoExtraKeys>(
            { SyncAuth.signUp.record(it) }
        )
        is Event.SyncAuthPaired -> EventWrapper<NoExtraKeys>(
            { SyncAuth.paired.record(it) }
        )
        is Event.SyncAuthOtherExternal -> EventWrapper<NoExtraKeys>(
            { SyncAuth.otherExternal.record(it) }
        )
        is Event.SyncAuthFromShared -> EventWrapper<NoExtraKeys>(
            { SyncAuth.autoLogin.record(it) }
        )
        is Event.SyncAuthRecovered -> EventWrapper<NoExtraKeys>(
            { SyncAuth.recovered.record(it) }
        )
        is Event.SyncAuthSignOut -> EventWrapper<NoExtraKeys>(
            { SyncAuth.signOut.record(it) }
        )
        is Event.SyncAuthScanPairing -> EventWrapper<NoExtraKeys>(
            { SyncAuth.scanPairing.record(it) }
        )
        is Event.SyncAccountOpened -> EventWrapper<NoExtraKeys>(
            { SyncAccount.opened.record(it) }
        )
        is Event.SyncAccountClosed -> EventWrapper<NoExtraKeys>(
            { SyncAccount.closed.record(it) }
        )
        is Event.SyncAccountSyncNow -> EventWrapper<NoExtraKeys>(
            { SyncAccount.syncNow.record(it) }
        )
        is Event.SignInToSendTab -> EventWrapper<NoExtraKeys>(
            { SyncAccount.signInToSendTab.record(it) }
        )
        is Event.SendTab -> EventWrapper<NoExtraKeys>(
            { SyncAccount.sendTab.record(it) }
        )
        is Event.PreferenceToggled -> EventWrapper(
            { Events.preferenceToggled.record(it) },
            { Events.preferenceToggledKeys.valueOf(it) }
        )
        is Event.HistoryOpened -> EventWrapper<NoExtraKeys>(
            { History.opened.record(it) }
        )
        is Event.HistoryItemShared -> EventWrapper<NoExtraKeys>(
            { History.shared.record(it) }
        )
        is Event.HistoryItemOpened -> EventWrapper<NoExtraKeys>(
            { History.openedItem.record(it) }
        )
        is Event.HistoryItemRemoved -> EventWrapper<NoExtraKeys>(
            { History.removed.record(it) }
        )
        is Event.HistoryAllItemsRemoved -> EventWrapper<NoExtraKeys>(
            { History.removedAll.record(it) }
        )
        is Event.CollectionRenamed -> EventWrapper<NoExtraKeys>(
            { Collections.renamed.record(it) }
        )
        is Event.CollectionTabRestored -> EventWrapper<NoExtraKeys>(
            { Collections.tabRestored.record(it) }
        )
        is Event.CollectionAllTabsRestored -> EventWrapper<NoExtraKeys>(
            { Collections.allTabsRestored.record(it) }
        )
        is Event.CollectionTabRemoved -> EventWrapper<NoExtraKeys>(
            { Collections.tabRemoved.record(it) }
        )
        is Event.CollectionShared -> EventWrapper<NoExtraKeys>(
            { Collections.shared.record(it) }
        )
        is Event.CollectionRemoved -> EventWrapper<NoExtraKeys>(
            { Collections.removed.record(it) }
        )
        is Event.CollectionTabSelectOpened -> EventWrapper<NoExtraKeys>(
            { Collections.tabSelectOpened.record(it) }
        )
        is Event.ReaderModeAvailable -> EventWrapper<NoExtraKeys>(
            { ReaderMode.available.record(it) }
        )
        is Event.ReaderModeOpened -> EventWrapper<NoExtraKeys>(
            { ReaderMode.opened.record(it) }
        )
        is Event.ReaderModeClosed -> EventWrapper<NoExtraKeys>(
            { ReaderMode.closed.record(it) }
        )
        is Event.ReaderModeAppearanceOpened -> EventWrapper<NoExtraKeys>(
            { ReaderMode.appearance.record(it) }
        )
        is Event.CollectionTabLongPressed -> EventWrapper<NoExtraKeys>(
            { Collections.longPress.record(it) }
        )
        is Event.CollectionSaveButtonPressed -> EventWrapper(
            { Collections.saveButton.record(it) },
            { Collections.saveButtonKeys.valueOf(it) }
        )
        is Event.CollectionAddTabPressed -> EventWrapper<NoExtraKeys>(
            { Collections.addTabButton.record(it) }
        )
        is Event.CollectionRenamePressed -> EventWrapper<NoExtraKeys>(
            { Collections.renameButton.record(it) }
        )
        is Event.CollectionSaved -> EventWrapper(
            { Collections.saved.record(it) },
            { Collections.savedKeys.valueOf(it) }
        )
        is Event.CollectionTabsAdded -> EventWrapper(
            { Collections.tabsAdded.record(it) },
            { Collections.tabsAddedKeys.valueOf(it) }
        )
        is Event.SearchWidgetNewTabPressed -> EventWrapper<NoExtraKeys>(
            { SearchWidget.newTabButton.record(it) }
        )
        is Event.SearchWidgetVoiceSearchPressed -> EventWrapper<NoExtraKeys>(
            { SearchWidget.voiceButton.record(it) }
        )
        is Event.PrivateBrowsingGarbageIconTapped -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingMode.garbageIcon.record(it) }
        )
        is Event.PrivateBrowsingSnackbarUndoTapped -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingMode.snackbarUndo.record(it) }
        )
        is Event.PrivateBrowsingNotificationTapped -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingMode.notificationTapped.record(it) }
        )
        is Event.PrivateBrowsingNotificationOpenTapped -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingMode.notificationOpen.record(it) }
        )
        is Event.PrivateBrowsingNotificationDeleteAndOpenTapped -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingMode.notificationDelete.record(it) }
        )
        is Event.PrivateBrowsingCreateShortcut -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingShortcut.createShortcut.record(it) }
        )
        is Event.PrivateBrowsingAddShortcutCFR -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingShortcut.cfrAddShortcut.record(it) }
        )
        is Event.PrivateBrowsingCancelCFR -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingShortcut.cfrCancel.record(it) }
        )
        is Event.PrivateBrowsingPinnedShortcutPrivateTab -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingShortcut.pinnedShortcutPriv.record(it) }
        )
        is Event.PrivateBrowsingStaticShortcutTab -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingShortcut.staticShortcutTab.record(it) }
        )
        is Event.PrivateBrowsingStaticShortcutPrivateTab -> EventWrapper<NoExtraKeys>(
            { PrivateBrowsingShortcut.staticShortcutPriv.record(it) }
        )
        is Event.WhatsNewTapped -> EventWrapper<NoExtraKeys>(
            { Events.whatsNewTapped.record(it) }
        )
        is Event.TabMediaPlay -> EventWrapper<NoExtraKeys>(
            { Tab.mediaPlay.record(it) }
        )
        is Event.TabMediaPause -> EventWrapper<NoExtraKeys>(
            { Tab.mediaPause.record(it) }
        )
        is Event.MediaPlayState -> EventWrapper<NoExtraKeys>(
            { MediaState.play.record(it) }
        )
        is Event.MediaPauseState -> EventWrapper<NoExtraKeys>(
            { MediaState.pause.record(it) }
        )
        is Event.MediaStopState -> EventWrapper<NoExtraKeys>(
            { MediaState.stop.record(it) }
        )
        is Event.InAppNotificationDownloadOpen -> EventWrapper<NoExtraKeys>(
            { DownloadNotification.inAppOpen.record(it) }
        )
        is Event.InAppNotificationDownloadTryAgain -> EventWrapper<NoExtraKeys>(
            { DownloadNotification.inAppTryAgain.record(it) }
        )
        is Event.NotificationDownloadCancel -> EventWrapper<NoExtraKeys>(
            { DownloadNotification.cancel.record(it) }
        )
        is Event.NotificationDownloadOpen -> EventWrapper<NoExtraKeys>(
            { DownloadNotification.open.record(it) }
        )
        is Event.NotificationDownloadPause -> EventWrapper<NoExtraKeys>(
            { DownloadNotification.pause.record(it) }
        )
        is Event.NotificationDownloadResume -> EventWrapper<NoExtraKeys>(
            { DownloadNotification.resume.record(it) }
        )
        is Event.NotificationDownloadTryAgain -> EventWrapper<NoExtraKeys>(
            { DownloadNotification.tryAgain.record(it) }
        )
        is Event.NotificationMediaPlay -> EventWrapper<NoExtraKeys>(
            { MediaNotification.play.record(it) }
        )
        is Event.NotificationMediaPause -> EventWrapper<NoExtraKeys>(
            { MediaNotification.pause.record(it) }
        )
        is Event.TrackingProtectionTrackerList -> EventWrapper<NoExtraKeys>(
            { TrackingProtection.etpTrackerList.record(it) }
        )
        is Event.TrackingProtectionIconPressed -> EventWrapper<NoExtraKeys>(
            { TrackingProtection.etpShield.record(it) }
        )
        is Event.TrackingProtectionSettingsPanel -> EventWrapper<NoExtraKeys>(
            { TrackingProtection.panelSettings.record(it) }
        )
        is Event.TrackingProtectionSettings -> EventWrapper<NoExtraKeys>(
            { TrackingProtection.etpSettings.record(it) }
        )
        is Event.TrackingProtectionException -> EventWrapper<NoExtraKeys>(
            { TrackingProtection.exceptionAdded.record(it) }
        )
        is Event.TrackingProtectionSettingChanged -> EventWrapper(
            { TrackingProtection.etpSettingChanged.record(it) },
            { TrackingProtection.etpSettingChangedKeys.valueOf(it) }
        )
        is Event.OpenedLink -> EventWrapper(
            { Events.openedLink.record(it) },
            { Events.openedLinkKeys.valueOf(it) }
        )
        is Event.OpenLogins -> EventWrapper<NoExtraKeys>(
            { Logins.openLogins.record(it) }
        )
        is Event.OpenOneLogin -> EventWrapper<NoExtraKeys>(
            { Logins.openIndividualLogin.record(it) }
        )
        is Event.CopyLogin -> EventWrapper<NoExtraKeys>(
            { Logins.copyLogin.record(it) }
        )
        is Event.ViewLoginPassword -> EventWrapper<NoExtraKeys>(
            { Logins.viewPasswordLogin.record(it) }
        )
        is Event.PrivateBrowsingShowSearchSuggestions -> EventWrapper<NoExtraKeys>(
            { SearchSuggestions.enableInPrivate.record(it) }
        )
        is Event.ToolbarPositionChanged -> EventWrapper(
            { ToolbarSettings.changedPosition.record(it) },
            { ToolbarSettings.changedPositionKeys.valueOf(it) }
        )
        is Event.CustomEngineAdded -> EventWrapper<NoExtraKeys>(
            { UserSpecifiedSearchEngines.customEngineAdded.record(it) }
        )
        is Event.CustomEngineDeleted -> EventWrapper<NoExtraKeys>(
            { UserSpecifiedSearchEngines.customEngineDeleted.record(it) }
        )
        is Event.SaveLoginsSettingChanged -> EventWrapper(
            { Logins.saveLoginsSettingChanged.record(it) },
            { Logins.saveLoginsSettingChangedKeys.valueOf(it) }
        )
        is Event.TopSiteOpenDefault -> EventWrapper<NoExtraKeys>(
            { TopSites.openDefault.record(it) }
        )
        is Event.TopSiteOpenInNewTab -> EventWrapper<NoExtraKeys>(
            { TopSites.openInNewTab.record(it) }
        )
        is Event.TopSiteOpenInPrivateTab -> EventWrapper<NoExtraKeys>(
            { TopSites.openInPrivateTab.record(it) }
        )
        is Event.TopSiteRemoved -> EventWrapper<NoExtraKeys>(
            { TopSites.remove.record(it) }
        )
        is Event.SupportTapped -> EventWrapper<NoExtraKeys>(
            { AboutPage.supportTapped.record(it) }
        )
        is Event.PrivacyNoticeTapped -> EventWrapper<NoExtraKeys>(
            { AboutPage.privacyNoticeTapped.record(it) }
        )
        is Event.RightsTapped -> EventWrapper<NoExtraKeys>(
            { AboutPage.rightsTapped.record(it) }
        )
        is Event.LicensingTapped -> EventWrapper<NoExtraKeys>(
            { AboutPage.licensingTapped.record(it) }
        )
        is Event.LibrariesThatWeUseTapped -> EventWrapper<NoExtraKeys>(
            { AboutPage.librariesTapped.record(it) }
        )
        is Event.PocketTopSiteClicked -> EventWrapper<NoExtraKeys>(
            { Pocket.pocketTopSiteClicked.record(it) }
        )
        is Event.PocketTopSiteRemoved -> EventWrapper<NoExtraKeys>(
            { Pocket.pocketTopSiteRemoved.record(it) }
        )
        is Event.DarkThemeSelected -> EventWrapper(
            { AppTheme.darkThemeSelected.record(it) },
            { AppTheme.darkThemeSelectedKeys.valueOf(it) }
        )
        is Event.AddonsOpenInSettings -> EventWrapper<NoExtraKeys>(
            { Addons.openAddonsInSettings.record(it) }
        )
        is Event.AddonsOpenInToolbarMenu -> EventWrapper(
            { Addons.openAddonInToolbarMenu.record(it) },
            { Addons.openAddonInToolbarMenuKeys.valueOf(it) }
        )
        is Event.TipDisplayed -> EventWrapper(
            { Tip.displayed.record(it) },
            { Tip.displayedKeys.valueOf(it) }
        )
        is Event.TipPressed -> EventWrapper(
            { Tip.pressed.record(it) },
            { Tip.pressedKeys.valueOf(it) }
        )
        is Event.TipClosed -> EventWrapper(
            { Tip.closed.record(it) },
            { Tip.closedKeys.valueOf(it) }
        )
        is Event.VoiceSearchTapped -> EventWrapper<NoExtraKeys>(
            { VoiceSearch.tapped.record(it) }
        )
        is Event.SearchWidgetCFRDisplayed -> EventWrapper<NoExtraKeys>(
            { SearchWidgetCfr.displayed.record(it) }
        )
        is Event.SearchWidgetCFRCanceled -> EventWrapper<NoExtraKeys>(
            { SearchWidgetCfr.canceled.record(it) }
        )
        is Event.SearchWidgetCFRNotNowPressed -> EventWrapper<NoExtraKeys>(
            { SearchWidgetCfr.notNowPressed.record(it) }
        )
        is Event.SearchWidgetCFRAddWidgetPressed -> EventWrapper<NoExtraKeys>(
            { SearchWidgetCfr.addWidgetPressed.record(it) }
        )
        is Event.TabCounterMenuItemTapped -> EventWrapper(
            { Events.tabCounterMenuAction.record(it) },
            { Events.tabCounterMenuActionKeys.valueOf(it) }
        )

        // Don't record other events in Glean:
        is Event.AddBookmark -> null
        is Event.OpenedBookmark -> null
        is Event.OpenedAppFirstRun -> null
        is Event.InteractWithSearchURLArea -> null
        is Event.ClearedPrivateData -> null
        is Event.DismissedOnboarding -> null
        is Event.FennecToFenixMigrated -> null
    }

class GleanMetricsService(private val context: Context) : MetricsService {
    override val type = MetricServiceType.Data

    private val logger = Logger("GleanMetricsService")
    private var initialized = false

    private val activationPing = ActivationPing(context)
    private val installationPing = FirstSessionPing(context)

    override fun start() {
        logger.debug("Enabling Glean.")
        // Initialization of Glean already happened in FenixApplication.
        Glean.setUploadEnabled(true)

        if (initialized) return
        initialized = true

        // The code below doesn't need to execute immediately, so we'll add them to the visual
        // completeness task queue to be run later.
        context.components.performance.visualCompletenessQueue.runIfReadyOrQueue {
            // We have to initialize Glean *on* the main thread, because it registers lifecycle
            // observers. However, the activation ping must be sent *off* of the main thread,
            // because it calls Google ad APIs that must be called *off* of the main thread.
            // These two things actually happen in parallel, but that should be ok because Glean
            // can handle events being recorded before it's initialized.
            Glean.registerPings(Pings)

            // setStartupMetrics is not a fast function. It does not need to be done before we can consider
            // ourselves initialized. So, let's do it, well, later.
            setStartupMetrics()
        }
    }

    internal fun setStartupMetrics() {
        setPreferenceMetrics()
        Metrics.apply {
            defaultBrowser.set(BrowsersCache.all(context).isDefaultBrowser)
            MozillaProductDetector.getMozillaBrowserDefault(context)?.also {
                defaultMozBrowser.set(it)
            }
            mozillaProducts.set(MozillaProductDetector.getInstalledMozillaProducts(context))

            adjustCampaign.set(context.settings().adjustCampaignId)
            adjustAdGroup.set(context.settings().adjustAdGroup)
            adjustCreative.set(context.settings().adjustCreative)
            adjustNetwork.set(context.settings().adjustNetwork)

            searchWidgetInstalled.set(context.settings().searchWidgetInstalled)

            val topSitesSize = context.settings().topSitesSize
            hasTopSites.set(topSitesSize > 0)
            if (topSitesSize > 0) {
                topSitesCount.add(topSitesSize)
            }

            toolbarPosition.set(
                if (context.settings().shouldUseBottomToolbar) {
                    Event.ToolbarPositionChanged.Position.BOTTOM.name
                } else {
                    Event.ToolbarPositionChanged.Position.TOP.name
                }
            )
        }

        SearchDefaultEngine.apply {
            val defaultEngine = context
                .components
                .search
                .searchEngineManager
                .defaultSearchEngine ?: return@apply

            code.set(defaultEngine.identifier)
            name.set(defaultEngine.name)
            submissionUrl.set(defaultEngine.buildSearchUrl(""))
        }

        activationPing.checkAndSend()
        installationPing.checkAndSend()
    }

    private fun setPreferenceMetrics() {
        // We purposefully make all of our preferences the string_list format to make data analysis
        // simpler. While it makes things like booleans a bit more complicated, it means all our
        // preferences can be analyzed with the same dashboard and compared.
        Preferences.apply {
            showSearchSuggestions.set(context.settings().shouldShowSearchSuggestions.toStringList())
            remoteDebugging.set(context.settings().isRemoteDebuggingEnabled.toStringList())
            telemetry.set(context.settings().isTelemetryEnabled.toStringList())
            searchBrowsingHistory.set(context.settings().shouldShowHistorySuggestions.toStringList())
            searchBookmarks.set(context.settings().shouldShowBookmarkSuggestions.toStringList())
            showClipboardSuggestions.set(context.settings().shouldShowClipboardSuggestions.toStringList())
            showSearchShortcuts.set(context.settings().shouldShowSearchShortcuts.toStringList())
            openLinksInAPrivateTab.set(context.settings().openLinksInAPrivateTab.toStringList())
            searchSuggestionsPrivate.set(context.settings().shouldShowSearchSuggestionsInPrivate.toStringList())
            showVoiceSearch.set(context.settings().shouldShowVoiceSearch.toStringList())
            openLinksInApp.set(context.settings().openLinksInExternalApp.toStringList())

            val isLoggedIn =
                context.components.backgroundServices.accountManager.accountProfile() != null
            sync.set(isLoggedIn.toStringList())

            val syncedItems = SyncEnginesStorage(context).getStatus().entries.filter {
                it.value
            }.map { it.key.nativeName }

            syncItems.set(syncedItems)

            val toolbarPositionSelection =
                if (context.settings().shouldUseFixedTopToolbar) {
                    "fixed_top"
                } else if (context.settings().shouldUseBottomToolbar) {
                    "bottom"
                } else {
                    "top"
                }

            toolbarPosition.set(listOf(toolbarPositionSelection))

            val etpSelection =
                if (!context.settings().shouldUseTrackingProtection) {
                    ""
                } else if (context.settings().useStandardTrackingProtection) {
                    "standard"
                } else if (context.settings().useStrictTrackingProtection) {
                    "strict"
                } else if (context.settings().useCustomTrackingProtection) {
                    "custom"
                } else {
                    ""
                }

            trackingProtection.set(listOf(etpSelection))

            val accessibilitySelection = mutableListOf<String>()

            if (context.settings().switchServiceIsEnabled) { accessibilitySelection.add("switch") }

            if (context.settings().touchExplorationIsEnabled) {
                accessibilitySelection.add("touch exploration")
            }

            accessibilityServices.set(accessibilitySelection.toList())

            val themeSelection =
                if (context.settings().shouldUseLightTheme) {
                    "light"
                } else if (context.settings().shouldUseDarkTheme) {
                    "dark"
                } else if (context.settings().shouldFollowDeviceTheme) {
                    "system"
                } else if (context.settings().shouldUseAutoBatteryTheme) {
                    "battery"
                } else {
                    ""
                }

            theme.set(listOf(themeSelection))
        }
    }

    override fun stop() {
        Glean.setUploadEnabled(false)
    }

    override fun track(event: Event) {
        event.wrapper?.track(event)
    }

    override fun shouldTrack(event: Event): Boolean {
        return event.wrapper != null
    }
}

// Helper function for making our booleans fit into the string list formatting
fun Boolean.toStringList(): List<String> {
    return listOf(this.toString())
}
