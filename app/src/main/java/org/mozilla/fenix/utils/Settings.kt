/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import mozilla.components.feature.sitepermissions.SitePermissionsRules
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.booleanPreference
import mozilla.components.support.ktx.android.content.floatPreference
import mozilla.components.support.ktx.android.content.intPreference
import mozilla.components.support.ktx.android.content.stringPreference
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.Config
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.MozillaProductDetector
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.settings.PhoneFeature
import java.security.InvalidParameterException

/**
 * A simple wrapper for SharedPreferences that makes reading preference a little bit easier.
 */
class Settings private constructor(
    context: Context,
    private val isCrashReportEnabledInBuild: Boolean
) : PreferencesHolder {
    companion object {
        const val autoBounceMaximumCount = 2
        const val trackingProtectionOnboardingMaximumCount = 2
        const val FENIX_PREFERENCES = "fenix_preferences"
        private const val BLOCKED_INT = 0
        private const val ASK_TO_ALLOW_INT = 1
        private const val CFR_COUNT_CONDITION_FOCUS_INSTALLED = 1
        private const val CFR_COUNT_CONDITION_FOCUS_NOT_INSTALLED = 3

        private fun actionToInt(action: SitePermissionsRules.Action) = when (action) {
            SitePermissionsRules.Action.BLOCKED -> BLOCKED_INT
            SitePermissionsRules.Action.ASK_TO_ALLOW -> ASK_TO_ALLOW_INT
        }

        private fun intToAction(action: Int) = when (action) {
            BLOCKED_INT -> SitePermissionsRules.Action.BLOCKED
            ASK_TO_ALLOW_INT -> SitePermissionsRules.Action.ASK_TO_ALLOW
            else -> throw InvalidParameterException("$action is not a valid SitePermissionsRules.Action")
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

    var usePrivateMode by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_private_mode),
        default = false
    )

    var defaultSearchEngineName by stringPreference(
        appContext.getPreferenceKey(R.string.pref_key_search_engine),
        default = ""
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

    val isExperimentationEnabled by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_experimentation),
        default = true
    )

    private var trackingProtectionOnboardingShownThisSession = false

    val shouldShowTrackingProtectionOnboarding: Boolean
        get() = trackingProtectionOnboardingCount < trackingProtectionOnboardingMaximumCount &&
                !trackingProtectionOnboardingShownThisSession

    val shouldAutoBounceQuickActionSheet: Boolean
        get() = autoBounceQuickActionSheetCount < autoBounceMaximumCount

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
        appContext.getPreferenceKey(R.string.pref_key_tracking_protection_strict),
        false
    )

    val themeSettingString: String
        get() = when {
            shouldFollowDeviceTheme -> appContext.getString(R.string.preference_follow_device_theme)
            shouldUseAutoBatteryTheme -> appContext.getString(R.string.preference_auto_battery_theme)
            shouldUseDarkTheme -> appContext.getString(R.string.preference_dark_theme)
            shouldUseLightTheme -> appContext.getString(R.string.preference_light_theme)
            else -> appContext.getString(R.string.preference_light_theme)
        }

    @VisibleForTesting(otherwise = PRIVATE)
    internal val autoBounceQuickActionSheetCount by intPreference(
        appContext.getPreferenceKey(R.string.pref_key_bounce_quick_action),
        default = 0
    )

    fun incrementAutomaticBounceQuickActionSheetCount() {
        preferences.edit().putInt(
            appContext.getPreferenceKey(R.string.pref_key_bounce_quick_action),
            autoBounceQuickActionSheetCount + 1
        ).apply()
    }

    val shouldShowSearchSuggestions by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_show_search_suggestions),
        default = true
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

    fun getSitePermissionsPhoneFeatureAction(feature: PhoneFeature) =
        intToAction(preferences.getInt(feature.getPreferenceKey(appContext), ASK_TO_ALLOW_INT))

    fun setSitePermissionsPhoneFeatureAction(
        feature: PhoneFeature,
        value: SitePermissionsRules.Action
    ) {
        preferences.edit().putInt(feature.getPreferenceKey(appContext), actionToInt(value)).apply()
    }

    fun getSitePermissionsCustomSettingsRules(): SitePermissionsRules {
        return SitePermissionsRules(
            notification = getSitePermissionsPhoneFeatureAction(PhoneFeature.NOTIFICATION),
            microphone = getSitePermissionsPhoneFeatureAction(PhoneFeature.MICROPHONE),
            location = getSitePermissionsPhoneFeatureAction(PhoneFeature.LOCATION),
            camera = getSitePermissionsPhoneFeatureAction(PhoneFeature.CAMERA)
        )
    }

    var fxaSignedIn by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_fxa_signed_in),
        default = true
    )

    var fxaHasSyncedItems by booleanPreference(
        appContext.getPreferenceKey(R.string.pref_key_fxa_has_synced_items),
        default = true
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
        get() = preferences.getInt(appContext.getPreferenceKey(R.string.pref_key_private_mode_opened), 0)

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
}
