/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.android.ComposeNotIdleException
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.Constants
import org.mozilla.fenix.helpers.Constants.LONG_CLICK_DURATION
import org.mozilla.fenix.helpers.Constants.RETRY_COUNT
import org.mozilla.fenix.helpers.Constants.SPEECH_RECOGNITION
import org.mozilla.fenix.helpers.SessionLoadedIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.getStringResource
import org.mozilla.fenix.helpers.TestHelper.isPackageInstalled
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the search fragment.
 */
class SearchRobot {
    fun verifySearchView() = assertSearchView()
    fun verifyBrowserToolbar() = assertBrowserToolbarEditView()
    fun verifyScanButton() = assertScanButton()

    fun verifyVoiceSearchButtonVisibility(enabled: Boolean) {
        if (enabled) {
            assertTrue(voiceSearchButton.waitForExists(waitingTime))
        } else {
            assertFalse(voiceSearchButton.waitForExists(waitingTime))
        }
    }

    // Device or AVD requires a Google Services Android OS installation
    fun startVoiceSearch() {
        voiceSearchButton.click()

        // Accept runtime permission (API 30) for Google Voice
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            val allowPermission = mDevice.findObject(
                UiSelector().text(
                    when {
                        Build.VERSION.SDK_INT == Build.VERSION_CODES.R -> "Allow all the time"
                        else -> "While using the app"
                    },
                ),
            )

            if (allowPermission.exists()) {
                allowPermission.click()
            }

            if (isPackageInstalled(Constants.PackageName.GOOGLE_QUICK_SEARCH)) {
                Intents.intended(IntentMatchers.hasAction(SPEECH_RECOGNITION))
            }
        }
    }

    fun verifySearchEngineButton() = assertSearchButton()

    fun verifySearchEngineSuggestionResults(rule: ComposeTestRule, searchSuggestion: String) {
        rule.waitForIdle()
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(UiSelector().textContains(searchSuggestion))
                        .waitForExists(waitingTime),
                )
                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    expandSearchSuggestionsList()
                }
            }
        }
    }

    fun verifyFirefoxSuggestResults(rule: ComposeTestRule, searchSuggestion: String) {
        rule.waitForIdle()
        for (i in 1..RETRY_COUNT) {
            try {
                rule.onNodeWithTag("mozac.awesomebar.suggestions")
                    .performScrollToNode(hasText(searchSuggestion))
                    .assertExists()
                break
            } catch (e: AssertionError) {
                if (i == RETRY_COUNT) {
                    throw e
                } else {
                    expandSearchSuggestionsList()
                }
            }
        }
    }

    fun verifyNoSuggestionsAreDisplayed(rule: ComposeTestRule, searchSuggestion: String) {
        rule.waitForIdle()

        assertFalse(
            mDevice.findObject(UiSelector().textContains(searchSuggestion))
                .waitForExists(waitingTime),
        )
    }

    fun verifyAllowSuggestionsInPrivateModeDialog() {
        assertTrue(
            mDevice.findObject(
                UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_title)),
            ).waitForExists(waitingTime),
        )
        assertTrue(
            mDevice.findObject(
                UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_text)),
            ).exists(),
        )
        assertTrue(
            mDevice.findObject(
                UiSelector().text("Learn more"),
            ).exists(),
        )
        assertTrue(
            mDevice.findObject(
                UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_allow_button)),
            ).exists(),
        )
        assertTrue(
            mDevice.findObject(
                UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_do_not_allow_button)),
            ).exists(),
        )
    }

    fun denySuggestionsInPrivateMode() {
        mDevice.findObject(
            UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_do_not_allow_button)),
        ).click()
    }

    fun allowSuggestionsInPrivateMode() {
        mDevice.findObject(
            UiSelector().text(getStringResource(R.string.search_suggestions_onboarding_allow_button)),
        ).click()
    }

    fun verifySearchEnginePrompt(rule: ComposeTestRule, searchEngineName: String) =
        assertSearchEnginePrompt(rule, searchEngineName)
    fun verifySearchBarEmpty() = assertSearchBarEmpty()

    fun verifyKeyboardVisibility() = assertKeyboardVisibility(isExpectedToBeVisible = true)
    fun verifySearchEngineList(rule: ComposeTestRule) = rule.assertSearchEngineList()
    fun verifySearchEngineIcon(expectedText: String) {
        onView(withContentDescription(expectedText))
    }
    fun verifyDefaultSearchEngine(expectedText: String) = assertDefaultSearchEngine(expectedText)

    fun verifyEnginesListShortcutContains(rule: ComposeTestRule, searchEngineName: String) = assertEngineListShortcutContains(rule, searchEngineName)

    fun changeDefaultSearchEngine(rule: ComposeTestRule, searchEngineName: String) =
        rule.selectDefaultSearchEngine(searchEngineName)

    fun clickSearchEngineShortcutButton() {
        val searchEnginesShortcutButton = mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/search_engines_shortcut_button"),
        )
        searchEnginesShortcutButton.waitForExists(waitingTime)
        searchEnginesShortcutButton.click()
    }

    fun clickScanButton() {
        scanButton.waitForExists(waitingTime)
        scanButton.click()
    }

    fun clickDismissPermissionRequiredDialog() {
        dismissPermissionButton.waitForExists(waitingTime)
        dismissPermissionButton.click()
    }

    fun clickGoToPermissionsSettings() {
        goToPermissionsSettingsButton.waitForExists(waitingTime)
        goToPermissionsSettingsButton.click()
    }

    fun verifyScannerOpen() {
        assertTrue(
            mDevice.findObject(UiSelector().resourceId("$packageName:id/view_finder"))
                .waitForExists(waitingTime),
        )
    }

    fun typeSearch(searchTerm: String) {
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"),
        ).waitForExists(waitingTime)

        browserToolbarEditView().setText(searchTerm)

        mDevice.waitForIdle()
    }

    @OptIn(ExperimentalTestApi::class)
    fun scrollToSearchEngineSettings(rule: ComposeTestRule) {
        // Soft keyboard is visible on screen on view access; hide it
        onView(allOf(withId(R.id.search_wrapper))).perform(
            closeSoftKeyboard(),
        )

        mDevice.findObject(UiSelector().text("Google"))
            .waitForExists(waitingTime)

        rule.onNodeWithTag("mozac.awesomebar.suggestions")
            .performScrollToIndex(5)
    }

    fun clickClearButton() {
        clearButton().click()
    }

    fun longClickToolbar() {
        mDevice.waitForWindowUpdate(packageName, waitingTime)
        mDevice.findObject(UiSelector().resourceId("$packageName:id/awesomeBar"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
            .waitForExists(waitingTime)
        val toolbar = mDevice.findObject(By.res("$packageName:id/toolbar"))
        toolbar.click(LONG_CLICK_DURATION)
    }

    fun clickPasteText() {
        mDevice.findObject(UiSelector().textContains("Paste")).waitForExists(waitingTime)
        val pasteText = mDevice.findObject(By.textContains("Paste"))
        pasteText.click()
    }

    fun clickSearchEnginePrompt(rule: ComposeTestRule, searchEngineName: String) =
        rule.onNodeWithText("Search $searchEngineName").performClick()

    fun expandSearchSuggestionsList() {
        onView(allOf(withId(R.id.search_wrapper))).perform(
            closeSoftKeyboard(),
        )
        awesomeBar.swipeUp(2)
    }

    fun verifyTranslatedFocusedNavigationToolbar(toolbarHintString: String) =
        assertTranslatedFocusedNavigationToolbar(toolbarHintString)

    fun verifySearchEngineShortcuts(rule: ComposeTestRule, vararg searchEngines: String) {
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/awesome_bar"),
        ).swipeUp(1)

        for (searchEngine in searchEngines) {
            rule.waitForIdle()
            rule.onNodeWithText(searchEngine).assertIsDisplayed()
        }
    }

    fun verifySearchEngineShortcutsAreNotDisplayed(rule: ComposeTestRule, vararg searchEngines: String) {
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/pill_wrapper_divider"),
        ).waitForExists(waitingTime)

        for (searchEngine in searchEngines) {
            rule.waitForIdle()
            rule.onNodeWithText(searchEngine).assertDoesNotExist()
        }
    }

    fun verifyTypedToolbarText(expectedText: String) {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_url_view"))
            .waitForExists(waitingTime)
        onView(
            allOf(
                withText(expectedText),
                withId(R.id.mozac_browser_toolbar_edit_url_view),
            ),
        ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    class Transition {
        private lateinit var sessionLoadedIdlingResource: SessionLoadedIdlingResource

        fun dismissSearchBar(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {
            mDevice.waitForIdle()
            closeSoftKeyboard()
            mDevice.pressBack()
            try {
                assertTrue(searchWrapper().waitUntilGone(waitingTimeShort))
            } catch (e: AssertionError) {
                mDevice.pressBack()
                assertTrue(searchWrapper().waitUntilGone(waitingTimeShort))
            }

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun openBrowser(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.waitForIdle()
            browserToolbarEditView().setText("mozilla\n")
            mDevice.pressEnter()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun submitQuery(query: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            sessionLoadedIdlingResource = SessionLoadedIdlingResource()
            searchWrapper().waitForExists(waitingTime)
            browserToolbarEditView().setText(query)
            mDevice.pressEnter()

            runWithIdleRes(sessionLoadedIdlingResource) {
                assertTrue(
                    mDevice.findObject(
                        UiSelector().resourceId("$packageName:id/browserLayout"),
                    ).waitForExists(waitingTime),
                )
            }

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickSearchEngineSettings(
            rule: ComposeTestRule,
            interact: SettingsSubMenuSearchRobot.() -> Unit,
        ): SettingsSubMenuSearchRobot.Transition {
            rule.onNodeWithText("Search engine settings")
                .assertIsDisplayed()
                .assertHasClickAction()
                .performClick()

            SettingsSubMenuSearchRobot().interact()
            return SettingsSubMenuSearchRobot.Transition()
        }
    }
}

private fun browserToolbarEditView() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"))

private val dismissPermissionButton =
    mDevice.findObject(UiSelector().text("DISMISS"))

private val goToPermissionsSettingsButton =
    mDevice.findObject(UiSelector().text("GO TO SETTINGS"))

private val scanButton =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/qr_scan_button"))

private fun clearButton() =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_clear_view"))

private fun searchWrapper() = mDevice.findObject(UiSelector().resourceId("$packageName:id/search_wrapper"))

private fun assertSearchEnginePrompt(rule: ComposeTestRule, searchEngineName: String) {
    rule.waitForIdle()
    rule.onNodeWithText("Search $searchEngineName").assertIsDisplayed()
    rule.onNodeWithText(
        getStringResource(R.string.search_engine_suggestions_description),
    ).assertIsDisplayed()
}

private fun assertSearchView() =
    assertTrue(
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/search_wrapper"),
        ).waitForExists(waitingTime),
    )

private fun assertBrowserToolbarEditView() =
    assertTrue(
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"),
        ).waitForExists(waitingTime),
    )

private fun assertScanButton() =
    assertTrue(
        scanButton.waitForExists(waitingTime),
    )

private fun assertSearchButton() =
    assertTrue(
        mDevice.findObject(
            UiSelector().resourceId("$packageName:id/search_engines_shortcut_button"),
        ).waitForExists(waitingTime),
    )

private fun assertSearchBarEmpty() =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view")
                .textContains(""),
        ).waitForExists(waitingTime),
    )

fun searchScreen(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
    SearchRobot().interact()
    return SearchRobot.Transition()
}

private fun assertKeyboardVisibility(isExpectedToBeVisible: Boolean): () -> Unit = {
    searchWrapper().waitForExists(waitingTime)

    assertEquals(
        "Keyboard not shown",
        isExpectedToBeVisible,
        mDevice
            .executeShellCommand("dumpsys input_method | grep mInputShown")
            .contains("mInputShown=true"),
    )
}

private fun ComposeTestRule.assertSearchEngineList() {
    onView(withId(R.id.mozac_browser_toolbar_edit_icon)).click()

    onNodeWithText("Google")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("Amazon.com")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("Bing")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("DuckDuckGo")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("Wikipedia")
        .assertExists()
        .assertIsDisplayed()

    onNodeWithText("eBay")
        .assertExists()
        .assertIsDisplayed()
}

@OptIn(ExperimentalTestApi::class)
private fun assertEngineListShortcutContains(rule: ComposeTestRule, searchEngineName: String) {
    try {
        rule.waitForIdle()
    } catch (e: ComposeNotIdleException) {
        mDevice.pressBack()
        navigationToolbar {
        }.clickUrlbar {
            clickSearchEngineShortcutButton()
        }
    } finally {
        mDevice.findObject(
            UiSelector().textContains("Google"),
        ).waitForExists(waitingTime)

        rule.onNodeWithTag("mozac.awesomebar.suggestions")
            .performScrollToIndex(5)

        rule.onNodeWithText(searchEngineName)
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}

private fun ComposeTestRule.selectDefaultSearchEngine(searchEngine: String) {
    onView(withId(R.id.mozac_browser_toolbar_edit_icon)).click()

    onNodeWithText(searchEngine)
        .assertExists()
        .assertIsDisplayed()
        .performClick()
}

private fun assertDefaultSearchEngine(expectedText: String) =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/mozac_browser_toolbar_edit_icon")
                .descriptionContains(expectedText),
        ).waitForExists(waitingTime),
    )

private fun assertTranslatedFocusedNavigationToolbar(toolbarHintString: String) =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view")
                .textContains(toolbarHintString),
        ).waitForExists(waitingTime),
    )

private val awesomeBar =
    mDevice.findObject(UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_edit_url_view"))

private val voiceSearchButton = mDevice.findObject(UiSelector().description("Voice search"))

private fun goBackButton() = onView(allOf(withContentDescription("Navigate up")))
