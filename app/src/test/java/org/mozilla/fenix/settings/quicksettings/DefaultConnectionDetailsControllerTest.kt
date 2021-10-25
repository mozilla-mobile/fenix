/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.concept.engine.permission.SitePermissions
import mozilla.components.feature.session.TrackingProtectionUseCases
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class DefaultConnectionDetailsControllerTest {

    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var navController: NavController

    @MockK(relaxed = true)
    private lateinit var fragment: Fragment

    @MockK(relaxed = true)
    private lateinit var sitePermissions: SitePermissions

    private lateinit var controller: DefaultConnectionDetailsController

    private lateinit var tab: TabSessionState

    private var gravity = 54

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val trackingProtectionUseCases: TrackingProtectionUseCases = mockk(relaxed = true)
        context = spyk(testContext)
        tab = createTab("https://mozilla.org")
        controller = DefaultConnectionDetailsController(
            fragment = fragment,
            context = context,
            navController = { navController },
            sitePermissions = sitePermissions,
            gravity = gravity,
            getCurrentTab = { tab }
        )

        every { fragment.context } returns context
        every { context.components.useCases.trackingProtectionUseCases } returns trackingProtectionUseCases

        val onComplete = slot<(Boolean) -> Unit>()
        every {
            trackingProtectionUseCases.containsException.invoke(
                any(),
                capture(onComplete)
            )
        }.answers { onComplete.captured.invoke(true) }
    }

    @Test
    fun `WHEN handleBackPressed is called THEN should call popBackStack and navigate`() {
        controller.handleBackPressed()

        verify {
            navController.popBackStack()

            navController.navigate(any<NavDirections>())
        }
    }
}
