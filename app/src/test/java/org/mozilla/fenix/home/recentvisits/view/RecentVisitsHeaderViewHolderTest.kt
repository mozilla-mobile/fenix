/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentvisits.view

import android.view.LayoutInflater
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.databinding.RecentVisitsHeaderBinding
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor

@RunWith(FenixRobolectricTestRunner::class)
class RecentVisitsHeaderViewHolderTest {

    private lateinit var binding: RecentVisitsHeaderBinding
    private lateinit var interactor: SessionControlInteractor

    @Before
    fun setup() {
        binding = RecentVisitsHeaderBinding.inflate(LayoutInflater.from(testContext))
        interactor = mockk(relaxed = true)
    }

    @Test
    fun `WHEN show all button is clicked THEN interactor is called`() {
        RecentVisitsHeaderViewHolder(binding.root, interactor)

        binding.showAllButton.performClick()

        verify { interactor.onHistoryShowAllClicked() }
    }
}
