/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.children
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import mozilla.components.browser.toolbar.BrowserToolbar
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.BrowserToolbarView
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.*
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.ui.robots.BrowserRobot
import org.mozilla.fenix.ui.robots.HomeScreenRobot
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

@RunWith(AndroidJUnit4::class)
class HomeFragmentBenchmark {

    private lateinit var mockWebServer: MockWebServer

    private lateinit var genericURL : TestAssetHelper.TestAsset

    private lateinit var intent: Intent

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val homeActivityRule = HomeActivityTestRule(
        skipOnboarding = true, skipShouldShowJumpBackIn = false
    )

    @get: Rule
    val intentReceiverActivityTestRule  = ActivityTestRule(
        IntentReceiverActivity::class.java, true, false
    )


    @Before
    fun setup() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher();
            start()
        }

        genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        intent = Intent(Intent.ACTION_VIEW, genericURL.url)
        intentReceiverActivityTestRule.launchActivity(intent)

    }


    @Test
    fun homeScreenBenchmark() {
        val toolbar = homeActivityRule.activity.findViewById<BrowserToolbar>(R.id.toolbar)
        var homeButton = getHomeButton(toolbar)

        benchmarkRule.measureRepeated {
            homeActivityRule.runOnUiThread {
                homeButton!!.callOnClick()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            runWithTimingDisabled {
                val recyclerView = homeActivityRule.activity.findViewById<RecyclerView>(R.id.sessionControlRecyclerView)
                recyclerView.scrollToPosition(1)
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