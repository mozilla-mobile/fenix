/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.content.Context
import androidx.annotation.VisibleForTesting
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.content.PermissionHighlightsState
import mozilla.components.concept.engine.permission.SitePermissions
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.Reducer
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.quicksettings.QuickSettingsFragmentStore.Companion.createStore
import org.mozilla.fenix.settings.quicksettings.WebsiteInfoState.Companion.createWebsiteInfoState
import org.mozilla.fenix.settings.quicksettings.ext.shouldBeEnabled
import org.mozilla.fenix.settings.quicksettings.ext.shouldBeVisible
import org.mozilla.fenix.trackingprotection.TrackingProtectionState
import org.mozilla.fenix.utils.Settings
import java.util.EnumMap

/**
 * [QuickSettingsSheetDialogFragment]'s unique [Store].
 * Encompasses it's own:
 *  - [State] for all Views displayed in this Fragment.
 *  - [Action]s mapping a user / system interaction to an intention to modify the above State.
 *  - [Reducer]s for modifying the above State based on the above Actions.
 *
 *  The [createStore] helper method can be used for creating one such [State] based on all current
 *  conditions of the app and web page visited.
 *
 * @param initialState [QuickSettingsFragmentState] that will be shown initially to the user.
 */
class QuickSettingsFragmentStore(
    initialState: QuickSettingsFragmentState
) : Store<QuickSettingsFragmentState, QuickSettingsFragmentAction>(
    initialState,
    ::quickSettingsFragmentReducer
) {
    companion object {
        /**
         * Construct an initial [QuickSettingsFragmentState] for all Views displayed by the
         * [QuickSettingsSheetDialogFragment].
         *
         * @param context [Context] used for access to various Android resources.
         * @param websiteUrl [String] the URL of the current web page.
         * @param websiteTitle [String] the title of the current web page.
         * @param isSecured [Boolean] whether the connection is secured (TLS) or not.
         * @param permissions [SitePermissions]? list of website permissions and their status.
         * @param settings [Settings] application settings.
         * @param certificateName [String] the certificate name of the current web page.
         * @param sessionId [String] The current session ID.
         * @param isTrackingProtectionEnabled [Boolean] Current status of tracking protection
         * for this session.
         */
        @Suppress("LongParameterList")
        fun createStore(
            context: Context,
            websiteUrl: String,
            websiteTitle: String,
            certificateName: String,
            isSecured: Boolean,
            permissions: SitePermissions?,
            permissionHighlights: PermissionHighlightsState,
            settings: Settings,
            sessionId: String,
            isTrackingProtectionEnabled: Boolean
        ) = QuickSettingsFragmentStore(
            QuickSettingsFragmentState(
                webInfoState = createWebsiteInfoState(
                    websiteUrl,
                    websiteTitle,
                    isSecured,
                    certificateName
                ),
                websitePermissionsState = createWebsitePermissionState(
                    context,
                    permissions,
                    permissionHighlights,
                    settings
                ),
                trackingProtectionState = createTrackingProtectionState(
                    context,
                    sessionId,
                    websiteUrl,
                    isTrackingProtectionEnabled
                )
            )
        )

        /**
         * Construct an initial [WebsitePermissions
         * State] to be rendered by [WebsitePermissionsView]
         * containing the permissions requested by the current website.
         *
         * Users can modify the returned [WebsitePermissionsState] after it is initially displayed.
         *
         * @param context [Context] used for various Android interactions.
         * @param permissions [SitePermissions]? list of website permissions and their status.
         * @param settings [Settings] application settings.
         */
        @VisibleForTesting
        fun createWebsitePermissionState(
            context: Context,
            permissions: SitePermissions?,
            permissionHighlights: PermissionHighlightsState,
            settings: Settings
        ): WebsitePermissionsState {
            val state = EnumMap<PhoneFeature, WebsitePermission>(PhoneFeature::class.java)
            for (feature in PhoneFeature.values()) {
                state[feature] = feature.toWebsitePermission(
                    context,
                    permissions,
                    permissionHighlights,
                    settings
                )
            }
            return state
        }

        /**
         * Construct an initial [TrackingProtectionState] to be rendered by
         * [TrackingProtectionView].
         *
         * @param context [Context] used for various Android interactions.
         * @param sessionId [String] The current session ID.
         * @param websiteUrl [String] the URL of the current web page.
         * @param isTrackingProtectionEnabled [Boolean] Current status of tracking protection
         * for this session.
         */
        @VisibleForTesting
        fun createTrackingProtectionState(
            context: Context,
            sessionId: String,
            websiteUrl: String,
            isTrackingProtectionEnabled: Boolean
        ): TrackingProtectionState {
            return TrackingProtectionState(
                tab = context.components.core.store.state.findTabOrCustomTab(sessionId),
                url = websiteUrl,
                isTrackingProtectionEnabled = isTrackingProtectionEnabled,
                listTrackers = listOf(),
                mode = TrackingProtectionState.Mode.Normal,
                lastAccessedCategory = ""
            )
        }

        /**
         * [PhoneFeature] to a [WebsitePermission] mapper.
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun PhoneFeature.toWebsitePermission(
            context: Context,
            permissions: SitePermissions?,
            permissionHighlights: PermissionHighlightsState,
            settings: Settings
        ): WebsitePermission {
            return if (this == PhoneFeature.AUTOPLAY) {
                val autoplayValues = AutoplayValue.values(context, settings, permissions)
                val selected =
                    autoplayValues.firstOrNull { it.isSelected() } ?: AutoplayValue.getFallbackValue(
                        context,
                        settings,
                        permissions
                    )
                WebsitePermission.Autoplay(
                    autoplayValue = selected,
                    options = autoplayValues,
                    isVisible = permissionHighlights.isAutoPlayBlocking || permissions !== null
                )
            } else {
                WebsitePermission.Toggleable(
                    phoneFeature = this,
                    status = getActionLabel(context, permissions, settings),
                    isVisible = shouldBeVisible(permissions, settings),
                    isEnabled = shouldBeEnabled(context, permissions, settings),
                    isBlockedByAndroid = !isAndroidPermissionGranted(context)
                )
            }
        }
    }
}
