/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.Runs
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.sitepermissions.SitePermissions
import mozilla.components.feature.sitepermissions.SitePermissions.Status.NO_DECISION
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.PermissionStorage
import org.mozilla.fenix.ext.directionsEq
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.quicksettings.ext.shouldBeEnabled
import org.mozilla.fenix.settings.toggle
import org.mozilla.fenix.utils.Settings

@ExperimentalCoroutinesApi
@RunWith(FenixRobolectricTestRunner::class)
class DefaultQuickSettingsControllerTest {
    private val context = testContext

    private lateinit var browserStore: BrowserStore
    private lateinit var tab: TabSessionState

    @MockK
    private lateinit var store: QuickSettingsFragmentStore
    private val coroutinesScope = TestCoroutineScope()

    @MockK(relaxed = true)
    private lateinit var navController: NavController

    @MockK(relaxed = true)
    private lateinit var sitePermissions: SitePermissions

    @MockK(relaxed = true)
    private lateinit var appSettings: Settings

    @MockK(relaxed = true)
    private lateinit var permissionStorage: PermissionStorage

    @MockK(relaxed = true)
    private lateinit var reload: SessionUseCases.ReloadUrlUseCase

    @MockK(relaxed = true)
    private lateinit var addNewTab: TabsUseCases.AddNewTabUseCase

    @MockK(relaxed = true)
    private lateinit var requestPermissions: (Array<String>) -> Unit

    @MockK(relaxed = true)
    private lateinit var displayPermissions: () -> Unit

    @MockK(relaxed = true)
    private lateinit var dismiss: () -> Unit

    private lateinit var controller: DefaultQuickSettingsController

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        tab = createTab("https://mozilla.org")
        browserStore = BrowserStore(BrowserState(tabs = listOf(tab)))
        sitePermissions = SitePermissions(origin = "", savedAt = 123)

