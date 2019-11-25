package org.mozilla.fenix.tabtray

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.logDebug

class TabTrayView(val container: ViewGroup) : LayoutContainer {

    override val containerView: View?
        get() = container

    val view: View = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tab_tray, container, true)

    fun update(state: TabTrayFragmentState) {
        logDebug("boek", state.toString())
    }
}