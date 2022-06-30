package org.mozilla.fenix.navigation

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.createGraph
import androidx.navigation.fragment.fragment
import androidx.navigation.navigation
import org.mozilla.fenix.StartupFragment
import org.mozilla.fenix.addons.*
import org.mozilla.fenix.browser.BrowserFragment
import org.mozilla.fenix.collections.CollectionCreationFragment
import org.mozilla.fenix.customtabs.ExternalAppBrowserFragment
import org.mozilla.fenix.exceptions.login.LoginExceptionsFragment
import org.mozilla.fenix.exceptions.trackingprotection.TrackingProtectionExceptionsFragment
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.library.bookmarks.BookmarkFragment
import org.mozilla.fenix.library.bookmarks.BookmarkSearchDialogFragment
import org.mozilla.fenix.library.bookmarks.addfolder.AddBookmarkFolderFragment
import org.mozilla.fenix.library.bookmarks.edit.EditBookmarkFragment
import org.mozilla.fenix.library.bookmarks.selectfolder.SelectBookmarkFolderFragment
import org.mozilla.fenix.library.downloads.DownloadFragment
import org.mozilla.fenix.library.history.HistoryFragment
import org.mozilla.fenix.library.history.HistorySearchDialogFragment
import org.mozilla.fenix.library.historymetadata.HistoryMetadataGroupFragment
import org.mozilla.fenix.library.recentlyclosed.RecentlyClosedFragment
import org.mozilla.fenix.library.syncedhistory.SyncedHistoryFragment
import org.mozilla.fenix.nimbus.NimbusBranchesFragment
import org.mozilla.fenix.nimbus.NimbusExperimentsFragment
import org.mozilla.fenix.onboarding.HomeOnboardingDialogFragment
import org.mozilla.fenix.perf.ProfilerStartDialogFragment
import org.mozilla.fenix.perf.ProfilerStopDialogFragment
import org.mozilla.fenix.search.SearchDialogFragment
import org.mozilla.fenix.settings.*
import org.mozilla.fenix.settings.about.AboutFragment
import org.mozilla.fenix.settings.about.AboutLibrariesFragment
import org.mozilla.fenix.settings.account.AccountProblemFragment
import org.mozilla.fenix.settings.account.AccountSettingsFragment
import org.mozilla.fenix.settings.account.SignOutFragment
import org.mozilla.fenix.settings.account.TurnOnSyncFragment
import org.mozilla.fenix.settings.address.AddressEditorFragment
import org.mozilla.fenix.settings.address.AddressManagementFragment
import org.mozilla.fenix.settings.advanced.LocaleSettingsFragment
import org.mozilla.fenix.settings.autofill.AutofillSettingFragment
import org.mozilla.fenix.settings.creditcards.CreditCardEditorFragment
import org.mozilla.fenix.settings.creditcards.CreditCardsManagementFragment
import org.mozilla.fenix.settings.deletebrowsingdata.DeleteBrowsingDataFragment
import org.mozilla.fenix.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitFragment
import org.mozilla.fenix.settings.logins.fragment.*
import org.mozilla.fenix.settings.quicksettings.ConnectionPanelDialogFragment
import org.mozilla.fenix.settings.quicksettings.QuickSettingsSheetDialogFragment
import org.mozilla.fenix.settings.search.AddSearchEngineFragment
import org.mozilla.fenix.settings.search.EditCustomSearchEngineFragment
import org.mozilla.fenix.settings.search.SearchEngineFragment
import org.mozilla.fenix.settings.sitepermissions.SitePermissionsDetailsExceptionsFragment
import org.mozilla.fenix.settings.sitepermissions.SitePermissionsExceptionsFragment
import org.mozilla.fenix.settings.sitepermissions.SitePermissionsManageExceptionsPhoneFeatureFragment
import org.mozilla.fenix.settings.sitepermissions.SitePermissionsManagePhoneFeatureFragment
import org.mozilla.fenix.settings.studies.StudiesFragment
import org.mozilla.fenix.settings.wallpaper.WallpaperSettingsFragment
import org.mozilla.fenix.share.AddNewDeviceFragment
import org.mozilla.fenix.share.ShareFragment
import org.mozilla.fenix.shortcut.CreateShortcutFragment
import org.mozilla.fenix.shortcut.PwaOnboardingDialogFragment
import org.mozilla.fenix.tabhistory.TabHistoryDialogFragment
import org.mozilla.fenix.tabstray.TabsTrayFragment
import org.mozilla.fenix.trackingprotection.TrackingProtectionBlockingFragment
import org.mozilla.fenix.trackingprotection.TrackingProtectionPanelDialogFragment

