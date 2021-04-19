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

    @Before
    fun setUp() {
        context = testContext
        accountManagerComponent = mockk(relaxed = true)
    }

    @Test
    fun `GIVEN an account exists THEN fetch the associated email address`() {
        val emailAddress = "firefoxIsFun@test.com"

//        val profile = Profile(
//            uid = "000",
//            email = emailAddress,
//            avatar = null,
//            displayName = "name"
//        )
//        every { fenixFxaManager.accountProfile } returns profile

        fenixFxaManager = spyk(FenixAccountManager(context))

        every { fenixFxaManager.accountManager } returns accountManagerComponent
        every { context.components.backgroundServices.accountManager } returns accountManagerComponent

        every { accountManagerComponent.accountProfile()?.email } returns emailAddress
        every { fenixFxaManager.accountProfile?.email } returns emailAddress

        every { accountManagerComponent.authenticatedAccount() != null } returns true
        every { accountManagerComponent.authenticatedAccount() } returns mockk(relaxed = true)
        every { fenixFxaManager.authenticatedAccount } returns true

        val result = fenixFxaManager.getAuthAccountEmail()
        // accountProfile?.email is always "". I wonder if the way we are fetching it needs to be async
        assertEquals(emailAddress, result)
    }

    @Test
    fun `GIVEN an account does not exist THEN return null when fetching the associated email address`() {
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
