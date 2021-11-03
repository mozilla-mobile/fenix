/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.content.Intent
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import org.junit.Assert.assertNotNull
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
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.ui.robots.mDevice
import org.mozilla.fenix.utils.view.ViewHolder

@RunWith(AndroidJUnit4::class)
class HomeFragmentBenchmark {

    private val resourceId = "sessionControlRecyclerView"

    private val homeButton = "Home screen"
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

            // The homescreen could potentially take a while to show on the screen.
            // Therefore, we should wait and make sure that the Main Thread is idled.
            // `waitForIdleSync()` waits for the message queue to be empty. Source:
            // https://android.googlesource.com/platform/frameworks/base/+/8f6f1f43eeb0d7263b626978ee2c21d4053bf610/core/java/android/app/Instrumentation.java#328
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            // The test has to scroll to the bottom since there is a possibility not everything is
            // laid out yet. For example, on smaller screen size (such as the Moto G5), pocket stories
            // will not be shown until the user scrolls down to it. By triggering a scrolling action
            // dependent on the device's height, we force everything to be laid out before the run
            // stops.
            val rv = getRecyclerView()
            while(rv.canScrollVertically(1)){
                homeActivityRule.runOnUiThread {
                    rv.scrollBy(0, getDeviceHeight())
                }
            }

            runWithTimingDisabled {
                // Make sure that everything is shown on screen. The recyclerview should be the last
                // thing to be drawn.
                assertNotNull(mDevice.findObject(By.res(TestHelper.packageName, resourceId)))

                homeButton = null
                // Click on the "Jump Back In" tab that was created  in the `Setup()`. This should be
                // the third item in the recyclerview.
                onView(withId(R.id.sessionControlRecyclerView)).perform(
                    RecyclerViewActions.actionOnItemAtPosition<ViewHolder>(2, click())
                )
            }
        }
    }

    /**
     * To calculate the device physical height, the old api `WindowManager.defaultDisplay` is used.
     * Android 30 deprecated both `Display.getSize()` and `defaultDisplay`. However, to be compatible
     * with older phones, the older API must be used.
     */
    @Suppress("Deprecation")
    private fun getDeviceHeight() : Int {
        val display = homeActivityRule.activity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.y
    }

    private fun getRecyclerView() : RecyclerView =
        (homeActivityRule.activity.supportFragmentManager.fragments[0]
            .childFragmentManager.fragments[0] as HomeFragment).sessionControlView!!.view

    /**
     * DFS search for the home screen button since it is created dynamically in
     * `BaseBrowserFragment.kt`. This AppCompatButton is created as an action and doesn't have a
     * resource ID making it hard to do `findViewById`. However, since it contains a contentDescription
     * we can find it with it.
     */
    private fun getHomeButton(view: View): View? {
        if (view is ViewGroup) {
            return view.children.firstNotNullOfOrNull(::getHomeButton)
        }

        return if (view.contentDescription == homeButton) view else null
    }
}
