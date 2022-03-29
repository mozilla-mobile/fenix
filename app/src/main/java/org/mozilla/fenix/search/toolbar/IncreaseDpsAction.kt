package org.mozilla.fenix.search.toolbar

import android.view.View
import mozilla.components.concept.toolbar.Toolbar
import org.mozilla.fenix.ext.increaseTapArea

class IncreaseDpsAction(
    private val action: Toolbar.Action
) : Toolbar.Action by action {

    override fun bind(view: View) {
        action.bind(view)
        view.increaseTapArea(TAP_INCREASE_DPS)
    }

    companion object {
        private const val TAP_INCREASE_DPS = 8
    }

}