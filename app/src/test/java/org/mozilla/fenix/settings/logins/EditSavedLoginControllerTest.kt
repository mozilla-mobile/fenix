/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.support.test.robolectric.testContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class EditSavedLoginControllerTest {
    private val store: LoginsFragmentStore = mockk(relaxed = true)
    private val context: Context = spyk(testContext)
    private val controller = EditSavedLoginsController(context = context, loginsFragmentStore = store)

    @Test
    fun `GIVEN a list of logins, WHEN findPotentialDuplicates is called on the controller, THEN a list of possible dupes is given`() {
        val item = SavedLogin(
            guid = "itemId",
            origin = "https://cats.com",
            username = "love4cats",
            password = "666",
            timeLastUsed = 0L
        )
        GlobalScope.launch(Dispatchers.IO) { controller.findPotentialDuplicates(item) }

        verify {
            store.dispatch(
                LoginsAction.ListOfDupes(
                    listOf(item)
                )
            )
        }
    }
}
