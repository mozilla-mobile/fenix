package org.mozilla.fenix.library.history

import android.content.res.Resources
import androidx.paging.PagingSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.history.HistoryDB
import org.mozilla.fenix.components.history.PagedHistoryProvider
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class HistoryDataSourceTest {

    private val historyProvider: PagedHistoryProvider = mockk()
    private val browserStore: BrowserStore = mockk()
    private val accountManager: FxaAccountManager = mockk()

    @Before
    fun setup() {
        coEvery {
            historyProvider.getHistory(0, 25, null)
        } returns listOf(
            createRegularHistory(isRemote = false),
            createRegularHistory(isRemote = false),
            createRegularHistory(isRemote = true),
            createRegularHistory(isRemote = true),
            createRegularHistory(isRemote = true),
            createRegularHistory(isRemote = true),
        )

        every { accountManager.authenticatedAccount() } returns mockk()
        every { browserStore.state } returns BrowserState()
    }

    @Test
    fun `WHEN data source is mixed THEN a loaded page contains both remote and local items`() = runTest {
        val dataSource = createDataSource(isRemote = null)

        val firstPage = dataSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 25,
                placeholdersEnabled = false
            )
        )
        val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

        assert(loadedItems.filterIsInstance<HistoryViewItem.HistoryItem>().size == 6)
    }

    @Test
    fun `WHEN data source is remote THEN a loaded page contains only remote items`() = runTest {
        val dataSource = createDataSource(isRemote = true)

        val firstPage = dataSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 25,
                placeholdersEnabled = false
            )
        )
        val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

        assert(loadedItems.filterIsInstance<HistoryViewItem.HistoryItem>().size == 4)
    }

    @Test
    fun `WHEN data source is local THEN a loaded page contains only local items`() = runTest {
        val dataSource = createDataSource(isRemote = false)

        val firstPage = dataSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 25,
                placeholdersEnabled = false
            )
        )
        val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

        assert(loadedItems.filterIsInstance<HistoryViewItem.HistoryItem>().size == 2)
    }

    @Test
    fun `WHEN data source is remote and the user is not authenticated THEN the first loaded page contains a sign in item`() =
        runTest {
            val dataSource = createDataSource(isRemote = true)
            every { accountManager.authenticatedAccount() } returns null

            val firstPage = dataSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 25,
                    placeholdersEnabled = false
                )
            )
            val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

            assert(loadedItems.contains(HistoryViewItem.SignInHistoryItem))
        }

    @Test
    fun `WHEN data source is local and the user is not authenticated THEN the first loaded page does not contain sign in item`() =
        runTest {
            val dataSource = createDataSource(isRemote = false)
            every { accountManager.authenticatedAccount() } returns null

            val firstPage = dataSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 25,
                    placeholdersEnabled = false
                )
            )
            val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

            assertFalse(loadedItems.contains(HistoryViewItem.SignInHistoryItem))
        }

    @Test
    fun `GIVEN items of all time frames THEN the page contains headers of all types`() =
        runTest {
            val dataSource = createDataSource(isRemote = null)
            coEvery {
                historyProvider.getHistory(0, 25, null)
            } returns listOf(
                createRegularHistory(
                    isRemote = false,
                    visitedAt = Calendar.getInstance().timeInMillis
                ),
                createRegularHistory(
                    isRemote = false,
                    visitedAt = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -1)
                    }.timeInMillis
                ),
                createRegularHistory(
                    isRemote = false,
                    visitedAt = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -4)
                    }.timeInMillis
                ),
                createRegularHistory(
                    isRemote = false,
                    visitedAt = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -15)
                    }.timeInMillis
                ),
                createRegularHistory(
                    isRemote = false,
                    visitedAt = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -35)
                    }.timeInMillis
                ),
            )

            val firstPage = dataSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 25,
                    placeholdersEnabled = false
                )
            )
            val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

            assertNotNull(
                loadedItems.find {
                    it is HistoryViewItem.TimeGroupHeader &&
                        it.timeGroup == HistoryItemTimeGroup.Today
                }
            )
            assertNotNull(
                loadedItems.find {
                    it is HistoryViewItem.TimeGroupHeader &&
                        it.timeGroup == HistoryItemTimeGroup.Yesterday
                }
            )
            assertNotNull(
                loadedItems.find {
                    it is HistoryViewItem.TimeGroupHeader &&
                        it.timeGroup == HistoryItemTimeGroup.ThisWeek
                }
            )
            assertNotNull(
                loadedItems.find {
                    it is HistoryViewItem.TimeGroupHeader &&
                        it.timeGroup == HistoryItemTimeGroup.ThisMonth
                }
            )
            assertNotNull(
                loadedItems.find {
                    it is HistoryViewItem.TimeGroupHeader &&
                        it.timeGroup == HistoryItemTimeGroup.Older
                }
            )
        }

    @Test
    fun `GIVEN items are not sorted properly by their time group THEN headers are not duplicated`() = runTest {
        val dataSource = createDataSource(isRemote = false)
        coEvery {
            historyProvider.getHistory(0, 25, null)
        } returns listOf(
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().timeInMillis
            ),
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }.timeInMillis
            ),
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().timeInMillis
            ),
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }.timeInMillis
            ),
        )

        val firstPage = dataSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 25,
                placeholdersEnabled = false
            )
        )
        val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

        assert(
            loadedItems.count {
                it is HistoryViewItem.TimeGroupHeader &&
                    it.timeGroup == HistoryItemTimeGroup.Today
            } == 1
        )
        assert(
            loadedItems.count {
                it is HistoryViewItem.TimeGroupHeader &&
                    it.timeGroup == HistoryItemTimeGroup.Yesterday
            } == 1
        )
    }

    @Test
    fun `WHEN data source is mixed THEN the first loaded page contains a recently closed item`() = runTest {
        val dataSource = createDataSource(isRemote = null)

        val firstPage = dataSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 25,
                placeholdersEnabled = false
            )
        )
        val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

        assertNotNull(loadedItems.find { it is HistoryViewItem.RecentlyClosedItem })
    }

    @Test
    fun `WHEN data source is local THEN the first loaded page does not contain a recently closed item`() = runTest {
        val dataSource = createDataSource(isRemote = false)

        val firstPage = dataSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 25,
                placeholdersEnabled = false
            )
        )
        val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

        assertNotNull(loadedItems.find { it is HistoryViewItem.RecentlyClosedItem })
    }

    @Test
    fun `WHEN data source is local THEN the first loaded page contains a synced history item`() = runTest {
        val dataSource = createDataSource(isRemote = false)

        val firstPage = dataSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 25,
                placeholdersEnabled = false
            )
        )
        val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

        assertNotNull(loadedItems.find { it is HistoryViewItem.SyncedHistoryItem })
    }

    @Test
    fun `WHEN data source is remote THEN the first loaded page does not contain a synced history item`() = runTest {
        val dataSource = createDataSource(isRemote = true)

        val firstPage = dataSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 25,
                placeholdersEnabled = false
            )
        )
        val loadedItems = (firstPage as PagingSource.LoadResult.Page).data

        assertNull(loadedItems.find { it is HistoryViewItem.SyncedHistoryItem })
    }

    private fun createRegularHistory(
        isRemote: Boolean = false,
        visitedAt: Long = 1658496038799,
    ) = HistoryDB.Regular(
        title = "Mozilla",
        url = "mozilla.org",
        visitedAt = visitedAt,
        selected = false,
        isRemote = isRemote
    )

    private fun createDataSource(
        historyProvider: PagedHistoryProvider = this@HistoryDataSourceTest.historyProvider,
        browserStore: BrowserStore = this@HistoryDataSourceTest.browserStore,
        isRemote: Boolean? = null,
        resources: Resources = testContext.resources,
        accountManager: FxaAccountManager = this@HistoryDataSourceTest.accountManager
    ) = HistoryDataSource(
        historyProvider = historyProvider,
        browserStore = browserStore,
        isRemote = isRemote,
        resources = resources,
        accountManager = accountManager
    )
}
