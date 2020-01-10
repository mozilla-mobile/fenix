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
        val tabs = listOf(
            createTabWithId("1"),
            createTabWithId("2")
        )

        store.dispatch(TabTrayFragmentAction.UpdateTabs(tabs = tabs)).join()
        assertNotSame(initialState, store.state)
    }

    private fun emptyDefaultState(): TabTrayFragmentState = TabTrayFragmentState(
        tabs = listOf()
    )

    private fun createTabWithId(id: String): Tab {
        return Tab(id, "", "", "", false, MediaState.None, null)
    }
}
