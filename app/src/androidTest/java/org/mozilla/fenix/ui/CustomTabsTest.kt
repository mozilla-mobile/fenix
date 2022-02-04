package org.mozilla.fenix.ui

import androidx.core.net.toUri
import androidx.test.rule.ActivityTestRule
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.createCustomTabIntent
import org.mozilla.fenix.helpers.TestHelper.openAppFromExternalLink
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.customTabScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.openEditURLView
import org.mozilla.fenix.ui.robots.searchScreen

class CustomTabsTest {
    private lateinit var mockWebServer: MockWebServer
    private val customMenuItem = "TestMenuItem"
    private val externalLinksPWAPage = "https://andiaj.github.io/testapp/htmlControlsForm.html"
    private val permissionsTestPage = "https://mozilla-mobile.github.io/testapp/permissions"
    private val testPageSubstring = "https://mozilla-mobile.github.io:443"
    private val hour = 10
    private val minute = 10

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule()

    @get: Rule
    val intentReceiverActivityTestRule = ActivityTestRule(
        IntentReceiverActivity::class.java, true, false
    )

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @SmokeTest
    @Test
    fun customTabsOpenExternalLinkTest() {

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                externalLinksPWAPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
            clickLinkMatchingText("External link")
            waitForPageToLoad()
            verifyCustomTabToolbarTitle("DuckDuckGo")
        }
    }

    @SmokeTest
    @Test
    fun customTabsSaveLoginTest() {

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                externalLinksPWAPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
            fillAndSubmitLoginCredentials("mozilla", "firefox")
            verifySaveLoginPromptIsDisplayed()
            saveLoginFromPrompt("Save")
        }

        openAppFromExternalLink(externalLinksPWAPage)

        browserScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLoginsAndPasswordSubMenu {
        }.openSavedLogins {
            verifySecurityPromptForLogins()
            tapSetupLater()
            verifySavedLoginFromPrompt("mozilla")
        }
    }

    @SmokeTest
    @Test
    fun customTabsSetCalendarFormTest() {

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                externalLinksPWAPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
            clickForm("Calendar Form", true)
            selectDate()
            clickFormViewButton("OK")
            clickSubmitDateButton()
            verifySelectedDate()
        }
    }

    @SmokeTest
    @Test
    fun customTabsSetClockFormTest() {

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                externalLinksPWAPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
            clickForm("Clock Form")
            selectTime(hour, minute)
            clickFormViewButton("OK")
            clickSubmitTimeButton()
            verifySelectedTime(hour, minute)
        }
    }

    @SmokeTest
    @Test
    fun customTabsDropDownFormTest() {

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                externalLinksPWAPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
            clickDropDownForm()
            selectDropDownOption("The National")
            clickSubmitDropDownButton()
            verifySelectedDropDownOption("The National")
        }
    }

    @SmokeTest
    @Test
    fun customTabsCameraPermissionTest() {

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                permissionsTestPage.toUri().toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
        }.clickStartCameraButton {
            clickAppPermissionButton(true)
            verifyCameraPermissionPrompt(testPageSubstring)
        }.clickPagePermissionButton(true) {
            verifyPageContent("Camera allowed")
        }
    }

    @SmokeTest
    @Test
    fun customTabCopyToolbarUrlTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem
            )
        )

        customTabScreen {
            longCLickAndCopyToolbarUrl()
        }

        openAppFromExternalLink(customTabPage.url.toString())

        navigationToolbar {
            openEditURLView()
        }

        searchScreen {
            clickClearButton()
            longClickToolbar()
            clickPasteText()
            verifyPastedToolbarText(customTabPage.url.toString())
        }
    }

    @SmokeTest
    @Test
    fun customTabShareTextTest() {
        val customTabPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        intentReceiverActivityTestRule.launchActivity(
            createCustomTabIntent(
                customTabPage.url.toString(),
                customMenuItem
            )
        )

        customTabScreen {
            waitForPageToLoad()
        }

        browserScreen {
            longClickMatchingText("content")
        }.clickShareSelectedText {
            verifyAndroidShareLayout()
        }
    }
}
