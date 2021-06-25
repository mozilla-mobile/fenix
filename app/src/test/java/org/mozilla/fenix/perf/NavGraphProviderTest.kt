/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import androidx.navigation.NavController
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi // runBlockingTest
class NavGraphProviderTest {

    // After https://github.com/mozilla-mobile/android-components/issues/10604 lands, use
    // coroutineTestRule.testDispatcher instead of passing it in.
    val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val coroutineTestRule = MainCoroutineRule(testDispatcher)

    @RelaxedMockK private lateinit var navController: NavController

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `WHEN we inflate the nav graph asynchronously THEN then the nav graph is eventually set asynchronously`() = testDispatcher.runBlockingTest {
        // By pausing the dispatcher, we're pausing the execution of async code in this block. If
        // inflation hasn't happened by the end of it, it's not synchronous.
        pauseDispatcher {
            NavGraphProvider.inflateNavGraphAsync(navController, this, testDispatcher)
            verifyInflateAndSetNavGraph(exactlyCount = 0)
        }

        // With the dispatcher resumed, all async code executes synchronously so we should have
        // inflate and set by now.
        verifyInflateAndSetNavGraph(exactlyCount = 1)
    }

    // This test case represents the standard usage pattern. Unfortunately, I don't know how to
    // force the coroutines to run in different orders to actually test that blocking works so I
    // only test that we don't set the nav graph redundantly.
    @Test
    fun `WHEN we inflate the nav graph async and block for inflation THEN we inflate and set the nav graph once`() = testDispatcher.runBlockingTest {
        NavGraphProvider.inflateNavGraphAsync(navController, this, testDispatcher)
        verifyInflateAndSetNavGraph(exactlyCount = 1)

        NavGraphProvider.blockForNavGraphInflation(navController)
        verifyInflateAndSetNavGraph(exactlyCount = 1)
    }

    // I don't believe this should ever happen in production but the case is incidently handled
    // by other code so we'll ensure we do the right thing in this case.
    @Test
    fun `WHEN we inflate the nav graph async twice THEN we only inflate and set the nav graph once`() = testDispatcher.runBlockingTest {
        NavGraphProvider.inflateNavGraphAsync(navController, this, testDispatcher)
        verifyInflateAndSetNavGraph(exactlyCount = 1)

        NavGraphProvider.inflateNavGraphAsync(navController, this, testDispatcher)
        verifyInflateAndSetNavGraph(exactlyCount = 1)
    }

    // This test case occurs in production when we:
    // - Long press the tab tray icon
    // - Create a new tab in opposite browsing mode (e.g. if we're in normal, we create one in private)
    //
    // This test case will happen then because the Activity is recreated during these steps but the
    // system uses a cached version of it that recreates parts of the fragment before
    // `HomeActivity.onCreate`, where we call inflate async, is called.
    @Test
    fun `GIVEN we haven't inflate the nav graph async WHEN we block for nav graph inflation THEN we inflate and set the nav graph synchronously`() = testDispatcher.runBlockingTest {
        // Only synchronous code will run with a paused dispatcher so we can make
        // sure the inflation happens synchronously.
        pauseDispatcher()

        NavGraphProvider.blockForNavGraphInflation(navController)
        verifyInflateAndSetNavGraph(exactlyCount = 1)
    }

    // This test case occurs in production when we:
    // - Long press the tab tray icon
    // - Create a new tab in opposite browsing mode (e.g. if we're in normal, we create one in private)
    //
    // This test case will happen then because the Activity is recreated during these steps but the
    // system uses a cached version of it that recreates parts of the fragment before
    // `HomeActivity.onCreate`, where we call inflate async, is called.
    @Test
    fun `WHEN we block for inflation and then inflate async THEN we inflate and set the nav graph once`() = testDispatcher.runBlockingTest {
        NavGraphProvider.blockForNavGraphInflation(navController)
        verifyInflateAndSetNavGraph(exactlyCount = 1)

        NavGraphProvider.inflateNavGraphAsync(navController, this, testDispatcher)
        verifyInflateAndSetNavGraph(exactlyCount = 1)
    }

    // This test case occurs in production when we:
    // - Long press the tabs tray button and open a private tab
    // - Hit the back button
    // - Click the toolbar
    //
    // This also occurs anytime we inflate synchronously when blockForNavGraph is called first
    // and a later part of the code needs to navigate so it also calls blockForNavGraph.
    @Test
    fun `GIVEN we haven't inflate the nav graph async WHEN we block for nav graph inflation twice THEN we inflate and set the nav graph once`() = testDispatcher.runBlockingTest {
        NavGraphProvider.blockForNavGraphInflation(navController)
        verifyInflateAndSetNavGraph(exactlyCount = 1)

        NavGraphProvider.blockForNavGraphInflation(navController)
        verifyInflateAndSetNavGraph(exactlyCount = 1)
    }

    private fun verifyInflateAndSetNavGraph(exactlyCount: Int) {
        verify(exactly = exactlyCount) { navController.navInflater.inflate(any()) }
        verify(exactly = exactlyCount) { navController.graph = any() }
    }
}
