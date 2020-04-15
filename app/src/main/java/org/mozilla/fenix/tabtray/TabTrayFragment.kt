/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mozilla.components.feature.tabs.tabstray.TabsFeature
import kotlinx.android.synthetic.main.fragment_tab_tray.tabsTray
import kotlinx.android.synthetic.main.fragment_tab_tray.view.*
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import androidx.navigation.Navigation.findNavController
import org.mozilla.fenix.ext.showToolbar

@SuppressWarnings("TooManyFunctions", "LargeClass")
class TabTrayFragment : Fragment(), UserInteractionHandler {
    private var tabsFeature: TabsFeature? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_tab_tray, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showToolbar("Open Tabs")

        tabsFeature = TabsFeature(
            tabsTray,
            requireComponents.core.store,
            requireComponents.useCases.tabsUseCases,
            { !it.content.private },
            ::closeTabsTray)

        view.tab_tray_open_new_tab.setOnClickListener {
            val directions = TabTrayFragmentDirections.actionGlobalSearch(null)
            val navController = findNavController(it)
            navController.navigate(directions)
        }

        view.tab_tray_go_home.setOnClickListener {
            val directions = TabTrayFragmentDirections.actionGlobalHome()
            val navController = findNavController(it)
            navController.navigate(directions)
        }

        view.private_browsing_button.setOnClickListener {

        }
    }

    override fun onStart() {
        super.onStart()

        tabsFeature?.start()
    }

    override fun onStop() {
        super.onStop()

        tabsFeature?.stop()
    }

    override fun onBackPressed(): Boolean {
        closeTabsTray()
        return true
    }

    private fun closeTabsTray() {
        activity?.supportFragmentManager?.beginTransaction()?.apply {
            //replace(R.id.container, BrowserFragment.create())
            commit()
        }
    }
}