/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentvisits.controller

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import mozilla.components.browser.state.action.HistoryMetadataAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.storage.DocumentType
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.HistoryMetadataKey
import mozilla.components.concept.storage.HistoryMetadataStorage
import mozilla.components.feature.tabs.TabsUseCases.SelectOrAddUseCase
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.RecentSearches
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(FenixRobolectricTestRunner::class)
class RecentVisitsControllerTest {

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)
    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()
    private val scope = coroutinesTestRule.scope

    private val selectOrAddTabUseCase: SelectOrAddUseCase = mockk(relaxed = true)
    private val navController = mockk<NavController>(relaxed = true)

    private lateinit var storage: HistoryMetadataStorage
    private lateinit var appStore: AppStore
    private lateinit var store: BrowserStore

    private lateinit var controller: DefaultRecentVisitsController

    @Before
    fun setup() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.homeFragment
        }
        storage = mockk(relaxed = true)
        appStore = mockk(relaxed = true)
        store = mockk(relaxed = true)

        controller = spyk(
            DefaultRecentVisitsController(
                appStore = appStore,
                store = store,
                selectOrAddTabUseCase = selectOrAddTabUseCase,
                navController = navController,
                scope = scope,
                storage = storage,
            )
        )
    }

    @Test
    fun handleHistoryShowAllClicked() = runTestOnMain {
        controller.handleHistoryShowAllClicked()

        verify {
            controller.dismissSearchDialogIfDisplayed()
            navController.navigate(
                HomeFragmentDirections.actionGlobalHistoryFragment()
            )
        }
    }

    @Test
    fun handleRecentHistoryGroupClicked() = runTestOnMain {
        val historyEntry = HistoryMetadata(
            key = HistoryMetadataKey("http://www.mozilla.com", "mozilla", null),
            title = "mozilla",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            totalViewTime = 10,
            documentType = DocumentType.Regular,
            previewImageUrl = null
        )
        val historyGroup = RecentHistoryGroup(
            title = "mozilla",
            historyMetadata = listOf(historyEntry)
        )

        controller.handleRecentHistoryGroupClicked(historyGroup)

        verify {
            navController.navigate(
                match<NavDirections> { it.actionId == R.id.action_global_history_metadata_group }
            )
        }
    }

    @Test
    fun handleRemoveGroup() = runTestOnMain {
        val historyMetadataKey = HistoryMetadataKey(
            "http://www.mozilla.com",
            "mozilla",
            null
        )

        val historyGroup = RecentHistoryGroup(
            title = "mozilla",
            historyMetadata = listOf(
                HistoryMetadata(
                    key = historyMetadataKey,
                    title = "mozilla",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    totalViewTime = 10,
                    documentType = DocumentType.Regular,
                    previewImageUrl = null
                )
            )
        )
        assertFalse(RecentSearches.groupDeleted.testHasValue())

        controller.handleRemoveRecentHistoryGroup(historyGroup.title)

        advanceUntilIdle()
        verify {
            store.dispatch(HistoryMetadataAction.DisbandSearchGroupAction(searchTerm = historyGroup.title))
            appStore.dispatch(AppAction.DisbandSearchGroupAction(searchTerm = historyGroup.title))
        }
        assertTrue(RecentSearches.groupDeleted.testHasValue())

        coVerify {
            storage.deleteHistoryMetadata(historyGroup.title)
        }
    }

    @Test
    fun handleRecentHistoryHighlightClicked() = runTestOnMain {
        val historyHighlight = RecentHistoryHighlight("title", "url")

        controller.handleRecentHistoryHighlightClicked(historyHighlight)

        verifyOrder {
            selectOrAddTabUseCase.invoke(historyHighlight.url)
            navController.navigate(R.id.browserFragment)
        }
    }

    @Test
    fun handleRemoveRecentHistoryHighlight() = runTestOnMain {
        val highlightUrl = "highlightUrl"
        controller.handleRemoveRecentHistoryHighlight(highlightUrl)

        verify {
            appStore.dispatch(AppAction.RemoveRecentHistoryHighlight(highlightUrl))
            scope.launch {
                storage.deleteHistoryMetadataForUrl(highlightUrl)
            }
        }
    }
}
