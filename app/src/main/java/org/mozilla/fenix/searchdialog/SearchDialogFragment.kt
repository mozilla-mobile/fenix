/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.searchdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.fragment_search.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.logDebug
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.search.toolbar.ToolbarInteractor
import org.mozilla.fenix.search.toolbar.ToolbarView

class TempSearchInteractor : ToolbarInteractor {
    override fun onUrlCommitted(url: String) {
        logDebug("boek", "onUrlCommitted $url")
    }

    override fun onEditingCanceled() {
        logDebug("boek", "onEditingCanceled")
    }

    override fun onTextChanged(text: String) {
        logDebug("boek", "onTextChanged $text")
    }
}

class SearchDialogFragment : AppCompatDialogFragment() {

    private lateinit var toolbarView: ToolbarView
    private val tempInteractor = TempSearchInteractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.SearchDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_dialog, container, false)

        toolbarView = ToolbarView(
            requireContext(),
            tempInteractor,
            null,
            false,
            view.toolbar,
            requireComponents.core.engine
        )

        return view
    }
}
