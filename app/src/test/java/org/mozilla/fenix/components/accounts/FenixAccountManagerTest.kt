/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.accounts

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import mozilla.components.concept.sync.AccessTokenInfo
import mozilla.components.concept.sync.AuthFlowUrl
import mozilla.components.concept.sync.DeviceConstellation
import mozilla.components.concept.sync.InFlightMigrationState
import mozilla.components.concept.sync.MigratingAccountInfo
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.concept.sync.StatePersistenceCallback
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class FenixAccountManagerTest {

    private lateinit var fenixFxaManager: FenixAccountManager
    private lateinit var accountManagerComponent: FxaAccountManager
    private lateinit var context: Context
    private lateinit var account: OAuthAccount
    private lateinit var profile: Profile

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        account = mockk(relaxed = true)
        profile = mockk(relaxed = true)
        accountManagerComponent = mockk(relaxed = true)
    }

    @Test
    fun `GIVEN an account exists THEN fetch the associated email address`() {
        every { accountManagerComponent.authenticatedAccount() } returns account
        every { accountManagerComponent.accountProfile() } returns profile
        every { context.components.backgroundServices.accountManager } returns accountManagerComponent

        fenixFxaManager = FenixAccountManager(context)

        val emailAddress = "firefoxIsFun@test.com"
        every { accountManagerComponent.accountProfile()?.email } returns emailAddress

        val result = fenixFxaManager.getAuthAccountEmail()
        assertEquals(emailAddress, result)
    }

    @Test
    fun `GIVEN an account does not exist THEN return null when fetching the associated email address`() {
        every { accountManagerComponent.authenticatedAccount() } returns null
        every { accountManagerComponent.accountProfile() } returns null
        every { context.components.backgroundServices.accountManager } returns accountManagerComponent

        fenixFxaManager = FenixAccountManager(context)

        val emailAddress = "firefoxIsFun@test.com"
        every { accountManagerComponent.accountProfile()?.email } returns emailAddress

        val result = fenixFxaManager.getAuthAccountEmail()
        assertEquals(null, result)
    }

    @Test
    fun `GIVEN an account is signed in and authenticated THEN check returns true`() {
        val signedIn = fenixFxaManager.signedInToFxa()
        assertTrue(signedIn)
    }

    @Test
    fun `GIVEN an account is signed in and NOT authenticated THEN check returns false`() {
        every { fenixFxaManager.authenticatedAccount } returns mockk()
        every { accountManagerComponent.accountNeedsReauth() } returns true
        val signedIn = fenixFxaManager.signedInToFxa()
        assertFalse(signedIn)
    }

    @Test
    fun `GIVEN an account is not signed in THEN check returns false`() {
        val signedIn = fenixFxaManager.signedInToFxa()
        assertFalse(signedIn)
    }
}
