/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES
import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityManager
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import mozilla.components.feature.sitepermissions.SitePermissionsRules
import mozilla.components.feature.sitepermissions.SitePermissionsRules.Action
import mozilla.components.feature.sitepermissions.SitePermissionsRules.AutoplayAction
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.booleanPreference
import mozilla.components.support.ktx.android.content.floatPreference
import mozilla.components.support.ktx.android.content.intPreference
import mozilla.components.support.ktx.android.content.longPreference
import mozilla.components.support.ktx.android.content.stringPreference
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.Config
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.metrics.MozillaProductDetector
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.deletebrowsingdata.DeleteBrowsingDataOnQuitType
import java.security.InvalidParameterException

/**
 * A simple wrapper for SharedPreferences that makes reading preference a little bit easier.
 */
@Suppress("LargeClass", "TooManyFunctions")
class Settings private constructor(
    context: Context,
    private val isCrashReportEnabledInBuild: Boolean
) : PreferencesHolder {
    companion object {
        const val showLoginsSecureWarningSyncMaxCount = 1
        const val showLoginsSecureWarningMaxCount = 1
        const val trackingProtectionOnboardingMaximumCount = 2
        const val FENIX_PREFERENCES = "fenix_preferences"

        private const val BLOCKED_INT = 0
        private const val ASK_TO_ALLOW_INT = 1
        private const val ALLOWED_INT = 2
        private const val CFR_COUNT_CONDITION_FOCUS_INSTALLED = 1
        private const val CFR_COUNT_CONDITION_FOCUS_NOT_INSTALLED = 3

        private fun Action.toInt() = when (this) {
            Action.BLOCKED -> BLOCKED_INT
            Action.ASK_TO_ALLOW -> ASK_TO_ALLOW_INT
            Action.ALLOWED -> ALLOWED_INT
        }

        private fun AutoplayAction.toInt() = when (this) {
            AutoplayAction.BLOCKED -> BLOCKED_INT
            AutoplayAction.ALLOWED -> ALLOWED_INT
        }

        private fun Int.toAction() = when (this) {
            BLOCKED_INT -> Action.BLOCKED
            ASK_TO_ALLOW_INT -> Action.ASK_TO_ALLOW
            ALLOWED_INT -> Action.ALLOWED
            else -> throw InvalidParameterException("$this is not a valid SitePermissionsRules.Action")
        }

        private fun Int.toAutoplayAction() = when (this) {
            BLOCKED_INT -> AutoplayAction.BLOCKED
            ALLOWED_INT -> AutoplayAction.ALLOWED
            // Users from older versions may have saved invalid values. Migrate them to BLOCKED
            ASK_TO_ALLOW_INT -> AutoplayAction.BLOCKED
            else -> throw InvalidParameterException("$this is not a valid SitePermissionsRules.AutoplayAction")
        }

        @VisibleForTesting
        internal var instance: Settings? = null

        @JvmStatic
        @Synchronized
        fun getInstance(
            context: Context,
            isCrashReportEnabledInBuild: Boolean = BuildConfig.CRASH_REPORTING && Config.channel.isReleased
        ): Settings {
            if (instance == null) {
                instance = Settings(context.applicationContext, isCrashReportEnabledInBuild)
            }
            return instance ?: throw AssertionError("Instance cleared")
        }
    }

    private val appContext = context.applicationContext

    override val preferences: SharedPreferences =
        appContext.getSharedPreferences(FENIX_PREFERENCES, MODE_PRIVATE)

    var forceEnableZoom by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_accessibility_force_enable_zoom),
        default = false
    )

    var adjustCampaignId by stringPreference(
        appContext.getPreferenceKey(R.string.pref_key_adjust_campaign),
        default = ""
    )

    var adjustNetwork by stringPreference(
        appContext.getPreferenceKey(R.string.pref_key_adjust_network),
        default = ""
    )

    var adjustAdGroup by stringPreference(
        appContext.getPreferenceKey(R.string.pref_key_adjust_adgroup),
        default = ""
    )

    var adjustCreative by stringPreference(
        appContext.getPreferenceKey(R.string.pref_key_adjust_creative),
        default = ""
    )

    var openLinksInAPrivateTab by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_open_links_in_a_private_tab),
        default = false
    )

    var defaultSearchEngineName by stringPreference(
        appContext.getPreferenceKey(R.string.pref_key_search_engine),
        default = ""
    )

    var readerModeOpened by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_reader_mode_opened),
        default = false
    )

    var openInAppOpened by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_open_in_app_opened),
        default = false
    )

    var installPwaOpened by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_install_pwa_opened),
        default = false
    )

    val isCrashReportingEnabled: Boolean
        get() = isCrashReportEnabledInBuild &&
            preferences.getBoolean(
                appContext.getPreferenceKey(R.string.pref_key_crash_reporter),
                true
            )

    val isRemoteDebuggingEnabled by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_remote_debugging),
        default = false
    )

    val isTelemetryEnabled by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_telemetry),
        default = true
    )

    val isMarketingTelemetryEnabled by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_marketing_telemetry),
        default = true
    )

    val isExperimentationEnabled by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_experimentation),
        default = true
    )

    val isAutoPlayEnabled = getSitePermissionsPhoneFeatureAction(
        PhoneFeature.AUTOPLAY_AUDIBLE, Action.BLOCKED
    ) != Action.BLOCKED

    private var trackingProtectionOnboardingShownThisSession = false
    var isOverrideTPPopupsForPerformanceTest = false

    val shouldShowTrackingProtectionOnboarding: Boolean
        get() = !isOverrideTPPopupsForPerformanceTest &&
                (trackingProtectionOnboardingCount < trackingProtectionOnboardingMaximumCount &&
                !trackingProtectionOnboardingShownThisSession)

    val shouldShowSecurityPinWarningSync: Boolean
        get() = loginsSecureWarningSyncCount < showLoginsSecureWarningSyncMaxCount

    val shouldShowSecurityPinWarning: Boolean
        get() = loginsSecureWarningCount < showLoginsSecureWarningMaxCount

    var shouldUseLightTheme by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_light_theme),
        default = false
    )

    var shouldUseAutoSize by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_accessibility_auto_size),
        default = true
    )

    var fontSizeFactor by floatPreference(
        appContext.getPreferenceKey(R.string.pref_key_accessibility_font_scale),
        default = 1f
    )

    val shouldShowHistorySuggestions by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_search_browsing_history),
        default = true
    )

    val shouldShowBookmarkSuggestions by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_search_bookmarks),
        default = true
    )

    val shouldShowClipboardSuggestions by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_show_clipboard_suggestions),
        default = true
    )

    val shouldShowSearchShortcuts by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_show_search_shortcuts),
        default = true
    )

    val shouldUseDarkTheme by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_dark_theme),
        default = false
    )

    var shouldFollowDeviceTheme by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_follow_device_theme),
        default = false
    )

    var shouldUseTrackingProtection by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection),
        default = true
    )

    val shouldUseAutoBatteryTheme by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_auto_battery_theme),
        default = false
    )

    val useStrictTrackingProtection by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_strict_default),
        true
    )

    val useCustomTrackingProtection by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_custom_option),
        false
    )

    val blockCookiesInCustomTrackingProtection by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_custom_cookies),
        true
    )

    val blockCookiesSelectionInCustomTrackingProtection by stringPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_custom_cookies_select),
        "social"
    )

    val blockTrackingContentInCustomTrackingProtection by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_custom_tracking_content),
        true
    )

    val blockTrackingContentSelectionInCustomTrackingProtection by stringPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_custom_tracking_content_select),
        "all"
    )

    val blockCryptominersInCustomTrackingProtection by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_custom_cryptominers),
        true
    )

    val blockFingerprintersInCustomTrackingProtection by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_custom_fingerprinters),
        true
    )

    val shouldUseFixedTopToolbar: Boolean
        get() {
            return touchExplorationIsEnabled || switchServiceIsEnabled
        }

    var lastKnownMode: BrowsingMode = BrowsingMode.Normal
        get() {
            val lastKnownModeWasPrivate = preferences.getBoolean(
                appContext.getPreferenceKey(R.string.pref_key_last_known_mode_private),
                false
            )

            return if (lastKnownModeWasPrivate) {
                BrowsingMode.Private
            } else {
                BrowsingMode.Normal
            }
        }

        set(value) {
            val lastKnownModeWasPrivate = (value == BrowsingMode.Private)

            preferences.edit()
                .putBoolean(
                appContext.getPreferenceKey(R.string.pref_key_last_known_mode_private),
                    lastKnownModeWasPrivate)
                .apply()

            field = value
        }

    var shouldDeleteBrowsingDataOnQuit by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_delete_browsing_data_on_quit),
        default = false
    )

    var shouldUseBottomToolbar by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_toolbar_bottom),
        // Default accessibility users to top toolbar
        default = !touchExplorationIsEnabled && !switchServiceIsEnabled
    )

    /**
     * Check each active accessibility service to see if it can perform gestures, if any can,
     * then it is *likely* a switch service is enabled. We are assuming this to be the case based on #7486
     */
    private val switchServiceIsEnabled: Boolean
        get() {
            val accessibilityManager =
                appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

            accessibilityManager?.getEnabledAccessibilityServiceList(0)?.let { activeServices ->
                for (service in activeServices) {
                    if (service.capabilities.and(CAPABILITY_CAN_PERFORM_GESTURES) == 1) {
                        return true
                    }
                }
            }

            return false
        }

    private val touchExplorationIsEnabled: Boolean
        get() {
            val accessibilityManager =
                appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            return accessibilityManager?.isTouchExplorationEnabled ?: false
        }

    val accessibilityServicesEnabled: Boolean
        get() { return touchExplorationIsEnabled || switchServiceIsEnabled }

    val toolbarSettingString: String
        get() = when {
            shouldUseBottomToolbar -> appContext.getString(R.string.preference_bottom_toolbar)
            else -> appContext.getString(R.string.preference_top_toolbar)
        }

    fun getDeleteDataOnQuit(type: DeleteBrowsingDataOnQuitType): Boolean =
        preferences.getBoolean(type.getPreferenceKey(appContext), false)

    fun setDeleteDataOnQuit(type: DeleteBrowsingDataOnQuitType, value: Boolean) {
        preferences.edit().putBoolean(type.getPreferenceKey(appContext), value).apply()
    }

    fun shouldDeleteAnyDataOnQuit() =
        DeleteBrowsingDataOnQuitType.values().any { getDeleteDataOnQuit(it) }

    val passwordsEncryptionKeyGenerated by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_encryption_key_generated),
        false
    )

    fun recordPasswordsEncryptionKeyGenerated() = preferences.edit().putBoolean(
        appContext.getPreferenceKey(R.string.pref_key_encryption_key_generated),
        true
    ).apply()

    @VisibleForTesting(otherwise = PRIVATE)
    internal val loginsSecureWarningSyncCount by intPreference(
        appContext.getPreferenceKey(R.string.pref_key_logins_secure_warning_sync),
        default = 0
    )

    @VisibleForTesting(otherwise = PRIVATE)
    internal val loginsSecureWarningCount by intPreference(
        appContext.getPreferenceKey(R.string.pref_key_logins_secure_warning),
        default = 0
    )

    fun incrementShowLoginsSecureWarningCount() {
        preferences.edit().putInt(
            appContext.getPreferenceKey(R.string.pref_key_logins_secure_warning),
            loginsSecureWarningCount + 1
        ).apply()
    }

    fun incrementShowLoginsSecureWarningSyncCount() {
        preferences.edit().putInt(
            appContext.getPreferenceKey(R.string.pref_key_logins_secure_warning_sync),
            loginsSecureWarningSyncCount + 1
        ).apply()
    }

    val shouldShowSearchSuggestions by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_show_search_suggestions),
        default = true
    )

    val defaultTopSitesAdded by booleanPreference(
        appContext.getPreferenceKey(R.string.default_top_sites_added),
        default = false
    )

    var shouldShowSearchSuggestionsInPrivate by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_show_search_suggestions_in_private),
        default = false
    )

    var showSearchSuggestionsInPrivateOnboardingFinished by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_show_search_suggestions_in_private_onboarding),
        default = false
    )

    @VisibleForTesting(otherwise = PRIVATE)
    internal val trackingProtectionOnboardingCount by intPreference(
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_onboarding),
        0
    )

    fun incrementTrackingProtectionOnboardingCount() {
        trackingProtectionOnboardingShownThisSession = true
        preferences.edit().putInt(
            appContext.getPreferenceKey(R.string.pref_key_tracking_protection_onboarding),
            trackingProtectionOnboardingCount + 1
        ).apply()
    }

    fun getSitePermissionsPhoneFeatureAction(
        feature: PhoneFeature,
        default: Action = Action.ASK_TO_ALLOW
    ) =
        preferences.getInt(feature.getPreferenceKey(appContext), default.toInt()).toAction()

    fun getSitePermissionsPhoneFeatureAutoplayAction(
        feature: PhoneFeature,
        default: AutoplayAction = AutoplayAction.BLOCKED
    ) = preferences.getInt(feature.getPreferenceKey(appContext), default.toInt()).toAutoplayAction()

    fun setSitePermissionsPhoneFeatureAction(
        feature: PhoneFeature,
        value: Action
    ) {
        preferences.edit().putInt(feature.getPreferenceKey(appContext), value.toInt()).apply()
    }

    fun getSitePermissionsCustomSettingsRules(): SitePermissionsRules {
        return SitePermissionsRules(
            notification = getSitePermissionsPhoneFeatureAction(PhoneFeature.NOTIFICATION),
            microphone = getSitePermissionsPhoneFeatureAction(PhoneFeature.MICROPHONE),
            location = getSitePermissionsPhoneFeatureAction(PhoneFeature.LOCATION),
            camera = getSitePermissionsPhoneFeatureAction(PhoneFeature.CAMERA),
            autoplayAudible = getSitePermissionsPhoneFeatureAutoplayAction(PhoneFeature.AUTOPLAY_AUDIBLE),
            // TODO autoplayInaudible will be hardcoded until additional options are added in #8017
            autoplayInaudible = AutoplayAction.ALLOWED
        )
    }

    var shouldPromptToSaveLogins by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_save_logins),
        default = true
    )

    var shouldAutofillLogins by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_autofill_logins),
        default = true
    )

    var fxaSignedIn by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_fxa_signed_in),
        default = false
    )

    var fxaHasSyncedItems by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_fxa_has_synced_items),
        default = false
    )

    var lastPlacesStorageMaintenance by longPreference(
        appContext.getPreferenceKey(R.string.pref_key_last_maintenance),
        default = 0
    )

    fun addSearchWidgetInstalled(count: Int) {
        val key = appContext.getPreferenceKey(R.string.pref_key_search_widget_installed)
        val newValue = preferences.getInt(key, 0) + count
        preferences.edit()
            .putInt(key, newValue)
            .apply()
    }

    val searchWidgetInstalled: Boolean
        get() = 0 < preferences.getInt(
            appContext.getPreferenceKey(R.string.pref_key_search_widget_installed),
            0
        )

    fun incrementNumTimesPrivateModeOpened() {
        preferences.edit().putInt(
            appContext.getPreferenceKey(R.string.pref_key_private_mode_opened),
            numTimesPrivateModeOpened + 1
        ).apply()
    }

    private var showedPrivateModeContextualFeatureRecommender by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_showed_private_mode_cfr),
        default = false
    )

    private val numTimesPrivateModeOpened: Int
        get() = preferences.getInt(
            appContext.getPreferenceKey(R.string.pref_key_private_mode_opened),
            0
        )

    val showPrivateModeContextualFeatureRecommender: Boolean
        get() {
            val focusInstalled = MozillaProductDetector
                .getInstalledMozillaProducts(appContext as Application)
                .contains(MozillaProductDetector.MozillaProducts.FOCUS.productName)

            val showCondition =
                (numTimesPrivateModeOpened == CFR_COUNT_CONDITION_FOCUS_INSTALLED && focusInstalled) ||
                        (numTimesPrivateModeOpened == CFR_COUNT_CONDITION_FOCUS_NOT_INSTALLED && !focusInstalled)

            if (showCondition && !showedPrivateModeContextualFeatureRecommender) {
                showedPrivateModeContextualFeatureRecommender = true
                return true
            }

            return false
        }

    var openLinksInExternalApp by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_open_links_in_external_app),
        default = false
    )
}
