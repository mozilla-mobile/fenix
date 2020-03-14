package org.mozilla.fenix.components.toolbar

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.storage.BookmarksStorage
import mozilla.components.feature.app.links.AppLinkRedirect
import mozilla.components.feature.app.links.AppLinksUseCases
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.Settings

class DefaultToolbarMenuTest {

    private lateinit var defaultToolbarMenu: DefaultToolbarMenu

    private val context: Context = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private val onItemTapped: (ToolbarMenu.Item) -> Unit = {}
    private val lifecycleOwner: LifecycleOwner = mockk(relaxed = true)
    private val bookmarksStorage: BookmarksStorage = mockk(relaxed = true)

    @Before
    fun setUp() {
        defaultToolbarMenu = spyk(
            DefaultToolbarMenu(
                context,
                sessionManager,
                hasAccountProblem = false,
                shouldReverseItems = false,
                onItemTapped = onItemTapped,
                lifecycleOwner = lifecycleOwner,
                bookmarksStorage = bookmarksStorage
            )
        )

        val settings = Settings.getInstance(context, true)
        mockkStatic("org.mozilla.fenix.ext.ContextKt")
        every { context.settings() } returns settings
    }

    @Test
    fun `get all low prio highlight items`() {
        every { context.components.useCases.webAppUseCases.isPinningSupported() } returns true
        every { context.components.useCases.webAppUseCases.isInstallable() } returns true

        val getAppLinkRedirect: AppLinksUseCases.GetAppLinkRedirect = mockk(relaxed = true)
        every { context.components.useCases.appLinksUseCases.appLinkRedirect } returns getAppLinkRedirect
        val appLinkRedirect: AppLinkRedirect = mockk(relaxed = true)
        every { appLinkRedirect.hasExternalApp() } returns true
        every { getAppLinkRedirect(any()) } returns appLinkRedirect

        val session: Session = mockk(relaxed = true)
        every { session.readerable } returns true
        every { sessionManager.selectedSession } returns session

        val list = defaultToolbarMenu.getLowPrioHighlightItems()

        assertEquals(ToolbarMenu.Item.AddToHomeScreen, list[0])
        assertEquals(ToolbarMenu.Item.ReaderMode(false), list[1])
        assertEquals(ToolbarMenu.Item.OpenInApp, list[2])
    }

    @Test
    fun `get all low prio highlight items without AddToHomeScreen`() {
        val settings = Settings.getInstance(context, true)
        mockkStatic("org.mozilla.fenix.ext.ContextKt")
        every { context.settings() } returns settings

        every { context.components.useCases.webAppUseCases.isPinningSupported() } returns false

        val getAppLinkRedirect: AppLinksUseCases.GetAppLinkRedirect = mockk(relaxed = true)
        every { context.components.useCases.appLinksUseCases.appLinkRedirect } returns getAppLinkRedirect
        val appLinkRedirect: AppLinkRedirect = mockk(relaxed = true)
        every { appLinkRedirect.hasExternalApp() } returns true
        every { getAppLinkRedirect(any()) } returns appLinkRedirect

        val session: Session = mockk(relaxed = true)
        every { session.readerable } returns true
        every { sessionManager.selectedSession } returns session

        val list = defaultToolbarMenu.getLowPrioHighlightItems()

        assertEquals(ToolbarMenu.Item.ReaderMode(false), list[0])
        assertEquals(ToolbarMenu.Item.OpenInApp, list[1])
    }

    @Test
    fun `get all low prio highlight items without ReaderMode`() {
        every { context.components.useCases.webAppUseCases.isPinningSupported() } returns true
        every { context.components.useCases.webAppUseCases.isInstallable() } returns true

        val getAppLinkRedirect: AppLinksUseCases.GetAppLinkRedirect = mockk(relaxed = true)
        every { context.components.useCases.appLinksUseCases.appLinkRedirect } returns getAppLinkRedirect
        every { getAppLinkRedirect(any()).hasExternalApp() } returns true

        val list = defaultToolbarMenu.getLowPrioHighlightItems()

        assertEquals(ToolbarMenu.Item.AddToHomeScreen, list[0])
        assertEquals(ToolbarMenu.Item.OpenInApp, list[1])
    }

    @Test
    fun `get all low prio highlight items without OpenInApp`() {
        every { context.components.useCases.webAppUseCases.isPinningSupported() } returns true
        every { context.components.useCases.webAppUseCases.isInstallable() } returns true

        val session: Session = mockk(relaxed = true)
        every { session.readerable } returns true
        every { sessionManager.selectedSession } returns session

        val getAppLinkRedirect: AppLinksUseCases.GetAppLinkRedirect = mockk(relaxed = true)
        every { context.components.useCases.appLinksUseCases.appLinkRedirect } returns getAppLinkRedirect
        every { getAppLinkRedirect(any()).hasExternalApp() } returns false

        val list = defaultToolbarMenu.getLowPrioHighlightItems()

        assertEquals(ToolbarMenu.Item.AddToHomeScreen, list[0])
        assertEquals(ToolbarMenu.Item.ReaderMode(false), list[1])
    }
}
