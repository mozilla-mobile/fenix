/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.os.Build
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.widget.RadioButton
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelative
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Config.OLDEST_SDK

@RunWith(FenixRobolectricTestRunner::class)
class ExtensionsTest {

    @MockK(relaxUnitFun = true) private lateinit var radioButton: RadioButton
    @MockK private lateinit var fragment: PreferenceFragmentCompat
    private lateinit var preference: Preference

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        preference = Preference(testContext)

        every { radioButton.context } returns testContext
        every {
            fragment.getString(R.string.pref_key_accessibility_force_enable_zoom)
        } returns "pref_key_accessibility_force_enable_zoom"
    }

    @Test
    fun `test radiobutton setStartCheckedIndicator`() {
        radioButton.setStartCheckedIndicator()

        verify { radioButton.putCompoundDrawablesRelative(start = withArg {
            assertEquals(Rect(0, 0, it.intrinsicWidth, it.intrinsicHeight), it.bounds)
        }) }
    }

    @Test
    fun `set change listener with typed argument`() {
        val callback = mockk<(Preference, String) -> Unit>(relaxed = true)
        preference.setOnPreferenceChangeListener<String> { pref, value ->
            callback(pref, value)
            true
        }

        assertFalse(preference.callChangeListener(10))
        verify { callback wasNot Called }

        assertTrue(preference.callChangeListener("Hello"))
        verify { callback(preference, "Hello") }
    }

    @Test
    fun `requirePreference returns corresponding preference`() {
        val switchPreference = mockk<SwitchPreference>()
        every {
            fragment.findPreference<SwitchPreference>("pref_key_accessibility_auto_size")
        } returns switchPreference
    }

    @Test
    fun `requirePreference throws if null preference is returned`() {
        every {
            fragment.findPreference<SwitchPreference>("pref_key_accessibility_force_enable_zoom")
        } returns null

        var exception: IllegalArgumentException? = null
        try {
            fragment.requirePreference<SwitchPreference>(R.string.pref_key_accessibility_force_enable_zoom)
        } catch (e: IllegalArgumentException) {
            exception = e
        }

        assertNotNull(exception)
    }

    @Config(sdk = [Build.VERSION_CODES.O])
    @Test
    fun `create notificationsSettingsIntent on Oreo`() {
        val context = mockk<Context> {
            every { packageName } returns "org.mozilla.fenix"
        }

        val intent = notificationsSettingsIntent(context)
        assertEquals(ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals("org.mozilla.fenix", intent.getStringExtra(EXTRA_APP_PACKAGE))
    }

    @Config(sdk = [OLDEST_SDK])
    @Test
    fun `create notificationsSettingsIntent on Lollipop`() {
        val context = mockk<Context> {
            every { packageName } returns "org.mozilla.fenix"
            every { applicationInfo } returns ApplicationInfo().apply {
                uid = 1234
            }
        }

        val intent = notificationsSettingsIntent(context)
        assertEquals(ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals("org.mozilla.fenix", intent.getStringExtra("app_package"))
        assertEquals(1234, intent.getIntExtra("app_uid", 0))
    }
}
