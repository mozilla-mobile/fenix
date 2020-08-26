/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class FragmentTest {

    @MockK private lateinit var navDirections: NavDirections
    @MockK private lateinit var mockFragment: Fragment
    @MockK private lateinit var mockOptions: NavOptions
    @MockK private lateinit var mockDestination: NavDestination
    @MockK private lateinit var navController: NavController
    private val mockId = 4

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(NavHostFragment::class)

        every { NavHostFragment.findNavController(mockFragment) } returns navController
        every { navController.currentDestination } returns mockDestination
        every { mockDestination.id } returns mockId
    }

    @Test
    fun `Test nav fun with ID and directions`() {
        every { navController.navigate(navDirections, null) } just Runs

        mockFragment.nav(mockId, navDirections)
        verify { navController.currentDestination }
        verify { navController.navigate(navDirections, null) }
        confirmVerified(mockFragment)
    }

    @Test
    fun `Test nav fun with ID, directions, and options`() {
        every { navController.navigate(navDirections, mockOptions) } just Runs

        mockFragment.nav(mockId, navDirections, mockOptions)
        verify { navController.currentDestination }
        verify { navController.navigate(navDirections, mockOptions) }
        confirmVerified(mockFragment)
    }

    @Test
    fun `hide fragment toolbar`() {
        val actionBar = mockk<ActionBar>(relaxUnitFun = true)
        val activity = mockk<AppCompatActivity> {
            every { supportActionBar } returns actionBar
        }
        every { mockFragment.requireActivity() } returns activity

        mockFragment.hideToolbar()

        verify { actionBar.hide() }
    }
}
