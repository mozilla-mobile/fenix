/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

/**
 * Parent Reducer for all [QuickSettingsFragmentState]s of all Views shown in this Fragment.
 */
fun quickSettingsFragmentReducer(
    state: QuickSettingsFragmentState,
    action: QuickSettingsFragmentAction
): QuickSettingsFragmentState {
    return when (action) {
        is WebsiteInfoAction -> {
            // There is no possible action that can change this View's state while it is displayed to the user.
            // Every time the View is recreated it starts with a fresh state. This is the only way to display
            // something different.
            state
        }
        is WebsitePermissionAction -> state.copy(
            websitePermissionsState = WebsitePermissionsStateReducer.reduce(
                state.websitePermissionsState,
                action
            )
        )
    }
}

object WebsitePermissionsStateReducer {
    /**
     * Handles creating a new [WebsitePermissionsState] based on the specific [WebsitePermissionAction]
     */
    fun reduce(
        state: WebsitePermissionsState,
        action: WebsitePermissionAction
    ): WebsitePermissionsState {
        return when (action) {
            is WebsitePermissionAction.TogglePermission -> {
                val key = action.updatedFeature
                val newWebsitePermission = state.getValue(key).copy(
                    status = action.updatedStatus,
                    isEnabled = action.updatedEnabledStatus
                )

                state + Pair(key, newWebsitePermission)
            }
        }
    }
}
