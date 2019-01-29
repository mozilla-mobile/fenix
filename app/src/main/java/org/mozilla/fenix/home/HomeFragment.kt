/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import kotlinx.android.synthetic.main.fragment_home.*
import org.mozilla.fenix.R
import org.mozilla.fenix.home.sessions.SessionsComponent
import org.mozilla.fenix.home.sessions.layoutComponents
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.ext.requireComponents
import kotlin.math.roundToInt


class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchIcon = requireComponents.search.searchEngineManager.getDefaultSearchEngine(requireContext()).let {
            BitmapDrawable(resources, it.icon)
        }

        toolbar.setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null)
        toolbar.compoundDrawablePadding = (12f * Resources.getSystem().displayMetrics.density).roundToInt()
        toolbar.setOnClickListener { it ->
            val extras = FragmentNavigator.Extras.Builder().addSharedElement(
                toolbar, ViewCompat.getTransitionName(toolbar)!!
            ).build()
            Navigation.findNavController(it).navigate(R.id.action_homeFragment_to_searchFragment, null, null, extras)
        }

        SessionsComponent(homeLayout, ActionBusFactory.get(this)).setup()
        layoutComponents(homeLayout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.fade)
        exitTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.fade)
    }
}
