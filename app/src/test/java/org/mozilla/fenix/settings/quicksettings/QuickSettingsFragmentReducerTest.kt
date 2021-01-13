/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.settings.PhoneFeature

@RunWith(FenixRobolectricTestRunner::class)
class QuickSettingsFragmentReducerTest {

    @Test
    fun `WebsitePermissionAction - TogglePermission`() {
        val toggleablePermission = WebsitePermission.Toggleable(
            phoneFeature = PhoneFeature.CAMERA,
            status = "status",
            isVisible = false,
            isEnabled = false,
            isBlockedByAndroid = false
        )

        val map =
            mapOf<PhoneFeature, WebsitePermission>(PhoneFeature.CAMERA to toggleablePermission)

        val state = QuickSettingsFragmentState(mock(), map)
        val newState = quickSettingsFragmentReducer(
            state,
            WebsitePermissionAction.TogglePermission(
                updatedFeature = PhoneFeature.CAMERA,
                updatedStatus = "newStatus",
                updatedEnabledStatus = true
            )
        )
        val result = newState.websitePermissionsState[PhoneFeature.CAMERA]!!
        assertEquals("newStatus", result.status)
        assertTrue(result.isEnabled)
    }

    @Test
    fun `WebsitePermissionAction - ChangeAutoplay`() {
        val permissionPermission = WebsitePermission.Autoplay(
            autoplayValue = AutoplayValue.BlockAll(
                label = "label",
                rules = mock(),
                sitePermission = null
            ),
            options = emptyList(),
            isVisible = false
        )

        val map =
            mapOf<PhoneFeature, WebsitePermission>(PhoneFeature.AUTOPLAY to permissionPermission)

        val state = QuickSettingsFragmentState(mock(), map)
        val autoplayValue = AutoplayValue.AllowAll(
            label = "newLabel",
            rules = mock(),
            sitePermission = null
        )
        val newState = quickSettingsFragmentReducer(
            state,
            WebsitePermissionAction.ChangeAutoplay(autoplayValue)
        )

        val result =
            newState.websitePermissionsState[PhoneFeature.AUTOPLAY] as WebsitePermission.Autoplay
        assertEquals(autoplayValue, result.autoplayValue)
    }
}
