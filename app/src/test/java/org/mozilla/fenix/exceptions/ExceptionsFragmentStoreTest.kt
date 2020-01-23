/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.exceptions

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class ExceptionsFragmentStoreTest {
    @Test
    fun onChange() = runBlocking {
        val initialState = emptyDefaultState()
        val store = ExceptionsFragmentStore(initialState)
        val newExceptionsItem = ExceptionItem("URL")

        store.dispatch(ExceptionsFragmentAction.Change(listOf(newExceptionsItem))).join()
        assertNotSame(initialState, store.state)
        assertEquals(
            store.state.items,
            listOf(newExceptionsItem)
        )
    }

    private fun emptyDefaultState(): ExceptionsFragmentState = ExceptionsFragmentState(
        items = listOf()
    )
}
