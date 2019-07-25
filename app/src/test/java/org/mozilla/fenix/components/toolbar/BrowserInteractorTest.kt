package org.mozilla.fenix.components.toolbar

import android.content.Context
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.app.links.AppLinkRedirect
import mozilla.components.feature.app.links.AppLinksUseCases
import org.junit.Test

import org.junit.Assert.*
import org.mozilla.fenix.browser.readermode.ReaderModeController
import org.mozilla.fenix.components.Analytics
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.Core
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.metrics

class BrowserInteractorTest {

    @Test
    fun onBrowserToolbarClicked() {
        val context: Context = mockk()
        val controller: BrowserToolbarController = mockk(relaxed = true)

        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            controller,
            mockk(),
            mockk(),
            mockk(),
            mockk()
        )

        interactor.onBrowserToolbarClicked()

        verify { controller.handleToolbarClick() }
    }

    @Test
    fun onBrowserToolbarMenuItemTapped() {
        val context: Context = mockk()
        val controller: BrowserToolbarController = mockk(relaxed = true)
        val item: ToolbarMenu.Item = mockk()

        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            controller,
            mockk(),
            mockk(),
            mockk(),
            mockk()
        )

        interactor.onBrowserToolbarMenuItemTapped(item)

        verify { controller.handleToolbarItemInteraction(item) }
    }

    @Test
    fun onQuickActionSheetOpened() {
        val context: Context = mockk()
        val metrics: MetricController = mockk()
        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk()
        )

        every { context.metrics } returns metrics
        every { metrics.track(Event.QuickActionSheetOpened) } just Runs

        interactor.onQuickActionSheetOpened()

        verify { metrics.track(Event.QuickActionSheetOpened) }
    }

    @Test
    fun onQuickActionSheetClosed() {
        val context: Context = mockk()
        val metrics: MetricController = mockk()
        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk()
        )

        every { context.metrics } returns metrics
        every { metrics.track(Event.QuickActionSheetClosed) } just Runs

        interactor.onQuickActionSheetClosed()

        verify { metrics.track(Event.QuickActionSheetClosed) }
    }

    @Test
    fun onQuickActionSheetSharePressed() {
        // TODO: I got rid of the shareUrl function passed in so I can't easily test this...?
        val context: Context = mockk()
        val session: Session = mockk()
        var selectedSessionUrl = ""
        val metrics: MetricController = mockk()

        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            session
        )

        val components: Components = mockk()
        val core: Core = mockk()
        val sessionManager: SessionManager = mockk()

        val analytics: Analytics = mockk()

        every { session.url } returns "mozilla.org"
        every { context.components } returns components
        every { components.analytics } returns analytics
        every { metrics.track(Event.QuickActionSheetShareTapped) } just Runs
        // Since we are mocking components, we must manually define metrics as `analytics.metrics`
        every { analytics.metrics } returns metrics
        every { components.core } returns core
        every { core.sessionManager } returns sessionManager
        every { sessionManager.selectedSession } returns session

        interactor.onQuickActionSheetSharePressed()

        verify { metrics.track(Event.QuickActionSheetShareTapped) }
        assertEquals("mozilla.org", selectedSessionUrl)
    }

    @Test
    fun onQuickActionSheetDownloadPressed() {
        val context: Context = mockk()
        val metrics: MetricController = mockk()
        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk()
        )

        every { context.metrics } returns metrics
        every { metrics.track(Event.QuickActionSheetDownloadTapped) } just Runs

        interactor.onQuickActionSheetDownloadPressed()

        verify { metrics.track(Event.QuickActionSheetDownloadTapped) }
    }

    @Test
    fun onQuickActionSheetBookmarkPressed() {
        val context: Context = mockk()
        val session: Session = mockk()
        var bookmarkedSession: Session? = null

        val metrics: MetricController = mockk()
        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            { bookmarkedSession = it },
            mockk(),
            session
        )

        val components: Components = mockk()
        val core: Core = mockk()
        val sessionManager: SessionManager = mockk()

        val analytics: Analytics = mockk()

        every { session.url } returns "mozilla.org"
        every { context.components } returns components
        every { components.analytics } returns analytics
        every { metrics.track(Event.QuickActionSheetBookmarkTapped) } just Runs
        // Since we are mocking components, we must manually define metrics as `analytics.metrics`
        every { analytics.metrics } returns metrics
        every { components.core } returns core
        every { core.sessionManager } returns sessionManager
        every { sessionManager.selectedSession } returns session

        interactor.onQuickActionSheetBookmarkPressed()

        verify { metrics.track(Event.QuickActionSheetBookmarkTapped) }
        assertEquals("mozilla.org", bookmarkedSession?.url)
    }

    @Test
    fun onQuickActionSheetReadPressed() {
        val context: Context = mockk()
        val metrics: MetricController = mockk()
        val session: Session = mockk()
        val readerModeController: ReaderModeController = mockk(relaxed = true)
        val browserStore: BrowserStore = mockk(relaxed = true)
        val interactor = BrowserInteractor(
            context,
            mockk(),
            browserStore,
            mockk(),
            readerModeController,
            mockk(),
            mockk(),
            session
        )

        every { context.metrics } returns metrics
        every { context.components.core.sessionManager.selectedSession } returns session
        every { session.readerMode } returns false
        every { metrics.track(Event.QuickActionSheetReadTapped) } just Runs

        interactor.onQuickActionSheetReadPressed()

        verify { metrics.track(Event.QuickActionSheetReadTapped) }
        verify { readerModeController.showReaderView() }
    }

    @Test
    fun onQuickActionSheetReadPressedWithActiveReaderMode() {
        val context: Context = mockk()
        val metrics: MetricController = mockk()
        val session: Session = mockk()
        val readerModeController: ReaderModeController = mockk(relaxed = true)
        val browserStore: BrowserStore = mockk(relaxed = true)

        val interactor = BrowserInteractor(
            context,
            mockk(),
            browserStore,
            mockk(),
            readerModeController,
            mockk(),
            mockk(),
            session
        )

        every { context.metrics } returns metrics
        every { context.components.core.sessionManager.selectedSession } returns session
        every { session.readerMode } returns true
        every { metrics.track(Event.QuickActionSheetReadTapped) } just Runs

        interactor.onQuickActionSheetReadPressed()

        verify { metrics.track(Event.QuickActionSheetReadTapped) }
        verify { readerModeController.hideReaderView() }
    }

    @Test
    fun onQuickActionSheetOpenLinkPressed() {
        val context: Context = mockk()
        val session: Session = mockk()
        val appLinksUseCases: AppLinksUseCases = mockk()

        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            appLinksUseCases,
            session
        )

        every { context.components.core.sessionManager.selectedSession } returns session
        every { session.url } returns "mozilla.org"

        val getAppLinkRedirect: AppLinksUseCases.GetAppLinkRedirect = mockk()
        val appLinkRedirect: AppLinkRedirect = mockk()
        val openAppLink: AppLinksUseCases.OpenAppLinkRedirect = mockk(relaxed = true)

        every { appLinksUseCases.appLinkRedirect } returns getAppLinkRedirect
        every { getAppLinkRedirect.invoke("mozilla.org") } returns appLinkRedirect
        every { appLinksUseCases.openAppLink } returns openAppLink
        every { appLinkRedirect.appIntent } returns mockk(relaxed = true)

        interactor.onQuickActionSheetOpenLinkPressed()

        verify { openAppLink.invoke(appLinkRedirect) }
    }

    @Test
    fun onQuickActionSheetAppearancePressed() {
        val context: Context = mockk()
        val readerModeController: ReaderModeController = mockk(relaxed = true)

        val interactor = BrowserInteractor(
            context,
            mockk(),
            mockk(),
            mockk(),
            readerModeController,
            mockk(),
            mockk(),
            mockk()
        )

        interactor.onQuickActionSheetAppearancePressed()

        verify { readerModeController.showControls() }
    }
}