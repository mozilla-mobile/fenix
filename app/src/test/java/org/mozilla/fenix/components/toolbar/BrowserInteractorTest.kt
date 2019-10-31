package org.mozilla.fenix.components.toolbar

import android.content.Context
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.session.Session
import org.junit.Before
import org.junit.Test

import org.mozilla.fenix.browser.readermode.ReaderModeController
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.metrics

class BrowserInteractorTest {

    lateinit var metrics: MetricController
    lateinit var context: Context
    lateinit var browserStore: BrowserFragmentStore
    lateinit var browserToolbarController: BrowserToolbarController
    lateinit var readerModeController: ReaderModeController
    lateinit var session: Session
    lateinit var interactor: BrowserInteractor

    @Before
    fun setup() {
        metrics = mockk()
        context = mockk()
        browserStore = mockk(relaxed = true)
        browserToolbarController = mockk(relaxed = true)
        readerModeController = mockk(relaxed = true)
        session = mockk()
        interactor = BrowserInteractor(
            context,
            browserStore,
            browserToolbarController,
            readerModeController,
            session
        )
        every { context.metrics } returns metrics
        every { context.components.core.sessionManager.selectedSession } returns session
    }

    @Test
    fun onTabCounterClicked() {
        interactor.onTabCounterClicked()
        verify { browserToolbarController.handleTabCounterClick() }
    }

    @Test
    fun onBrowserToolbarPaste() {
        val pastedText = "Mozilla"
        interactor.onBrowserToolbarPaste(pastedText)
        verify { browserToolbarController.handleToolbarPaste(pastedText) }
    }

    @Test
    fun onBrowserToolbarPasteAndGo() {
        val pastedText = "Mozilla"

        interactor.onBrowserToolbarPasteAndGo(pastedText)
        verify { browserToolbarController.handleToolbarPasteAndGo(pastedText) }
    }

    @Test
    fun onBrowserToolbarClicked() {
        interactor.onBrowserToolbarClicked()

        verify { browserToolbarController.handleToolbarClick() }
    }

    @Test
    fun onBrowserToolbarMenuItemTapped() {
        val item: ToolbarMenu.Item = mockk()

        interactor.onBrowserToolbarMenuItemTapped(item)

        verify { browserToolbarController.handleToolbarItemInteraction(item) }
    }
}
