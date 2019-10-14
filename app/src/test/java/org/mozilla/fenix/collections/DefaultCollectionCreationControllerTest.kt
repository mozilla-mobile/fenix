package org.mozilla.fenix.collections

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.components.Analytics
import org.mozilla.fenix.components.TabCollectionStorage

@ExperimentalCoroutinesApi
class DefaultCollectionCreationControllerTest {

    private val testCoroutineScope = TestCoroutineScope()

    private lateinit var controller: DefaultCollectionCreationController

    @MockK private lateinit var store: CollectionCreationStore
    @MockK(relaxed = true) private lateinit var dismiss: () -> Unit
    @MockK(relaxed = true) private lateinit var analytics: Analytics
    @MockK private lateinit var tabCollectionStorage: TabCollectionStorage
    @MockK private lateinit var tabsUseCases: TabsUseCases
    @MockK private lateinit var sessionManager: SessionManager
    @MockK private lateinit var state: CollectionCreationState

    @Before
    fun before() {
        MockKAnnotations.init(this)

        every { state.previousFragmentId } returns 0
        every { store.state } returns state

        controller = DefaultCollectionCreationController(store, dismiss, analytics,
            tabCollectionStorage, tabsUseCases, sessionManager, testCoroutineScope)
    }

    @Test
    fun `GIVEN previous step was SelectTabs or RenameCollection WHEN stepBack is called THEN null should be returned`() {
        assertNull(controller.stepBack(SaveCollectionStep.SelectTabs))
        assertNull(controller.stepBack(SaveCollectionStep.RenameCollection))
    }

    @Test
    fun `GIVEN previous step was SelectCollection AND more than one tab is open WHEN stepBack is called THEN SelectTabs should be returned`() {
        every { state.tabs } returns listOf(mockk(), mockk())

        assertEquals(SaveCollectionStep.SelectTabs, controller.stepBack(SaveCollectionStep.SelectCollection))
    }

    @Test
    fun `GIVEN previous step was SelectCollection AND one or fewer tabs are open WHEN stepbback is called THEN null should be returned`() {
        every { state.tabs } returns listOf(mockk())
        assertNull(controller.stepBack(SaveCollectionStep.SelectCollection))

        every { state.tabs } returns emptyList()
        assertNull(controller.stepBack(SaveCollectionStep.SelectCollection))
    }

    @Test
    fun `GIVEN previous step was NameCollection AND tabCollections is empty AND more than one tab is open WHEN stepBack is called THEN SelectTabs should be returned`() {
        every { state.tabCollections } returns emptyList()
        every { state.tabs } returns listOf(mockk(), mockk())

        assertEquals(SaveCollectionStep.SelectTabs, controller.stepBack(SaveCollectionStep.NameCollection))
    }

    @Test
    fun `GIVEN previous step was NameCollection AND tabCollections is empty AND one or fewer tabs are open WHEN stepBack is called THEN null should be returned`() {
        every { state.tabCollections } returns emptyList()
        every { state.tabs } returns listOf(mockk())
        assertNull(controller.stepBack(SaveCollectionStep.NameCollection))

        every { state.tabCollections } returns emptyList()
        every { state.tabs } returns emptyList()
        assertNull(controller.stepBack(SaveCollectionStep.NameCollection))
    }

    @Test
    fun `GIVEN previous step was NameCollection AND tabCollections is not empty WHEN stepBack is called THEN SelectCollection should be returned`() {
        every { state.tabCollections } returns listOf(mockk())

        assertEquals(SaveCollectionStep.SelectCollection, controller.stepBack(SaveCollectionStep.NameCollection))
    }
}
