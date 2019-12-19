/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import mozilla.components.feature.media.state.MediaState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Test

class TabTrayFragmentStoreTest {

    @Test
    fun UpdateTabs() {
        val initialState = emptyDefaultState()
        val store = TabTrayFragmentStore(initialState)
        val tabs = listOf<Tab>(
            createTabWithId("1"),
            createTabWithId("2")
        )

        store.dispatch(TabTrayFragmentAction.UpdateTabs(tabs=tabs))
        assertNotSame(initialState, store.state)
    }

    @Test
    fun SelectTab() {
        val initialState = emptyDefaultState()
        val store = TabTrayFragmentStore(initialState)
        val tabToSelect = createTabWithId("2")
        val tabs = listOf<Tab>(
            createTabWithId("1"),
            tabToSelect
        )

        store.dispatch(TabTrayFragmentAction.UpdateTabs(tabs))
        store.dispatch(TabTrayFragmentAction.SelectTab(tab=tabToSelect))

        //val secondTab = store.state.tabs.find { it.sessionId == tabToSelect.sessionId }
        //assertTrue(secondTab.selected)
    }

    @Test
    fun DeselectTab() {

    }

    @Test
    fun ExitEditMode() {
        val initialState = emptyEditState()
        val store = TabTrayFragmentStore(initialState)

        store.dispatch(TabTrayFragmentAction.ExitEditMode)
        assertFalse(store.state.mode.isEditing)
    }

    @Test
    fun EnterEditMode() {

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
