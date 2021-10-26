/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.core.view.children
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import mozilla.components.browser.toolbar.BrowserToolbar
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.utils.view.ViewHolder

@RunWith(AndroidJUnit4::class)
class HomeFragmentBenchmark {

    private lateinit var mockWebServer: MockWebServer

    private lateinit var genericURL: TestAssetHelper.TestAsset

    private lateinit var intent: Intent

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val homeActivityRule = HomeActivityTestRule(
        skipOnboarding = true, skipShouldShowJumpBackIn = true
    )

    @get: Rule
    val intentReceiverActivityTestRule = ActivityTestRule(
        IntentReceiverActivity::class.java, true, false
    )

    @Before
    fun setup() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }

        genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        intent = Intent(Intent.ACTION_VIEW, genericURL.url)
        intentReceiverActivityTestRule.launchActivity(intent)
    }

    /**
     * Benchmarks the time taken from clicking the "Home Button" on the URL bar located on the
     * BrowserFragment up until the HomeFragment is shown and usable.
     *
     * NOTE: According to Google's documentations, this test should be annotated with @UiThreadTest.
     * However, doing so will make UiAutomator or Espresso API's unusable since the test will hang.
     * Instead, we use the `runOnUiThread{}` which is the method that @UiThreadTest uses.
     */
    @Test
    fun homeScreenBenchmark() {
        var homeButton: View? = null

        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                homeButton = getHomeButton(homeActivityRule.activity.findViewById<BrowserToolbar>(R.id.toolbar))
            }
            homeActivityRule.runOnUiThread {
                homeButton!!.callOnClick()
            }

            // We have to wait for the Sync otherwise we'll execute the test too quickly
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            runWithTimingDisabled {
                homeButton = null
                onView(withId(R.id.sessionControlRecyclerView)).perform(
                    RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(2, click())
                )
            }
        }
    }

    private fun getHomeButton(view: View): View? {
        if (view is ViewGroup) {
            return view.children.map(::getHomeButton).firstOrNull()
        }

        return if (view.contentDescription == "Home screen") view else null
    }
}
