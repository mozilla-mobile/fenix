/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import mozilla.components.browser.domains.autocomplete.DomainAutocompleteProvider
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.settings

class ToolbarIntegration(
    context: Context,
    toolbar: BrowserToolbar,
    browserLayout: ViewGroup,
    toolbarMenu: ToolbarMenu,
    domainAutocompleteProvider: DomainAutocompleteProvider,
    historyStorage: HistoryStorage,
    sessionManager: SessionManager,
    sessionId: String? = null,
    isPrivate: Boolean
) : BaseToolbarIntegration(
    context,
    toolbar,
    toolbarMenu,
    ToolbarFeature.RenderStyle.UncoloredUrl,
    sessionId,
    isPrivate,
    displaySeperator = true
) {

    init {
        val tabsAction = TabCounterToolbarButton(sessionManager, isPrivate) {
            toolbar.hideKeyboard()
            // We need to dynamically add the options here because if you do it in XML it overwrites
            val options = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, false)
                .setEnterAnim(R.anim.fade_in)
                .build()
            val extras =
                FragmentNavigator.Extras.Builder()
                    .addSharedElement(
                        browserLayout,
                        "$TAB_ITEM_TRANSITION_NAME${sessionManager.selectedSession?.id}"
                    )
                    .build()
            val navController = Navigation.findNavController(toolbar)
            if (!navController.popBackStack(R.id.homeFragment, false)) {
                navController.nav(
                    R.id.browserFragment,
                    R.id.action_browserFragment_to_homeFragment,
                    null,
                    options,
                    extras
                )
            }
        }
        toolbar.addBrowserAction(tabsAction)

        ToolbarAutocompleteFeature(toolbar).apply {
            addDomainProvider(domainAutocompleteProvider)
            if (context.settings().shouldShowHistorySuggestions) {
                addHistoryStorageProvider(historyStorage)
            }
        }
    }

    companion object {
        private const val TAB_ITEM_TRANSITION_NAME = "tab_item"
    }
}