class NavigationManager {

    private inline fun <reified F: Fragment> NavGraphBuilder.buildFragmentDestination(context: Context, navRouteInfo: NavRouteInfo) {
        fragment<F>(navRouteInfo.navRoute) {
            with(navRouteInfo) {
                destinationLabelId?.let { label = context.getString(it) }
                screenArgs.forEach {
                    argument(name = it.argName) {
                        type = it.argType
                        defaultValue = it.defaultValue
                    }
                }
            }
        }
    }

    private fun NavGraphBuilder.buildSitePermissionExceptionsGraph(context: Context) {
        navigation(startDestination = SitePermissionsExceptionsFragment.NAV_ROUTE_INFO.navRoute, route = "site_permissions_exceptions_graph") {
            buildFragmentDestination<SitePermissionsExceptionsFragment>(context, SitePermissionsExceptionsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SitePermissionsManageExceptionsPhoneFeatureFragment>(context, SitePermissionsManageExceptionsPhoneFeatureFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SitePermissionsDetailsExceptionsFragment>(context, SitePermissionsDetailsExceptionsFragment.NAV_ROUTE_INFO)
        }
    }

    private fun NavGraphBuilder.buildAddOnsManagementGraph(context: Context) {
        navigation(startDestination = AddonsManagementFragment.NAV_ROUTE_INFO.navRoute, route = "add_ons_management_graph") {
            buildFragmentDestination<AddonsManagementFragment>(context, AddonsManagementFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<InstalledAddonDetailsFragment>(context, InstalledAddonDetailsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<NotYetSupportedAddonFragment>(context, NotYetSupportedAddonFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddonPermissionsDetailsFragment>(context, AddonPermissionsDetailsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddonInternalSettingsFragment>(context, AddonInternalSettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddonDetailsFragment>(context, AddonDetailsFragment.NAV_ROUTE_INFO)
        }
    }

    private fun NavGraphBuilder.buildSearchEngineGraph(context: Context) {
        navigation(startDestination = SearchEngineFragment.NAV_ROUTE_INFO.navRoute, route = "search_engine_graph") {
            buildFragmentDestination<SearchEngineFragment>(context, SearchEngineFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddSearchEngineFragment>(context, AddSearchEngineFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<EditCustomSearchEngineFragment>(context, EditCustomSearchEngineFragment.NAV_ROUTE_INFO)
        }
    }

    private fun NavGraphBuilder.buildAutoFillGraph(context: Context) {
        navigation(startDestination = AutofillSettingFragment.NAV_ROUTE_INFO.navRoute, route = "autofill_graph") {
            buildFragmentDestination<AutofillSettingFragment>(context, AutofillSettingFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<CreditCardEditorFragment>(context, CreditCardEditorFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<CreditCardsManagementFragment>(context, CreditCardsManagementFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddressEditorFragment>(context, AddressEditorFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddressManagementFragment>(context, AddressManagementFragment.NAV_ROUTE_INFO)
        }
    }

    private fun NavGraphBuilder.buildNimbusExperimentGraph(context: Context) {
        navigation(startDestination = NimbusExperimentsFragment.NAV_ROUTE_INFO.navRoute, route = "nimbus_experiment_graph") {
            buildFragmentDestination<NimbusExperimentsFragment>(context, NimbusExperimentsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<NimbusBranchesFragment>(context, NimbusBranchesFragment.NAV_ROUTE_INFO)
        }
    }

    fun createNavGraph(context: Context, navController: NavController) {
        navController.createGraph(startDestination = StartupFragment.NAV_ROUTE_INFO.navRoute) {
            buildFragmentDestination<StartupFragment>(context, StartupFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<HomeFragment>(context, HomeFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<TabsTrayFragment>(context, TabsTrayFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<HomeOnboardingDialogFragment>(context,HomeOnboardingDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SitePermissionsManagePhoneFeatureFragment>(context,SitePermissionsManagePhoneFeatureFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SearchDialogFragment>(context, SearchDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<RecentlyClosedFragment>(context, RecentlyClosedFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<BrowserFragment>(context, BrowserFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<ExternalAppBrowserFragment>(context, ExternalAppBrowserFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<HistoryFragment>(context, HistoryFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<HistorySearchDialogFragment>(context, HistorySearchDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<HistoryMetadataGroupFragment>(context, HistoryMetadataGroupFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SyncedHistoryFragment>(context, SyncedHistoryFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<DownloadFragment>(context, DownloadFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<BookmarkFragment>(context, BookmarkFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<BookmarkSearchDialogFragment>(context, BookmarkSearchDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<EditBookmarkFragment>(context, EditBookmarkFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SelectBookmarkFolderFragment>(context, SelectBookmarkFolderFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddBookmarkFolderFragment>(context, AddBookmarkFolderFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SavedLoginsAuthFragment>(context, SavedLoginsAuthFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SavedLoginsFragment>(context, SavedLoginsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<LoginExceptionsFragment>(context, LoginExceptionsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<LoginDetailFragment>(context, LoginDetailFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<EditLoginFragment>(context, EditLoginFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddLoginFragment>(context, AddLoginFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SettingsFragment>(context, SettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<ProfilerStartDialogFragment>(context, ProfilerStartDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<ProfilerStopDialogFragment>(context, ProfilerStopDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<TabsSettingsFragment>(context, TabsSettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<HomeSettingsFragment>(context, HomeSettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<WallpaperSettingsFragment>(context, WallpaperSettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<DataChoicesFragment>(context, DataChoicesFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<StudiesFragment>(context, StudiesFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AccessibilityFragment>(context, AccessibilityFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AccountSettingsFragment>(context, AccountSettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<TurnOnSyncFragment>(context, TurnOnSyncFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<PairFragment>(context, PairFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AboutFragment>(context, AboutFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SecretSettingsFragment>(context, SecretSettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SecretDebugSettingsFragment>(context, SecretDebugSettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AboutLibrariesFragment>(context, AboutLibrariesFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<CustomizationFragment>(context, CustomizationFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<PrivateBrowsingFragment>(context, PrivateBrowsingFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<HttpsOnlyFragment>(context, HttpsOnlyFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<TrackingProtectionFragment>(context, TrackingProtectionFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<DeleteBrowsingDataFragment>(context, DeleteBrowsingDataFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<TrackingProtectionExceptionsFragment>(context, TrackingProtectionExceptionsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<CollectionCreationFragment>(context, CollectionCreationFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<CreateShortcutFragment>(context, CreateShortcutFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<PwaOnboardingDialogFragment>(context, PwaOnboardingDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<ShareFragment>(context, ShareFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<QuickSettingsSheetDialogFragment>(context, QuickSettingsSheetDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AccountProblemFragment>(context, AccountProblemFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SignOutFragment>(context, SignOutFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<TrackingProtectionPanelDialogFragment>(context, TrackingProtectionPanelDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<ConnectionPanelDialogFragment>(context, ConnectionPanelDialogFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<TrackingProtectionBlockingFragment>(context, TrackingProtectionBlockingFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<DeleteBrowsingDataOnQuitFragment>(context, DeleteBrowsingDataOnQuitFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<AddNewDeviceFragment>(context, AddNewDeviceFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<LocaleSettingsFragment>(context, LocaleSettingsFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<SavedLoginsSettingFragment>(context, SavedLoginsSettingFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<WebExtensionActionPopupFragment>(context, WebExtensionActionPopupFragment.NAV_ROUTE_INFO)
            buildFragmentDestination<TabHistoryDialogFragment>(context, TabHistoryDialogFragment.NAV_ROUTE_INFO)

            buildSitePermissionExceptionsGraph(context)
            buildAddOnsManagementGraph(context)
            buildSearchEngineGraph(context)
            buildAutoFillGraph(context)
            buildNimbusExperimentGraph(context)
        }
    }
}