/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings.protections.cookiebanners

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.cookiehandling.CookieBannersStorage
import mozilla.components.concept.engine.permission.SitePermissions
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.session.TrackingProtectionUseCases
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.CookieBanners
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.trackingprotection.ProtectionsAction
import org.mozilla.fenix.trackingprotection.ProtectionsStore

@RunWith(FenixRobolectricTestRunner::class)
internal class DefaultCookieBannerDetailsControllerTest {

    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var navController: NavController

    @MockK(relaxed = true)
    private lateinit var fragment: Fragment

    @MockK(relaxed = true)
    private lateinit var sitePermissions: SitePermissions

    @MockK(relaxed = true)
    private lateinit var cookieBannersStorage: CookieBannersStorage

    private lateinit var controller: DefaultCookieBannerDetailsController

    private lateinit var tab: TabSessionState

    private lateinit var browserStore: BrowserStore

    @MockK(relaxed = true)
    private lateinit var protectionsStore: ProtectionsStore

    @MockK(relaxed = true)
    private lateinit var reload: SessionUseCases.ReloadUrlUseCase

    @MockK(relaxed = true)
    private lateinit var engine: Engine

    @MockK(relaxed = true)
    private lateinit var publicSuffixList: PublicSuffixList

    private var gravity = 54

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()
    private val scope = coroutinesTestRule.scope

    @get:Rule
    val gleanRule = GleanTestRule(testContext)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val trackingProtectionUseCases: TrackingProtectionUseCases = mockk(relaxed = true)
        context = spyk(testContext)
        tab = createTab("https://mozilla.org")
        browserStore = BrowserStore(BrowserState(tabs = listOf(tab)))
        controller = spyk(
            DefaultCookieBannerDetailsController(
                fragment = fragment,
                context = context,
                ioScope = scope,
                cookieBannersStorage = cookieBannersStorage,
                navController = { navController },
                sitePermissions = sitePermissions,
                gravity = gravity,
                getCurrentTab = { tab },
                sessionId = tab.id,
                browserStore = browserStore,
                protectionsStore = protectionsStore,
                engine = engine,
                publicSuffixList = publicSuffixList,
                reload = reload,
            ),
        )

        every { fragment.context } returns context
        every { context.components.useCases.trackingProtectionUseCases } returns trackingProtectionUseCases

        val onComplete = slot<(Boolean) -> Unit>()
        every {
            trackingProtectionUseCases.containsException.invoke(
                any(),
                capture(onComplete),
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

    @Test
    fun `GIVEN cookie banner is enabled WHEN handleTogglePressed THEN remove from the storage, send telemetry and reload the tab`() =
        runTestOnMain {
            val isEnabled = true

            assertNull(CookieBanners.exceptionRemoved.testGetValue())
            every { protectionsStore.dispatch(any()) } returns mockk()

            controller.handleTogglePressed(isEnabled)

            advanceUntilIdle()

            coVerifyOrder {
                cookieBannersStorage.removeException(
                    uri = tab.content.url,
                    privateBrowsing = tab.content.private,
                )
                protectionsStore.dispatch(
                    ProtectionsAction.ToggleCookieBannerHandlingProtectionEnabled(
                        isEnabled,
                    ),
                )
                reload(tab.id)
            }

            assertNotNull(CookieBanners.exceptionRemoved.testGetValue())
        }

    @Test
    fun `GIVEN cookie banner is disabled WHEN handleTogglePressed THEN remove from the storage, send telemetry and reload the tab`() =
        runTestOnMain {
            val isEnabled = false

            assertNull(CookieBanners.exceptionRemoved.testGetValue())
            every { protectionsStore.dispatch(any()) } returns mockk()
            coEvery { controller.clearSiteData(any()) } just Runs

            controller.handleTogglePressed(isEnabled)

            advanceUntilIdle()

            coVerifyOrder {
                controller.clearSiteData(tab)
                cookieBannersStorage.addException(
                    uri = tab.content.url,
                    privateBrowsing = tab.content.private,
                )
                protectionsStore.dispatch(
                    ProtectionsAction.ToggleCookieBannerHandlingProtectionEnabled(
                        isEnabled,
                    ),
                )
                reload(tab.id)
            }

            assertNotNull(CookieBanners.exceptionAdded.testGetValue())
        }

    @Test
    fun `WHEN clearSiteData THEN delegate the call to the engine`() =
        runTestOnMain {
            coEvery { publicSuffixList.getPublicSuffixPlusOne(any()) } returns CompletableDeferred("mozilla.org")

            controller.clearSiteData(tab)

            coVerifyOrder {
                engine.clearData(
                    host = "mozilla.org",
                    data = Engine.BrowsingData.select(
                        Engine.BrowsingData.AUTH_SESSIONS,
                        Engine.BrowsingData.ALL_SITE_DATA,
                    ),
                )
            }
        }
}
