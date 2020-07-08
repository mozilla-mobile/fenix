/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.syncintegration

import android.os.SystemClock.sleep
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.ext.toUri
import org.mozilla.fenix.helpers.ext.waitNotNull
import org.mozilla.fenix.ui.robots.accountSettings
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.settingsSubMenuLoginsAndPassword

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class SyncIntegrationTest {
    val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
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

    // History item Desktop -> Fenix
    @Test
    fun checkHistoryFromDesktopTest() {
        signInFxSync()
        tapReturnToPreviousApp()
        // Let's wait until homescreen is shown to go to three dot menu
        TestAssetHelper.waitingTime
        mDevice.waitNotNull(Until.findObjects(By.res("org.mozilla.fenix.debug:id/counter_root")))
        homeScreen {
        }.openThreeDotMenu {
        }.openHistory {
        }
        historyAfterSyncIsShown()
    }

    // Bookmark item Desktop -> Fenix
    @Test
    fun checkBookmarkFromDesktopTest() {
        signInFxSync()
        tapReturnToPreviousApp()
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks { }
        bookmarkAfterSyncIsShown()
    }

    @Test
    fun checkAccountSettings() {
        signInFxSync()
        mDevice.waitNotNull(Until.findObjects(By.text("Account")), TestAssetHelper.waitingTime)

        goToAccountSettings()
        // This function to be added to the robot once the status of checkboxes can be checked
        // currently is not possible to select each one (History/Bookmark) and verify its status
        // verifyCheckBoxesSelected()
        // Then select/unselect each one and verify again that its status is correct
        // See issue #6544
        accountSettings {
            verifyBookmarksCheckbox()
            verifyHistoryCheckbox()
            verifySignOutButton()
            verifyDeviceName()
        }.disconnectAccount {
            mDevice.waitNotNull(Until.findObjects(By.text("Settings")), TestAssetHelper.waitingTime)
            verifySettingsView()
        }
    }

    // Login item Desktop -> Fenix
    @Test
    fun checkLoginsFromDesktopTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLoginsAndPasswordSubMenu {
        }.openSyncLogins {
            // Tap to sign in from Logins menu
            tapOnUseEmailToSignIn()
            typeEmail()
            tapOnContinueButton()
            typePassword()
            tapOnSignIn()
        }
        // Automatically goes back to Logins and passwords view
        settingsSubMenuLoginsAndPassword {
            verifyDefaultView()
            // Sync logings option is set to Off, no synced logins yet
            verifyDefaultViewBeforeSyncComplete()
        }.openSavedLogins {
            // Discard the secure your device message
            tapSetupLater()
            // Check the logins synced
            verifySavedLoginsAfterSync()
        }.goBack {
            // After checking the synced logins
            // on Logins and Passwords menu the Sync logins option is set to On
            verifyDefaultViewAfterSync()
        }
    }

    // Bookmark item Fenix -> Desktop
    @Test
    fun checkBookmarkFromDeviceTest() {
        val defaultWebPage = "example.com".toUri()!!
        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage) {
        }.openThreeDotMenu {
            verifyAddBookmarkButton()
            clickAddBookmarkButton()
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openTurnOnSyncMenu {
            useEmailInsteadButton()
            typeEmail()
            tapOnContinueButton()
            typePassword()
            sleep(TestAssetHelper.waitingTimeShort)
            tapOnSignIn()
        }
    }

    // History item Fenix -> Desktop
    @Test
    fun checkHistoryFromDeviceTest() {
        val defaultWebPage = "example.com".toUri()!!
        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage) {
        }.openThreeDotMenu {
        }.openSettings {
        }.openTurnOnSyncMenu {
            useEmailInsteadButton()
            typeEmail()
            tapOnContinueButton()
            typePassword()
            sleep(TestAssetHelper.waitingTimeShort)
            tapOnSignIn()
        }
    }

    // Useful functions for the tests
    fun typeEmail() {
        val emailInput = mDevice.findObject(UiSelector()
                .instance(0)
                .className(EditText::class.java))
        emailInput.waitForExists(TestAssetHelper.waitingTime)

        val emailAddress = javaClass.classLoader!!.getResource("email.txt").readText()
        emailInput.setText(emailAddress)
    }

    fun tapOnContinueButton() {
        val continueButton = mDevice.findObject(By.res("submit-btn"))
        continueButton.clickAndWait(Until.newWindow(), TestAssetHelper.waitingTime)
    }

    fun typePassword() {
        val passwordInput = mDevice.findObject(UiSelector()
                .instance(0)
                .className(EditText::class.java))

        val passwordValue = javaClass.classLoader!!.getResource("password.txt").readText()
        passwordInput.setText(passwordValue)
    }

    fun tapOnSignIn() {
        mDevice.waitNotNull(Until.findObjects(By.text("Sign in")))
        // Let's tap on enter, sometimes depending on the device the sign in button is
        // hidden by the keyboard
        mDevice.pressEnter()
    }

    fun historyAfterSyncIsShown() {
        mDevice.waitNotNull(Until.findObjects(By.text("http://www.example.com/")), TestAssetHelper.waitingTime)
    }

    fun bookmarkAfterSyncIsShown() {
        val bookmarkEntry = mDevice.findObject(By.text("Example Domain"))
        bookmarkEntry.isEnabled()
    }

    fun tapReturnToPreviousApp() {
        mDevice.waitNotNull(Until.findObjects(By.text("Save")), TestAssetHelper.waitingTime)
        mDevice.waitNotNull(Until.findObjects(By.text("Settings")), TestAssetHelper.waitingTime)

        /* Wait until the Settings shows the account synced */
        mDevice.waitNotNull(Until.findObjects(By.text("Account")), TestAssetHelper.waitingTime)
        mDevice.waitNotNull(Until.findObjects(By.res("org.mozilla.fenix.debug:id/email")), TestAssetHelper.waitingTime)
        TestAssetHelper.waitingTime
        // Go to Homescreen
        mDevice.pressBack()
    }

    fun signInFxSync() {
        homeScreen {
        }.openThreeDotMenu {
            verifySettingsButton()
        }.openSettings {}
        settingsAccount()
        useEmailInsteadButton()

        typeEmail()
        tapOnContinueButton()
        typePassword()
        sleep(TestAssetHelper.waitingTimeShort)
        tapOnSignIn()
    }

    fun goToAccountSettings() {
        enterAccountSettings()
        mDevice.waitNotNull(Until.findObjects(By.text("Device name")), TestAssetHelper.waitingTime)
    }
}

fun settingsAccount() = onView(allOf(withText("Turn on Sync"))).perform(click())
fun useEmailInsteadButton() = onView(withId(R.id.signInEmailButton)).perform(click())
fun enterAccountSettings() = onView(withId(R.id.email)).perform(click())
