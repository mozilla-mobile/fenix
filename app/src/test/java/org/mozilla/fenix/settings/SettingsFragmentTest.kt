/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.advanceUntilIdle
import mozilla.components.concept.fetch.Client
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import java.io.IOException

@RunWith(FenixRobolectricTestRunner::class)
class SettingsFragmentTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()
    private val settingsFragment = SettingsFragment()

    @Before
    fun setup() {
        // Mock client for fetching account avatar
        val client = mockk<Client>()
        every { client.fetch(any()) } throws IOException("test")

        every { testContext.components.core.engine.profiler } returns mockk(relaxed = true)
        every { testContext.components.core.client } returns client
        every { testContext.components.settings } returns mockk(relaxed = true)
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.backgroundServices } returns mockk(relaxed = true)

        mockkObject(FeatureFlags)

        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        activity.supportFragmentManager.beginTransaction()
            .add(settingsFragment, "test")
            .commitNow()
    }

    @Test
    fun `Add-on collection override pref is visible if debug menu active and feature is enabled`() = runTestOnMain {
        val settingsFragment = SettingsFragment()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        activity.supportFragmentManager.beginTransaction()
            .add(settingsFragment, "test")
            .commitNow()

        advanceUntilIdle()

        every { FeatureFlags.customExtensionCollectionFeature } returns true

        val preferenceAmoCollectionOverride = settingsFragment.findPreference<Preference>(
            settingsFragment.getPreferenceKey(R.string.pref_key_override_amo_collection),
        )

        settingsFragment.setupAmoCollectionOverridePreference(mockk(relaxed = true))
        assertNotNull(preferenceAmoCollectionOverride)
        assertFalse(preferenceAmoCollectionOverride!!.isVisible)

        val settings: Settings = mockk(relaxed = true)
        every { settings.showSecretDebugMenuThisSession } returns true
        settingsFragment.setupAmoCollectionOverridePreference(settings)
        assertTrue(preferenceAmoCollectionOverride.isVisible)
    }

    @Test
    fun `Add-on collection override pref is visible if already configured and feature is enabled`() = runTestOnMain {
        val settingsFragment = SettingsFragment()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        activity.supportFragmentManager.beginTransaction()
            .add(settingsFragment, "test")
            .commitNow()

        advanceUntilIdle()

        every { FeatureFlags.customExtensionCollectionFeature } returns true

        val preferenceAmoCollectionOverride = settingsFragment.findPreference<Preference>(
            settingsFragment.getPreferenceKey(R.string.pref_key_override_amo_collection),
        )

        settingsFragment.setupAmoCollectionOverridePreference(mockk(relaxed = true))
        assertNotNull(preferenceAmoCollectionOverride)
        assertFalse(preferenceAmoCollectionOverride!!.isVisible)

        val settings: Settings = mockk(relaxed = true)
        every { settings.showSecretDebugMenuThisSession } returns false

        every { settings.amoCollectionOverrideConfigured() } returns false
        settingsFragment.setupAmoCollectionOverridePreference(settings)
        assertFalse(preferenceAmoCollectionOverride.isVisible)

        every { settings.amoCollectionOverrideConfigured() } returns true
        settingsFragment.setupAmoCollectionOverridePreference(settings)
        assertTrue(preferenceAmoCollectionOverride.isVisible)
    }

    @Test
    fun `Add-on collection override pref is not visible if feature is disabled`() = runTestOnMain {
        val settingsFragment = SettingsFragment()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        activity.supportFragmentManager.beginTransaction()
            .add(settingsFragment, "test")
            .commitNow()

        advanceUntilIdle()

        every { FeatureFlags.customExtensionCollectionFeature } returns false

        val preferenceAmoCollectionOverride = settingsFragment.findPreference<Preference>(
            settingsFragment.getPreferenceKey(R.string.pref_key_override_amo_collection),
        )

        val settings: Settings = mockk(relaxed = true)
        settingsFragment.setupAmoCollectionOverridePreference(settings)
        assertNotNull(preferenceAmoCollectionOverride)
        assertFalse(preferenceAmoCollectionOverride!!.isVisible)

        every { settings.showSecretDebugMenuThisSession } returns true
        every { settings.amoCollectionOverrideConfigured() } returns true
        settingsFragment.setupAmoCollectionOverridePreference(settings)
        assertFalse(preferenceAmoCollectionOverride.isVisible)
    }

    private fun testHttpsOnlySummary(summary: String) {
        val httpsOnlyPreference = settingsFragment.findPreference<Preference>(
            settingsFragment.getPreferenceKey(R.string.pref_key_https_only_settings),
        )!!

        assertTrue(httpsOnlyPreference.summary.isNullOrEmpty())

        settingsFragment.setupHttpsOnlyPreferences()

        assertEquals(summary, httpsOnlyPreference.summary)
    }

    @Test
    fun `GIVEN the HttpsOnly is enabled THEN set the appropriate preference summary`() {
        every { testContext.settings().shouldUseHttpsOnly } returns true
        every { testContext.settings().shouldUseHttpsOnlyInPrivateTabsOnly } returns false
        testHttpsOnlySummary(
            summary = testContext.getString(R.string.preferences_https_only_on),
        )
    }

    @Test
    fun `GIVEN the HttpsOnly is disabled THEN set the appropriate preference summary`() {
        every { testContext.settings().shouldUseHttpsOnly } returns false
        every { testContext.settings().shouldUseHttpsOnlyInPrivateTabsOnly } returns false
        testHttpsOnlySummary(
            summary = testContext.getString(R.string.preferences_https_only_off),
        )
    }

    @Test
    fun `GIVEN the HttpsOnly is enabled in private tabs only THEN set the appropriate preference summary`() {
        every { testContext.settings().shouldUseHttpsOnly } returns true
        every { testContext.settings().shouldUseHttpsOnlyInPrivateTabsOnly } returns true
        testHttpsOnlySummary(
            summary = testContext.getString(R.string.preferences_https_only_in_private_tabs),
        )
    }

    private fun testTrackingProtectionSummary(summary: String) {
        val trackingProtectionPreference = settingsFragment.findPreference<Preference>(
            settingsFragment.getPreferenceKey(R.string.pref_key_tracking_protection_settings),
        )!!

        assertTrue(trackingProtectionPreference.summary.isNullOrEmpty())

        settingsFragment.setupTrackingProtectionPreference()

        assertEquals(summary, trackingProtectionPreference.summary)
    }

    @Test
    fun `GIVEN TrackingProtection is enabled THEN set the appropriate preference summary`() {
        every { testContext.settings().shouldUseTrackingProtection } returns true
        every { testContext.settings().useStrictTrackingProtection } returns false
        every { testContext.settings().useCustomTrackingProtection } returns false
        testTrackingProtectionSummary(
            summary = testContext.getString(R.string.tracking_protection_on),
        )
    }

    @Test
    fun `GIVEN TrackingProtection is disabled THEN set the appropriate preference summary`() {
        every { testContext.settings().shouldUseTrackingProtection } returns false
        every { testContext.settings().useStrictTrackingProtection } returns false
        every { testContext.settings().useCustomTrackingProtection } returns false
        testTrackingProtectionSummary(
            summary = testContext.getString(R.string.tracking_protection_off),
        )
    }

    @Test
    fun `GIVEN strict TrackingProtection is enabled THEN set the appropriate preference summary`() {
        every { testContext.settings().shouldUseTrackingProtection } returns true
        every { testContext.settings().useStrictTrackingProtection } returns true
        every { testContext.settings().useCustomTrackingProtection } returns false
        testTrackingProtectionSummary(
            summary = testContext.getString(R.string.preference_enhanced_tracking_protection_strict),
        )
    }

    @Test
    fun `GIVEN custom TrackingProtection is enabled THEN set the appropriate preference summary`() {
        every { testContext.settings().shouldUseTrackingProtection } returns true
        every { testContext.settings().useStrictTrackingProtection } returns false
        every { testContext.settings().useCustomTrackingProtection } returns true
        testTrackingProtectionSummary(
            summary = testContext.getString(R.string.preference_enhanced_tracking_protection_custom),
        )
    }

    @After
    fun tearDown() {
        unmockkObject(FeatureFlags)
    }
}
