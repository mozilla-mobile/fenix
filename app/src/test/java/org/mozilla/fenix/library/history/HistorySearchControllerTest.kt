/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.test.robolectric.testContext
import mozilla.telemetry.glean.testing.GleanTestRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.History
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class HistorySearchControllerTest {

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    @MockK(relaxed = true) private lateinit var activity: HomeActivity
    @MockK(relaxed = true) private lateinit var store: HistorySearchFragmentStore

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `WHEN editing is cancelled THEN clearToolbarFocus is called`() = runTest {
        var clearToolbarFocusInvoked = false
        createController(
            clearToolbarFocus = {
                clearToolbarFocusInvoked = true
            }
        ).handleEditingCancelled()

        assertTrue(clearToolbarFocusInvoked)
    }

    @Test
    fun `WHEN text changed THEN update query action is dispatched`() {
        val text = "fenix"

        createController().handleTextChanged(text)

        verify { store.dispatch(HistorySearchFragmentAction.UpdateQuery(text)) }
    }

    @Test
    fun `WHEN text is changed to empty THEN update query action is dispatched`() {
        val text = ""

        createController().handleTextChanged(text)

        verify { store.dispatch(HistorySearchFragmentAction.UpdateQuery(text)) }
    }

    @Test
    fun `WHEN url is tapped THEN openToBrowserAndLoad is called`() {
        val url = "https://www.google.com/"
        val flags = EngineSession.LoadUrlFlags.none()
        assertFalse(History.searchResultTapped.testHasValue())

        createController().handleUrlTapped(url, flags)
        createController().handleUrlTapped(url)

        assertTrue(History.searchResultTapped.testHasValue())
        assertNull(History.searchResultTapped.testGetValue().last().extra)
        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = true,
                from = BrowserDirection.FromHistorySearchDialog,
                flags = flags
            )
        }
    }

    private fun createController(
        clearToolbarFocus: () -> Unit = { },
    ): HistorySearchDialogController {
        return HistorySearchDialogController(
            activity = activity,
            fragmentStore = store,
            clearToolbarFocus = clearToolbarFocus,
        )
    }
}
