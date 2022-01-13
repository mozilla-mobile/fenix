/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.robolectric.fakes.RoboMenuItem

@RunWith(FenixRobolectricTestRunner::class)
class FragmentTest {

    private val navDirections: NavDirections = mockk(relaxed = true)
    private val mockDestination = spyk(NavDestination("hi"))
    private val mockExtras: Extras = mockk(relaxed = true)
    private val mockId = 4
    private val navController = spyk(NavController(testContext))
    private val mockFragment: Fragment = mockk(relaxed = true)
    private val mockOptions: NavOptions = mockk(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(NavHostFragment::class)
        every { (NavHostFragment.findNavController(mockFragment)) } returns navController
        every { (NavHostFragment.findNavController(mockFragment).currentDestination) } returns mockDestination
        every { (mockDestination.id) } returns mockId
        every { (navController.currentDestination) } returns mockDestination
        every { (NavHostFragment.findNavController(mockFragment).currentDestination?.id) } answers { (mockDestination.id) }
    }

    @Test
    fun `Test nav fun with ID and directions`() {
        every { (NavHostFragment.findNavController(mockFragment).navigate(navDirections, null)) } just Runs

        mockFragment.nav(mockId, navDirections)
        verify { (NavHostFragment.findNavController(mockFragment).currentDestination) }
        verify { (NavHostFragment.findNavController(mockFragment).navigate(navDirections, null)) }
        confirmVerified(mockFragment)
    }

    @Test
    fun `Test nav fun with ID, directions, and options`() {
        every { (NavHostFragment.findNavController(mockFragment).navigate(navDirections, mockOptions)) } just Runs

        mockFragment.nav(mockId, navDirections, mockOptions)
        verify { (NavHostFragment.findNavController(mockFragment).currentDestination) }
        verify { (NavHostFragment.findNavController(mockFragment).navigate(navDirections, mockOptions)) }
        confirmVerified(mockFragment)
    }

    @Test
    fun `GIVEN a Menu not containing a SearchView WHEN splitSearchViewItem is called THEN return a null for first value`() {
        val menuItems = listOf(RoboMenuItem(), RoboMenuItem())
        val menuItemPositionSlot = slot<Int>()
        val menu: Menu = mockk {
            every { size } returns menuItems.size
            every { getItem(capture(menuItemPositionSlot)) } answers { menuItems[menuItemPositionSlot.captured] }
        }

        val result = menu.splitSearchViewItem()

        assertNull(result.first)
        assertEquals(menuItems, result.second)
    }

    @Test
    fun `GIVEN a Menu containing a SearchView WHEN splitSearchViewItem is called THEN return the item with SearchView for first value`() {
        val actionViewMenuItem = RoboMenuItem().apply {
            actionView = mockk<SearchView>()
        }
        val otherVisibleMenuItems = listOf(RoboMenuItem(), RoboMenuItem())
        val menuItems = otherVisibleMenuItems + actionViewMenuItem
        val menuItemPositionSlot = slot<Int>()
        val menu: Menu = mockk {
            every { size } returns menuItems.size
            every { getItem(capture(menuItemPositionSlot)) } answers { menuItems[menuItemPositionSlot.captured] }
        }

        val result = menu.splitSearchViewItem()

        assertEquals(actionViewMenuItem, result.first)
        assertEquals(otherVisibleMenuItems, result.second)
    }

    @Test
    fun `Given a Menu containing both visible and invisible items WHEN splitSearchViewItem is called THEN return only the visible items for the second value`() {
        val visibleMenuItems = listOf(RoboMenuItem(), RoboMenuItem())
        val notVisibleMenuItems = listOf(
            RoboMenuItem().setVisible(false),
            RoboMenuItem().setVisible(false)
        )
        val menuItems = visibleMenuItems.zip(notVisibleMenuItems).flatMap { listOf(it.first, it.second) }
        val menuItemPositionSlot = slot<Int>()
        val menu: Menu = mockk {
            every { size } returns menuItems.size
            every { getItem(capture(menuItemPositionSlot)) } answers { menuItems[menuItemPositionSlot.captured] }
        }

        val result = menu.splitSearchViewItem()

        assertEquals(visibleMenuItems, result.second)
    }

    @Test
    fun `Given a Menu containing a SearchView WHEN configureSearchViewInMenu is called THEN properly configure the menu`() {
        val searchView = spyk(SearchView(testContext))
        val actionViewMenuItem = spyk(RoboMenuItem()).apply {
            actionView = searchView
        }
        val otherVisibleMenuItems = listOf(RoboMenuItem(), RoboMenuItem())
        val menuItems = otherVisibleMenuItems + actionViewMenuItem
        val menuItemPositionSlot = slot<Int>()
        val menu: Menu = mockk {
            every { size } returns menuItems.size
            every { getItem(capture(menuItemPositionSlot)) } answers { menuItems[menuItemPositionSlot.captured] }
        }
        var previousQuery = ""
        var queryTextChange = ""
        var queryTextSubmit = ""
        var searchStarted = false
        var searchEnded = false
        val containerActivity: FragmentActivity = mockk(relaxed = true)
        val fragment: Fragment = mockk {
            every { activity } returns containerActivity
        }

        fragment.configureSearchViewInMenu(
            menu = menu,
            queryHint = "queryHint",
            onQueryTextChange = { previous, new -> previousQuery = previous; queryTextChange = new },
            onQueryTextSubmit = { queryTextSubmit = it },
            onSearchStarted = { searchStarted = true },
            onSearchEnded = { searchEnded = true }
        )
        assertEquals("queryHint", searchView.queryHint)
        assertEquals(EditorInfo.IME_ACTION_DONE, searchView.imeOptions)
        assertEquals(Int.MAX_VALUE, searchView.maxWidth)

        val textListenerCaptor = slot<SearchView.OnQueryTextListener>()
        val actionExpandListenerCaptor = slot<MenuItem.OnActionExpandListener>()
        verify { searchView.setOnQueryTextListener(capture(textListenerCaptor)) }
        verify { actionViewMenuItem.setOnActionExpandListener(capture(actionExpandListenerCaptor)) }

        textListenerCaptor.captured.onQueryTextChange("testChange")
        assertEquals("", previousQuery)
        assertEquals("testChange", queryTextChange)

        textListenerCaptor.captured.onQueryTextChange("newTestChange")
        assertEquals("testChange", previousQuery)
        assertEquals("newTestChange", queryTextChange)

        textListenerCaptor.captured.onQueryTextSubmit("testSubmit")
        assertEquals("testSubmit", queryTextSubmit)

        actionExpandListenerCaptor.captured.onMenuItemActionExpand(actionViewMenuItem)
        otherVisibleMenuItems.forEach { assertFalse(it.isVisible) }
        assertTrue(searchStarted)

        actionExpandListenerCaptor.captured.onMenuItemActionCollapse(actionViewMenuItem)
        verify { containerActivity.invalidateOptionsMenu() }
        assertTrue(searchEnded)
    }
}
