package org.mozilla.fenix.navigation

import mozilla.appservices.places.uniffi.HistoryMetadata
import org.mozilla.fenix.GleanMetrics.SitePermissions
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.settings.HttpsOnlyFragment
import org.mozilla.fenix.settings.SecretSettingsFragment
import org.mozilla.fenix.settings.sitepermissions.SitePermissionsDetailsExceptionsFragment
import org.mozilla.fenix.settings.sitepermissions.SitePermissionsManageExceptionsPhoneFeatureFragment

class NavigationManager {

    object NavRoutes {
        const val Startup = "startup"
        const val Home = "home"
        const val TabsTrayDialog = "tabs_tray_dialog"
        const val HomeOnBoarding = "home_on_boarding"
        const val SearchDialog = "search_dialog"
        const val RecentlyClosed = "recently_closed"
        const val SitePermissionsManager = "site_permissions_manager"
        const val Browser = "browser"
        const val ExternalAppBrowser = "external_app_browser"
        const val History = "history"
        const val HistorySearchDialog = "history_search_dialog"
        const val HistoryMetadataGroup = "history_metadata_group"
        const val SyncedHistory = "synced_history"
        const val Downloads = "downloads"
        const val Bookmark = "bookmark"
        const val BookmarkSearchDialog = "bookmark_search_dialog"
        const val BookmarkEdit = "bookmark_edit"
        const val BookmarkSelectFolder = "bookmark_select_folder"
        const val BookmarkAddFolder = "bookmark_add_folder"
        const val SavedLoginsAuth = "saved_logins_auth"
        const val SavedLogins = "saved_logins"
        const val LoginExceptions = "login_exceptions"
        const val LoginDetails = "login_details"
        const val EditLogin = "edit_login"
        const val AddLogin = "add_login"
        const val Settings = "settings"
        const val ProfilerStartDialog = "profiler_start_dialog"
        const val ProfilerStopDialog = "profiler_stop_dialog"
        const val TabSettings = "tab_settings"
        const val HomeSettings = "home_settings"
        const val WelcomeSettings = "welcome_settings"
        const val DataChoices = "data_choices"
        const val Studies = "studies"
        const val SitePermissions = "site_permissions"
        const val Accessibility = "accessibility"
        const val AccountSettings = "account_settings"
        const val TurnOnSync = "turn_on_sync"
        const val Pair = "pair"
        const val About = "about"
        const val SecretSettingsPref = "secret_settings_pref"
        const val SecretInfoSettings = "secret_info_settings"
        const val AboutLibraries = "about_libraries"
        const val Customization = "customization"
        const val PrivateBrowsing = "private_browsing"
        const val HttpsOnly = "https_only"
        const val TrackingProtection = "track_protection"
        const val DeleteBrowsingData = "delete_browsing_data"
        const val TrackingProtectionException = "tracking_protection_exception"
        const val CollectionCreationDialog = "collection_creation_dialog"
        const val CreateShortcutDialog = "create_shortcut_dialog"
        const val PwaOnBoardingDialog = "pwa_on_boarding_dialog"
        const val ShareDialog = "share_dialog"
        const val QuickSettingsSheetDialog = "quick_settings_sheet_dialog"
        const val AccountProblem = "account_problem"
        const val SignOutDialog = "sign_out_dialog"
        const val TrackingProtectionPanelDialog = "tracking_protection_panel"
        const val ConnectionPanelDialog = "connection_panel_dialog"
        const val TrackingProtectionBlocking = "tracking_protection_blocking"
        const val DeleteBrowsingDataOnQuit = "delete_browsing_data_on_quit"
        const val AddNewDeviceFragment = "add_new_device"
        const val LocaleSettings = "locale_settings"
        const val SaveLoginSetting = "save_login_setting"
        const val WebExtensionPopUp = "web_extension_pop_up"
        const val TabHistoryDialog = "tab_history_dialog"

        object SitePermissionsExceptions {
            const val SitePermissionsExceptions = "site_permissions_exceptions"
            const val SitePermissionsManageExceptionsPhoneFeature =
                "site_permissions_manage_exceptions_phone_feature"
            const val SitePermissionsDetailsExceptions = "site_permissions_details_exceptions"
        }

        object AddOnsManagement {
            const val AddOnsManagement = "add_ons_management"
            const val InstalledAddOnDetails = "installed_add_ons_details"
            const val NotYetSupportedAddOn = "not_yet_supported_add_on"
            const val AddOnPermissionDetail = "add_on_permissions_detail"
            const val AddOnInternalSettings = "add_on_internal_settings"
            const val AddOnDetails = "add_on_details"
        }

        object SearchEngine {
            const val SearchEngine = "search_engine"
            const val AddSearchEngine = "add_search_engine"
            const val EditCustomSearchEngine = "edit_custom_search_engine"
        }

        object NimbusExperiment {
            const val NimbusExperiments = "nimbus_experiments"
            const val NimbusBranches = "nimbus_branches"
        }

        object Autofill {
            const val AutoFillSetting = "autofill_setting"
            const val CreditCardEditor = "credit_card_editor"
            const val CreditCardsManagement = "credit_cards_management"
            const val AddressEditor = "AddressEditor"
            const val AddressManagement = "AddressManagement"
        }
    }
}