/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_add_on_internal_settings.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar

/**
 * A fragment to show the web extension action popup with [EngineView].
 */
class WebExtensionActionPopupFragment : Fragment(), EngineSession.Observer {
    private val webExtensionTitle: String? by lazy {
        WebExtensionActionPopupFragmentArgs.fromBundle(requireNotNull(arguments)).webExtensionTitle
    }
    private val webExtensionId: String by lazy {
        WebExtensionActionPopupFragmentArgs.fromBundle(requireNotNull(arguments)).webExtensionId
    }
    private var engineSession: EngineSession? = null
    private val coreComponents by lazy { requireComponents.core }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Grab the [EngineSession] from the store when the view is created if it is available.
        if (engineSession == null) {
            engineSession = coreComponents.store.state.extensions[webExtensionId]?.popupSession
        }

        return inflater.inflate(R.layout.fragment_add_on_internal_settings, container, false)
    }

    override fun onResume() {
        super.onResume()
        val title = webExtensionTitle ?: webExtensionId
        showToolbar(title)
    }

    override fun onStart() {
        super.onStart()
        engineSession?.register(this)
    }

    override fun onStop() {
        super.onStop()
        engineSession?.unregister(this)
    }

    override fun onWindowRequest(windowRequest: WindowRequest) {
        if (windowRequest.type == WindowRequest.Type.CLOSE) {
            activity?.onBackPressed()
        }
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = engineSession
        // If we have the session, render it otherwise consume it from the store.
        if (session != null) {
            addonSettingsEngineView.render(session)
            consumePopupSession()
        } else {
            consumeFrom(coreComponents.store) { state ->
                state.extensions[webExtensionId]?.let { extState ->
                    extState.popupSession?.let {
                        if (engineSession == null) {
                            addonSettingsEngineView.render(it)
                            it.register(this)
                            consumePopupSession()
                            engineSession = it
                        }
                    }
                }
            }
        }
    }

    private fun consumePopupSession() {
        coreComponents.store.dispatch(
            WebExtensionAction.UpdatePopupSessionAction(webExtensionId, popupSession = null)
        )
    }
}
