/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.toolbar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import kotlinx.android.extensions.LayoutContainer
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.support.ktx.android.content.res.pxToDp
import org.mozilla.fenix.R
import org.mozilla.fenix.search.SearchState

interface ToolbarInteractor {
    fun onUrlCommitted(url: String)
    fun onEditingCanceled()
    fun onTextChanged(text: String)
}

class ToolbarView(
    private val container: ViewGroup,
    private val interactor: ToolbarInteractor,
    private val historyStorageProvider: () -> HistoryStorage?
) : LayoutContainer {

    override val containerView: View?
        get() = container

    val view: BrowserToolbar = LayoutInflater.from(container.context)
        .inflate(R.layout.component_search, container, true)
        .findViewById(R.id.toolbar)

    private var isInitialzied = false

    init {
        view.apply {
            editMode()

            elevation = resources.pxToDp(TOOLBAR_ELEVATION).toFloat()

            setOnUrlCommitListener {
                interactor.onUrlCommitted(it)
                false
            }

            background = null

            textColor = ContextCompat.getColor(context, R.color.photonGrey30)

            layoutParams.height = CoordinatorLayout.LayoutParams.MATCH_PARENT

            hint = context.getString(R.string.search_hint)

            setOnEditListener(object : mozilla.components.concept.toolbar.Toolbar.OnEditListener {
                override fun onCancelEditing(): Boolean {
                    interactor.onEditingCanceled()
                    return false
                }
                override fun onTextChanged(text: String) {
                    url = text
                    this@ToolbarView.interactor.onTextChanged(text)
                }
            })
        }

        ToolbarAutocompleteFeature(view).apply {
            addDomainProvider(ShippedDomainsProvider().also { it.initialize(view.context) })
            historyStorageProvider()?.also(::addHistoryStorageProvider)
        }
    }

    fun update(searchState: SearchState) {
        if (!isInitialzied) {
            view.url = searchState.query
            view.setSearchTerms(searchState.session?.searchTerms ?: "")
            view.editMode()
            isInitialzied = true
        }
    }

    companion object {
        private const val TOOLBAR_ELEVATION = 16
    }
}
