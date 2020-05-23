/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.android.synthetic.main.mozac_ui_tabcounter_layout.view.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

class SmokeTest {
    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityTestRule = HomeActivityTestRule()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun verifyTheNavToolBarButtonsTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        //  - Visit a URL, wait until it's loaded
        homeScreen {
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                verifyPageContent(defaultWebPage.content)

                // Verify the items of Nav URL Bar
                verifyNavURLBarItems()

                // Verify 'x' button actions
                verifyXButtonActions()
                pressBack()
                TestAssetHelper.waitingTime

                // Verify various items after returning back to the initial WebPage
                verifyPageContent(defaultWebPage.content)
                verifyNavURLBar()
                }.openHomeScreen {

                // Verify items on HomeScreen
                verifyHomeScreen()
                verifyExistingTabList()
            }
        }
    }
}
