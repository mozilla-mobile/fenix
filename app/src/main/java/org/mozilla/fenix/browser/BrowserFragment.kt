/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.component_search.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import mozilla.components.browser.toolbar.behavior.BrowserToolbarBottomBehavior
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.ktx.android.arch.lifecycle.addObservers
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import mozilla.components.feature.prompts.PromptFeature
import org.mozilla.fenix.BackHandler
import org.mozilla.fenix.components.FindInPageIntegration
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.getSafeManagedObservable
import org.mozilla.fenix.search.toolbar.SearchAction
import org.mozilla.fenix.search.toolbar.SearchState
import org.mozilla.fenix.search.toolbar.ToolbarComponent
import org.mozilla.fenix.search.toolbar.ToolbarUIView

class BrowserFragment : Fragment(), BackHandler {

    private lateinit var contextMenuFeature: ContextMenuFeature
    private lateinit var downloadsFeature: DownloadsFeature
    private lateinit var findInPageIntegration: FindInPageIntegration
    private lateinit var promptsFeature: PromptFeature
    private lateinit var sessionFeature: SessionFeature
    private lateinit var toolbarComponent: ToolbarComponent

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_browser, container, false)
        toolbarComponent = ToolbarComponent(
            view.browserLayout,
            ActionBusFactory.get(this),
            SearchState("", isEditing = false)
        )

        toolbarComponent.uiView.view.apply {
            setBackgroundColor(ContextCompat.getColor(view.context, R.color.offwhite))

            (layoutParams as CoordinatorLayout.LayoutParams).apply {
                // Stop toolbar from collapsing if TalkBack is enabled
                val accessibilityManager = context
                    ?.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

                if (!accessibilityManager.isEnabled) {
                    behavior = BrowserToolbarBottomBehavior(view.context, null)
                }

                gravity = Gravity.BOTTOM
                height = (resources.displayMetrics.density * TOOLBAR_HEIGHT).toInt()
            }
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = requireComponents.core.sessionManager

        contextMenuFeature = ContextMenuFeature(
            requireFragmentManager(),
            sessionManager,
            ContextMenuCandidate.defaultCandidates(
                requireContext(),
                requireComponents.useCases.tabsUseCases,
                view),
            view.engineView)

        downloadsFeature = DownloadsFeature(
            requireContext(),
            sessionManager = sessionManager,
            fragmentManager = childFragmentManager,
            onNeedToRequestPermissions = { permissions ->
                requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
            }
        )

        promptsFeature = PromptFeature(
            fragment = this,
            sessionManager = sessionManager,
            fragmentManager = requireFragmentManager(),
            onNeedToRequestPermissions = { permissions ->
                requestPermissions(permissions, REQUEST_CODE_PROMPT_PERMISSIONS)
            }
        )

        sessionFeature = SessionFeature(
            sessionManager,
            SessionUseCases(sessionManager),
            view.engineView
        )

        findInPageIntegration = FindInPageIntegration(requireComponents.core.sessionManager, view.findInPageView)

        lifecycle.addObservers(
            contextMenuFeature,
            downloadsFeature,
            findInPageIntegration,
            promptsFeature,
            sessionFeature,
            (toolbarComponent.uiView as ToolbarUIView).toolbarIntegration
        )

        getSafeManagedObservable<SearchAction>()
            .subscribe {
                if (it is SearchAction.ToolbarTapped) {
                    navigateToSearch()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        lifecycle.removeObserver(sessionFeature)
    }

    override fun onBackPressed(): Boolean {
        if (findInPageIntegration.onBackPressed()) return true
        if (sessionFeature.handleBackPressed()) return true

        // We'll want to improve this when we add multitasking
        requireComponents.core.sessionManager.remove()
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.onPermissionsResult(permissions, grantResults)
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.onPermissionsResult(permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        promptsFeature.onActivityResult(requestCode, resultCode, data)
    }

    private fun navigateToSearch() {
        Navigation.findNavController(toolbar)
            .navigate(R.id.action_browserFragment_to_searchFragment, null, null)
    }

    companion object {
        private const val REQUEST_CODE_DOWNLOAD_PERMISSIONS = 1
        private const val REQUEST_CODE_PROMPT_PERMISSIONS = 2
        private const val TOOLBAR_HEIGHT = 56f
    }
}
