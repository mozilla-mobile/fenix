/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator.Extras
import androidx.navigation.fragment.NavHostFragment
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mozilla.components.support.test.robolectric.testContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ObsoleteCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)

class FragmentTest {

    val navDirections: NavDirections = mockk(relaxed = true)
    val mockDestination = spyk(NavDestination("hi"))
    val mockExtras: Extras = mockk(relaxed = true)
    val mockId = 4
    val navController = spyk(NavController(testContext))
    val mockFragment: Fragment = mockk(relaxed = true)
    val mockOptions: NavOptions = mockk(relaxed = true)

    @Test
    fun `Test nav fun with ID and directions`() {
        mockkStatic(NavHostFragment::class)
        every { (NavHostFragment.findNavController(mockFragment)) } returns navController
        every { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()) } returns mockDestination
        every { (NavHostFragment.findNavController(mockFragment).navigate(navDirections)) } just Runs
        every { (mockDestination.getId()) } returns mockId
        every { (navController.getCurrentDestination()) } returns mockDestination
        every { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()?.getId()) } answers { (mockDestination.getId()) }

        mockFragment.nav(mockId, navDirections)
        verify { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()) }
        verify { (NavHostFragment.findNavController(mockFragment).navigate(navDirections)) }
        confirmVerified(mockFragment)
    }

    @Test
    fun `Test nav fun with ID, directions, and extras`() {
        mockkStatic(NavHostFragment::class)
        every { (NavHostFragment.findNavController(mockFragment)) } returns navController
        every { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()) } returns mockDestination
        every { (NavHostFragment.findNavController(mockFragment).navigate(navDirections, mockExtras)) } just Runs
        every { (mockDestination.getId()) } returns mockId
        every { (navController.getCurrentDestination()) } returns mockDestination
        every { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()?.getId()) } answers { (mockDestination.getId()) }

        mockFragment.nav(mockId, navDirections, mockExtras)
        verify { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()) }
        verify { (NavHostFragment.findNavController(mockFragment).navigate(navDirections, mockExtras)) }
        confirmVerified(mockFragment)
    }

    @Test
    fun `Test nav fun with ID, directions, and options`() {
        mockkStatic(NavHostFragment::class)
        every { (NavHostFragment.findNavController(mockFragment)) } returns navController
        every { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()) } returns mockDestination
        every { (NavHostFragment.findNavController(mockFragment).navigate(navDirections, mockOptions)) } just Runs
        every { (mockDestination.getId()) } returns mockId
        every { (navController.getCurrentDestination()) } returns mockDestination
        every { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()?.getId()) } answers { (mockDestination.getId()) }

        mockFragment.nav(mockId, navDirections, mockOptions)
        verify { (NavHostFragment.findNavController(mockFragment).getCurrentDestination()) }
        verify { (NavHostFragment.findNavController(mockFragment).navigate(navDirections, mockOptions)) }
        confirmVerified(mockFragment)
    }
}
