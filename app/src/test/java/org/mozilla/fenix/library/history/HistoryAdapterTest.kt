package org.mozilla.fenix.library.history

import androidx.paging.PagingData
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import java.util.Calendar

@RunWith(FenixRobolectricTestRunner::class)
internal class HistoryAdapterTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `WHEN the only item in the list is deleted THEN header is deleted as well`() {
        val adapter = HistoryAdapter(mockk()) {}

        val removedItem = createHistory(visitedAt = Calendar.getInstance().timeInMillis)
        val notRemovedItem = createHistory(
            visitedAt = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.timeInMillis
        )
        val snapshot = listOf(
            HistoryViewItem.TopSeparatorHistoryItem,
            HistoryViewItem.TimeGroupHeader(
                title = removedItem.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = removedItem.historyTimeGroup,
                collapsed = false,
            ),
            createHistoryViewItem(removedItem),
            HistoryViewItem.TimeGroupSeparatorHistoryItem(removedItem.historyTimeGroup),
            HistoryViewItem.TimeGroupHeader(
                title = notRemovedItem.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = notRemovedItem.historyTimeGroup,
                collapsed = false,
            ),
            createHistoryViewItem(notRemovedItem)
        )

        assert(
            adapter.calculateTimeGroupsToRemove(
                removedItems = setOf(removedItem),
                snapshot = snapshot
            ).contains(removedItem.historyTimeGroup)
        )
    }

    @Test
    fun `WHEN the only item in the group is deleted THEN header of that group is deleted as well`() {
        val adapter = HistoryAdapter(mockk()) {}

        val now = Calendar.getInstance().timeInMillis
        val removedItem = createHistory(visitedAt = now)
        val snapshot = listOf(
            HistoryViewItem.TopSeparatorHistoryItem,
            HistoryViewItem.TimeGroupHeader(
                title = removedItem.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = removedItem.historyTimeGroup,
                collapsed = false
            ),
            createHistoryViewItem(removedItem)
        )

        assert(
            adapter.calculateTimeGroupsToRemove(
                removedItems = setOf(removedItem),
                snapshot = snapshot
            ).contains(removedItem.historyTimeGroup)
        )
    }

    @Test
    fun `WHEN not all items inside a group are deleted THEN header of the group is not deleted`() {
        val adapter = HistoryAdapter(mockk()) {}

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val removedItemA = createHistory(visitedAt = today.timeInMillis)
        val removedItemB = createHistory(visitedAt = today.timeInMillis)
        val removedItemC = createHistory(visitedAt = today.timeInMillis)
        val notRemovedItem = createHistory(visitedAt = today.timeInMillis)

        val snapshot = listOf(
            HistoryViewItem.TopSeparatorHistoryItem,
            HistoryViewItem.TimeGroupHeader(
                title = removedItemA.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = removedItemA.historyTimeGroup,
                collapsed = false,
            ),
            createHistoryViewItem(removedItemA),
            createHistoryViewItem(removedItemB),
            createHistoryViewItem(removedItemC),
            createHistoryViewItem(notRemovedItem)
        )

        assert(
            !adapter.calculateTimeGroupsToRemove(
                removedItems = setOf(removedItemA, removedItemB, removedItemC),
                snapshot = snapshot
            ).contains(notRemovedItem.historyTimeGroup)
        )
    }

    @Test
    fun `WHEN all items inside a group are deleted THEN header of the group is deleted as well`() {
        val adapter = HistoryAdapter(mockk()) {}

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val removedItemA = createHistory(visitedAt = today.timeInMillis)
        val removedItemB = createHistory(visitedAt = today.timeInMillis)
        val removedItemC = createHistory(visitedAt = today.timeInMillis)
        val removedItemD = createHistory(visitedAt = today.timeInMillis)

        val snapshot = listOf(
            HistoryViewItem.TopSeparatorHistoryItem,
            HistoryViewItem.TimeGroupHeader(
                title = removedItemA.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = removedItemA.historyTimeGroup,
                collapsed = false,
            ),
            createHistoryViewItem(removedItemA),
            createHistoryViewItem(removedItemB),
            createHistoryViewItem(removedItemC),
            createHistoryViewItem(removedItemD)
        )

        assert(
            !adapter.calculateTimeGroupsToRemove(
                removedItems = setOf(removedItemA, removedItemB, removedItemC),
                snapshot = snapshot
            ).contains(HistoryItemTimeGroup.Today)
        )
    }

    @Test
    fun `WHEN all items inside multiple groups are deleted THEN headers of the groups are deleted as well`() {
        val adapter = HistoryAdapter(mockk()) {}

        val removedItemA = createHistory(
            visitedAt = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 0)
            }.timeInMillis
        )
        val notRemovedItemA = createHistory(
            visitedAt = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.timeInMillis
        )
        val removedItemB = createHistory(
            visitedAt = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -4)
            }.timeInMillis
        )
        val notRemovedItemB = createHistory(
            visitedAt = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -15)
            }.timeInMillis
        )

        val snapshot = listOf(
            HistoryViewItem.TopSeparatorHistoryItem,

            HistoryViewItem.TimeGroupHeader(
                title = removedItemA.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = removedItemA.historyTimeGroup,
                collapsed = false,
            ),
            createHistoryViewItem(removedItemA),
            HistoryViewItem.TimeGroupSeparatorHistoryItem(removedItemA.historyTimeGroup),

            HistoryViewItem.TimeGroupHeader(
                title = notRemovedItemA.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = notRemovedItemA.historyTimeGroup,
                collapsed = false,
            ),
            createHistoryViewItem(notRemovedItemA),
            createHistoryViewItem(notRemovedItemA.copy()),
            createHistoryViewItem(notRemovedItemA.copy()),
            createHistoryViewItem(notRemovedItemA.copy()),
            HistoryViewItem.TimeGroupSeparatorHistoryItem(notRemovedItemA.historyTimeGroup),

            HistoryViewItem.TimeGroupHeader(
                title = removedItemB.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = removedItemB.historyTimeGroup,
                collapsed = false,
            ),
            createHistoryViewItem(removedItemB),
            HistoryViewItem.TimeGroupSeparatorHistoryItem(removedItemB.historyTimeGroup),

            HistoryViewItem.TimeGroupHeader(
                title = notRemovedItemB.historyTimeGroup.humanReadable(testContext.resources),
                timeGroup = notRemovedItemB.historyTimeGroup,
                collapsed = false,
            ),
            createHistoryViewItem(notRemovedItemB),
            createHistoryViewItem(notRemovedItemB.copy()),
            createHistoryViewItem(notRemovedItemB.copy()),
            createHistoryViewItem(notRemovedItemB.copy()),
            HistoryViewItem.TimeGroupSeparatorHistoryItem(notRemovedItemB.historyTimeGroup),
        )

        val result = adapter.calculateTimeGroupsToRemove(
            removedItems = setOf(removedItemA, removedItemB),
            snapshot = snapshot
        )

        assert(result.contains(removedItemA.historyTimeGroup))
        assert(result.contains(removedItemB.historyTimeGroup))
    }

    @Test
    fun `WHEN adapter has a single history item THEN it is not empty`() = runTest {
        var isEmpty: Boolean? = null
        val adapter = HistoryAdapter(mockk()) {
            isEmpty = it
        }
        val data = PagingData.from(
            listOf<HistoryViewItem>(
                createHistoryViewItem(),
                createHistoryViewItem(),
                createHistoryViewItem()
            )
        )
        adapter.submitData(data)
        advanceUntilIdle()
        assert(isEmpty == false)
    }

    @Test
    fun `WHEN adapter has a single history group item THEN it is not empty`() = runTest {
        var isEmpty: Boolean? = null
        val adapter = HistoryAdapter(mockk()) {
            isEmpty = it
        }
        val data = PagingData.from(
            listOf<HistoryViewItem>(
                createGroupHistoryViewItem(),
                createGroupHistoryViewItem(),
                createGroupHistoryViewItem()
            )
        )
        adapter.submitData(data)
        advanceUntilIdle()
        assert(isEmpty == false)
    }

    @Test
    fun `WHEN adapter does not have a history or history group item THEN it is empty`() = runTest {
        var isEmpty: Boolean? = null
        val adapter = HistoryAdapter(mockk()) {
            isEmpty = it
        }
        val data = PagingData.from(
            listOf(
                HistoryViewItem.TopSeparatorHistoryItem,
                HistoryViewItem.RecentlyClosedItem("", ""),
                HistoryViewItem.SyncedHistoryItem(""),
            )
        )
        adapter.submitData(data)
        advanceUntilIdle()
        assert(isEmpty == true)
    }

    private fun createHistory(
        visitedAt: Long = 1658496038799,
    ) = History.Regular(
        position = 0,
        title = "Mozilla",
        url = "mozilla.org",
        visitedAt = visitedAt,
        historyTimeGroup = HistoryItemTimeGroup.timeGroupForTimestamp(visitedAt),
        selected = false,
    )

    private fun createHistoryViewItem(
        data: History.Regular = createHistory()
    ) = HistoryViewItem.HistoryItem(
        data = data,
        collapsed = false
    )

    private fun createGroupHistory(
        visitedAt: Long = 1658496038799,
    ) = History.Group(
        position = 0,
        title = "Mozilla",
        visitedAt = visitedAt,
        historyTimeGroup = HistoryItemTimeGroup.timeGroupForTimestamp(visitedAt),
        items = listOf(),
        selected = false,
    )

    private fun createGroupHistoryViewItem(
        data: History.Group = createGroupHistory()
    ) = HistoryViewItem.HistoryGroupItem(
        data = data,
        collapsed = false
    )
}
