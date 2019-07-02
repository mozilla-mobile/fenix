/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.toolbar

import android.content.Context
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import mozilla.components.browser.domains.autocomplete.DomainAutocompleteProvider
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.runWithSession
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.feature.toolbar.ToolbarPresenter
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.android.view.hideKeyboard
import org.mozilla.fenix.R
import org.mozilla.fenix.ThemeManager
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.utils.Settings

class ToolbarIntegration(
    context: Context,
    toolbar: BrowserToolbar,
    domainAutocompleteProvider: DomainAutocompleteProvider,
    historyStorage: HistoryStorage,
    sessionId: String? = null
) : LifecycleAwareFeature {

    private var renderStyle: ToolbarFeature.RenderStyle = ToolbarFeature.RenderStyle.UncoloredUrl

    init {
        ToolbarAutocompleteFeature(toolbar).apply {
            addDomainProvider(domainAutocompleteProvider)
            if (Settings.getInstance(context).shouldShowVisitedSitesBookmarks) {
                addHistoryStorageProvider(historyStorage)
            }
        }
    }

    private val toolbarPresenter: ToolbarPresenter = ToolbarPresenter(
        toolbar,
        context.components.core.sessionManager,
        sessionId,
        ToolbarFeature.UrlRenderConfiguration(
            PublicSuffixList(context),
            ThemeManager.resolveAttribute(R.attr.primaryText, context), renderStyle = renderStyle
        )
    )

    override fun start() {
        toolbarPresenter.start()
    }

    override fun stop() {
        toolbarPresenter.stop()
    }
}
