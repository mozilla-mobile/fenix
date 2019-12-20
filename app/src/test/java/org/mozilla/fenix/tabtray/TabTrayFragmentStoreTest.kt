/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import kotlinx.coroutines.runBlocking
import mozilla.components.feature.media.state.MediaState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TabTrayFragmentStoreTest {

    @Test
    fun UpdateTabs() = runBlocking {
        val initialState = emptyDefaultState()
        val store = TabTrayFragmentStore(initialState)
        val tabs = listOf<Tab>(
            createTabWithId("1"),
            createTabWithId("2")
        )

        store.dispatch(TabTrayFragmentAction.UpdateTabs(tabs = tabs)).join()
        assertNotSame(initialState, store.state)
    }

    @Test
    fun SelectTab() = runBlocking {
        val initialState = emptyDefaultState()
        val store = TabTrayFragmentStore(initialState)
        val idToSelect = "2"
        val tabToSelect = createTabWithId(idToSelect)
        val tabs = listOf<Tab>(
            createTabWithId("1"),
            createTabWithId(idToSelect),
            createTabWithId("3")
        )

        store.dispatch(TabTrayFragmentAction.UpdateTabs(tabs)).join()
        store.dispatch(TabTrayFragmentAction.SelectTab(tab = tabToSelect)).join()

        val selected = store.state.mode.selectedTabs
        assertEquals(selected.toList().size, 1)
        assertEquals(selected.first().sessionId, idToSelect)
    }

    @Test
    fun DeselectTab() = runBlocking {
        val initialState = emptyDefaultState()
        val store = TabTrayFragmentStore(initialState)
        val idToSelect = "2"
        val tabToSelect = createTabWithId(idToSelect)
        val tabs = listOf<Tab>(
            createTabWithId("1"),
            createTabWithId("3")
        )

        store.dispatch(TabTrayFragmentAction.UpdateTabs(tabs)).join()
        store.dispatch(TabTrayFragmentAction.SelectTab(tab = tabToSelect)).join()
        store.dispatch(TabTrayFragmentAction.DeselectTab(tab = tabToSelect)).join()

        val selected = store.state.mode.selectedTabs
        assertEquals(selected.toList().size, 0)
    }

    @Test
    fun ExitEditMode() = runBlocking {
        val initialState = emptyEditState()
        val store = TabTrayFragmentStore(initialState)

        store.dispatch(TabTrayFragmentAction.ExitEditMode).join()
        assertFalse(store.state.mode.isEditing)
    }

    @Test
    fun EnterEditMode() = runBlocking {
        val initialState = emptyEditState()
        val store = TabTrayFragmentStore(initialState)

        store.dispatch(TabTrayFragmentAction.EnterEditMode).join()
        assertTrue(store.state.mode.isEditing)
    }

    private fun emptyDefaultState(): TabTrayFragmentState = TabTrayFragmentState(
        tabs = listOf(),
        mode = TabTrayFragmentState.Mode.Normal
    )

    private fun emptyEditState(): TabTrayFragmentState = TabTrayFragmentState(
        tabs = listOf(),
        mode = TabTrayFragmentState.Mode.Editing(setOf())
    )

    private fun createTabWithId(id: String): Tab {
        return Tab(id, "", "", "", false, MediaState.None, null)
    }
}
