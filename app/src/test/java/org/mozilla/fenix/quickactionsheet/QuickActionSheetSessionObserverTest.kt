/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.quickactionsheet

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mozilla.components.browser.session.Session
import mozilla.components.feature.app.links.AppLinkRedirect
import mozilla.components.feature.app.links.AppLinksUseCases
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.toolbar.BrowserFragmentStore
import org.mozilla.fenix.components.toolbar.QuickActionSheetAction

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class QuickActionSheetSessionObserverTest {

    private val components: Components = mockk(relaxed = true)
    private val appLinkRedirect: AppLinksUseCases.GetAppLinkRedirect = mockk(relaxed = true)
    private val store: BrowserFragmentStore = mockk(relaxed = true)
    private val dispatch: (QuickActionSheetAction) -> Unit = { store.dispatch(it) }

    private val session: Session = mockk()
    private val observer = spyk(QuickActionSheetSessionObserver(mockk(), components, dispatch))

    @Before
    fun setup() {
        every { components.useCases.appLinksUseCases.appLinkRedirect } returns appLinkRedirect
    }

    @Test
    fun `onLoadingStateChanged dispatches BounceNeededChange and updates bookmark button`() {
        every { observer.updateBookmarkState(session) } just Runs

        observer.onLoadingStateChanged(session, true)
        verify(exactly = 0) { store.dispatch(QuickActionSheetAction.BounceNeededChange) }

        observer.onLoadingStateChanged(session, false)
        verify { observer.updateBookmarkState(session) }
        verify { store.dispatch(QuickActionSheetAction.BounceNeededChange) }
    }

    @Test
    fun `onUrlChanged updates bookmark and app link buttons`() {
        val url = "https://example.com"

        every { session.url } returns url

        every { observer.updateBookmarkState(session) } just Runs
        every { appLinkRedirect.invoke(url) } returns AppLinkRedirect(mockk(), "", false)

        observer.onUrlChanged(session, "")
        verify { observer.updateBookmarkState(session) }
        verify { appLinkRedirect.invoke("https://example.com") }
        verify { store.dispatch(QuickActionSheetAction.AppLinkStateChange(true)) }
    }
}
