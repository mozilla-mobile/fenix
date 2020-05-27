/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.utils.Settings

@RunWith(FenixRobolectricTestRunner::class)
class DefaultSavedLoginsControllerTest {
    private val store: LoginsFragmentStore = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)
    private val sortingStrategy: SortingStrategy = SortingStrategy.Alphabetically(testContext)
    private val context: Context = spyk(testContext)
    private val controller = DefaultSavedLoginsController(context = context, loginsFragmentStore = store, settings = settings)

    @Test
    fun `GIVEN a sorting strategy, WHEN handleSort is called on the controller, THEN the correct action should be dispatched and the strategy saved in sharedPref`() {
        controller.handleSort(sortingStrategy)

        verify {
            store.dispatch(
                LoginsAction.SortLogins(
                    SortingStrategy.Alphabetically(
                        testContext
                    )
                )
            )
            settings.savedLoginsSortingStrategy = sortingStrategy
        }
    }
}
