/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.navOptions
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class StartSearchIntentProcessorTest {

    private val metrics: MetricController = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val out: Intent = mockk(relaxed = true)

    @Test
    fun `do not process blank intents`() {
        StartSearchIntentProcessor(metrics).process(Intent(), navController, out)

        verify { metrics wasNot Called }
        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `do not process when search extra is false`() {
        val intent = Intent().apply {
            removeExtra(HomeActivity.OPEN_TO_SEARCH)
        }
        StartSearchIntentProcessor(metrics).process(intent, navController, out)

        verify { metrics wasNot Called }
        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `process search intents`() {
        val intent = Intent().apply {
            putExtra(HomeActivity.OPEN_TO_SEARCH, StartSearchIntentProcessor.SEARCH_WIDGET)
        }
        StartSearchIntentProcessor(metrics).process(intent, navController, out)
        val options = navOptions {
            popUpTo = R.id.homeFragment
        }

        verify { metrics.track(Event.SearchWidgetNewTabPressed) }
        verify {
            navController.nav(
                null,
                NavGraphDirections.actionGlobalSearchDialog(
                    sessionId = null,
                    searchAccessPoint = Event.PerformedSearch.SearchAccessPoint.WIDGET
                ),
                options
            )
        }
        verify { out.removeExtra(HomeActivity.OPEN_TO_SEARCH) }
    }

    @Test
    fun `process assist intent`() {
        val intent = Intent().apply { action=Intent.ACTION_ASSIST }
        StartSearchIntentProcessor(metrics).process(intent, navController, out)
        val options = navOptions {
            popUpTo = R.id.homeFragment
        }

        verify {
            navController.nav(
                    null,
                    NavGraphDirections.actionGlobalSearchDialog(
                            sessionId = null,
                            searchAccessPoint = Event.PerformedSearch.SearchAccessPoint.ASSIST
                    ),
                    options
            )
        }

        verify { out wasNot Called }
        verify { metrics wasNot Called }
    }
}