        controller = spyk(
            DefaultQuickSettingsController(
                context = context,
                quickSettingsStore = store,
                browserStore = browserStore,
                sessionId = tab.id,
                ioScope = coroutinesScope,
                navController = navController,
                sitePermissions = sitePermissions,
                settings = appSettings,
                permissionStorage = permissionStorage,
                reload = reload,
                addNewTab = addNewTab,
                requestRuntimePermissions = requestPermissions,
                displayPermissions = displayPermissions,
                dismiss = dismiss
            )
        )
    }

    @After
    fun cleanUp() {
        coroutinesScope.cleanupTestCoroutines()
    }

    @Test
    fun `handlePermissionsShown should delegate to an injected parameter`() {
        controller.handlePermissionsShown()

        verify {
            displayPermissions()
        }
    }

    @Test
    fun `handlePermissionToggled blocked by Android should handleAndroidPermissionRequest`() {
        val cameraFeature = PhoneFeature.CAMERA
        val websitePermission = mockk<WebsitePermission>()
        every { websitePermission.phoneFeature } returns cameraFeature
        every { websitePermission.isBlockedByAndroid } returns true

        controller.handlePermissionToggled(websitePermission)

        verify {
            controller.handleAndroidPermissionRequest(cameraFeature.androidPermissionsList)
        }
    }

    @Test
    fun `handlePermissionToggled allowed by Android should toggle the permissions and modify View's state`() {
        val websitePermission = mockk<WebsitePermission>()
        every { websitePermission.phoneFeature } returns PhoneFeature.CAMERA
        every { websitePermission.isBlockedByAndroid } returns false
        every { store.dispatch(any()) } returns mockk()

        controller.handlePermissionToggled(websitePermission)

        // We want to verify that the Status is toggled and this event is passed to Controller also.
        assertSame(NO_DECISION, sitePermissions.camera)
        verify {
            controller.handlePermissionsChange(sitePermissions.toggle(PhoneFeature.CAMERA))
        }
        // We should also modify View's state. Not necessarily as the last operation.
        verify {
            store.dispatch(match { action ->
                PhoneFeature.CAMERA == (action as WebsitePermissionAction.TogglePermission).updatedFeature
            })
        }
    }

    @Test
    fun `handlePermissionToggled blocked by user should navigate to site permission manager`() {
        val websitePermission = mockk<WebsitePermission>()
        val invalidSitePermissionsController = DefaultQuickSettingsController(
            context = context,
            quickSettingsStore = store,
            browserStore = BrowserStore(),
            ioScope = coroutinesScope,
            navController = navController,
            sessionId = "123",
            sitePermissions = null,
            settings = appSettings,
            permissionStorage = permissionStorage,
            reload = reload,
            addNewTab = addNewTab,
            requestRuntimePermissions = requestPermissions,
            displayPermissions = displayPermissions,
            dismiss = dismiss
        )

        every { websitePermission.phoneFeature } returns PhoneFeature.CAMERA
        every { websitePermission.isBlockedByAndroid } returns false
        every { navController.navigate(any<NavDirections>()) } just Runs

        invalidSitePermissionsController.handlePermissionToggled(websitePermission)

        verify {
            navController.navigate(directionsEq(
                QuickSettingsSheetDialogFragmentDirections.actionGlobalSitePermissionsManagePhoneFeature(PhoneFeature.CAMERA)
            ))
        }
    }

    @Test
    fun `handleAutoplayChanged will add autoplay permission`() {
        val autoplayValue = mockk<AutoplayValue.AllowAll>(relaxed = true)

        every { store.dispatch(any()) } returns mockk()
        every { controller.handleAutoplayAdd(any()) } returns Unit

        controller.sitePermissions = null

        controller.handleAutoplayChanged(autoplayValue)

        verify {
            controller.handleAutoplayAdd(any())
            store.dispatch(any())
        }
    }

    @Test
    fun `handleAutoplayChanged will update autoplay permission`() {
        val autoplayValue = mockk<AutoplayValue.AllowAll>(relaxed = true)

        every { store.dispatch(any()) } returns mockk()
        every { controller.handleAutoplayAdd(any()) } returns Unit
        every { controller.handlePermissionsChange(any()) } returns Unit
        every { autoplayValue.updateSitePermissions(any()) } returns mock()

        controller.handleAutoplayChanged(autoplayValue)

        verify {
            autoplayValue.updateSitePermissions(any())
            store.dispatch(any())
        }
    }
    @Test
    fun `handleAndroidPermissionGranted should update the View's state`() {
        val featureGranted = PhoneFeature.CAMERA
        val permissionStatus = featureGranted.getActionLabel(context, sitePermissions, appSettings)
        val permissionEnabled = featureGranted.shouldBeEnabled(context, sitePermissions, appSettings)
        every { store.dispatch(any()) } returns mockk()

        controller.handleAndroidPermissionGranted(featureGranted)

        verify {
            store.dispatch(withArg { action ->
                action as WebsitePermissionAction.TogglePermission
                assertEquals(featureGranted, action.updatedFeature)
                assertEquals(permissionStatus, action.updatedStatus)
                assertEquals(permissionEnabled, action.updatedEnabledStatus)
            })
        }
    }

    @Test
    fun `handleAndroidPermissionRequest should request from the injected callback`() {
        val testPermissions = arrayOf("TestPermission")

        controller.handleAndroidPermissionRequest(testPermissions)

        verify { requestPermissions(testPermissions) }
    }

    @Test
    fun `handlePermissionsChange should store the updated permission and reload webpage`() = coroutinesScope.runBlockingTest {
        val testPermissions = mockk<SitePermissions>()

        controller.handlePermissionsChange(testPermissions)
        advanceUntilIdle()

        coVerifyOrder {
            permissionStorage.updateSitePermissions(testPermissions)
            reload(tab.id)
        }
    }

    @Test
    fun `handleAutoplayAdd should store the updated permission and reload webpage`() = coroutinesScope.runBlockingTest {
        val testPermissions = mockk<SitePermissions>()

        controller.handleAutoplayAdd(testPermissions)
        advanceUntilIdle()

        coVerifyOrder {
            permissionStorage.add(testPermissions)
            reload(tab.id)
        }
    }
}
