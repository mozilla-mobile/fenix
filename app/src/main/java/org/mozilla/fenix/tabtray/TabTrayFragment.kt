/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R

@SuppressWarnings("TooManyFunctions", "LargeClass")
class TabTrayFragment : Fragment() {

    var tabTrayMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateTitle()

        // More
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tab_tray, container, false)

        updateTitle()

        return view
    }

    override fun onResume() {
        super.onResume()
        updateTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tab_tray_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        this.tabTrayMenu = menu
        updateMenuItems()
    }

    private fun updateTitle() {
        // TODO:  Why isn't this displaying?
        activity?.title = requireContext().getString(R.string.tab_header_label)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    private fun updateMenuItems() {
        val inPrivateMode = (activity as HomeActivity).browsingModeManager.mode.isPrivate

        // Hide all icons when in selection mode with nothing selected

        // TODO:  Use store
        val showAnyOverflowIcons = true // tabTrayStore.state.tabs.isNotEmpty()
        this.tabTrayMenu?.findItem(R.id.tab_tray_select_to_save_menu_item)?.isVisible =
            showAnyOverflowIcons && !inPrivateMode
        this.tabTrayMenu?.findItem(R.id.tab_tray_share_menu_item)?.isVisible = showAnyOverflowIcons
        this.tabTrayMenu?.findItem(R.id.tab_tray_close_menu_item)?.isVisible = showAnyOverflowIcons
    }
}