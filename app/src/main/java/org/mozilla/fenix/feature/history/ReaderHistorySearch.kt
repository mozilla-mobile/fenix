/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.feature.history

import android.content.Context
import androidx.annotation.VisibleForTesting
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import mozilla.components.browser.state.action.ReaderAction
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.webextensions.WebExtensionController
import org.json.JSONObject

typealias onReaderViewStatusChange = (available: Boolean, active: Boolean) -> Unit

/**
 * Feature implementation that provides a reader view for the selected
 * session, based on a web extension.
 *
 * @property context a reference to the context.
 * @property engine a reference to the application's browser engine.
 * @property store a reference to the application's [BrowserStore].
 * @param controlsView the view to use to display reader mode controls.
 * @property onReaderViewStatusChange a callback invoked to indicate whether
 * or not reader view is available and active for the page loaded by the
 * currently selected session. The callback will be invoked when a page is
 * loaded or refreshed, on any navigation (back or forward), and when the
 * selected session changes.
 */
class HistorySearchFeature(
    private val context: Context,
    private val engine: Engine,
    private val store: BrowserStore,
    private val historyStorageDelegate: HistorySearchStorageDelegate
) : LifecycleAwareFeature {

    private var scope: CoroutineScope? = null

    @VisibleForTesting
    // This is an internal var to make it mutable for unit testing purposes only
    internal var extensionController = WebExtensionController(
        READER_VIEW_EXTENSION_ID,
        READER_VIEW_EXTENSION_URL,
        READER_VIEW_CONTENT_PORT
    )


    override fun start() {
        ensureExtensionInstalled()
    }

    override fun stop() {
        scope?.cancel()
    }


    @VisibleForTesting
    internal fun checkReaderState(session: TabSessionState? = store.state.selectedTab) {
        session?.engineState?.engineSession?.let { engineSession ->
            val message = createCheckReaderStateMessage()
            if (extensionController.portConnected(engineSession, READER_VIEW_CONTENT_PORT)) {
                extensionController.sendContentMessage(message, engineSession, READER_VIEW_CONTENT_PORT)
            }
            if (extensionController.portConnected(engineSession, READER_VIEW_ACTIVE_CONTENT_PORT)) {
                extensionController.sendContentMessage(message, engineSession, READER_VIEW_ACTIVE_CONTENT_PORT)
            }
            store.dispatch(ReaderAction.UpdateReaderableCheckRequiredAction(session.id, false))
        }
    }



    private fun ensureExtensionInstalled() {
        val feature = WeakReference(this)
        extensionController.install(engine,onSuccess = {
            feature.get()?.connectReaderViewContentScript()
        })
    }

    @VisibleForTesting
    internal fun connectReaderViewContentScript(session: TabSessionState? = store.state.selectedTab) {

        session?.engineState?.engineSession?.let { engineSession ->
            extensionController.registerContentMessageHandler(
                engineSession,
                ReaderViewContentMessageHandler(historyStorageDelegate),
                READER_VIEW_CONTENT_PORT
            )
            store.dispatch(ReaderAction.UpdateReaderConnectRequiredAction(session.id, false))
        }
    }
    /**
     * Handles content messages from regular pages.
     */
    private open class ReaderViewContentMessageHandler(
        protected val storageDelegate: HistorySearchStorageDelegate,
    ) : MessageHandler {
        override fun onPortConnected(port: Port) {
            port.postMessage(createCheckReaderStateMessage())
        }

        override fun onPortMessage(message: Any, port: Port) {
            if (message is JSONObject) {
                if(message.has("textContent")){
                    val pageContent = message.optString("textContent")
                    val pageUrl = message.optString("url")
                    if (pageContent.isNullOrEmpty()) {
                        return
                    }

                    storageDelegate.store(pageUrl, pageContent)
                    println("pageUrl: $pageUrl")
                    println(pageContent)
                }
            }
        }
    }

    @VisibleForTesting
    companion object {
        private val logger = Logger("ReaderView")

        internal const val READER_VIEW_EXTENSION_ID = "historySearch@mozac.org"
        // Name of the port connected to all pages for checking whether or not
        // a page is readerable (see readerview_content.js).
        internal const val READER_VIEW_CONTENT_PORT = "mozacReaderview"
        // Name of the port connected to active reader pages for updating
        // appearance configuration (see readerview.js).
        internal const val READER_VIEW_ACTIVE_CONTENT_PORT = "mozacReaderviewActive"
        internal const val READER_VIEW_EXTENSION_URL = "resource://android/assets/extensions/historySearch/"

        // Constants for building messages sent to the web extension:
        // Change the font type: {"action": "setFontType", "value": "sans-serif"}
        // Show reader view: {"action": "show", "value": {"fontSize": 3, "fontType": "serif", "colorScheme": "dark"}}
        internal const val ACTION_MESSAGE_KEY = "action"
        internal const val ACTION_SHOW = "show"
        internal const val ACTION_HIDE = "hide"
        internal const val ACTION_CHECK_READER_STATE = "checkReaderState"
        internal const val ACTION_SET_COLOR_SCHEME = "setColorScheme"
        internal const val ACTION_CHANGE_FONT_SIZE = "changeFontSize"
        internal const val ACTION_SET_FONT_TYPE = "setFontType"
        internal const val ACTION_VALUE = "value"
        internal const val ACTION_VALUE_SHOW_FONT_SIZE = "fontSize"
        internal const val ACTION_VALUE_SHOW_FONT_TYPE = "fontType"
        internal const val ACTION_VALUE_SHOW_COLOR_SCHEME = "colorScheme"
        internal const val READERABLE_RESPONSE_MESSAGE_KEY = "readerable"
        internal const val BASE_URL_RESPONSE_MESSAGE_KEY = "baseUrl"
        internal const val ACTIVE_URL_RESPONSE_MESSAGE_KEY = "activeUrl"

        // Constants for storing the reader mode config in shared preferences
        internal const val SHARED_PREF_NAME = "mozac_feature_reader_view"
        internal const val COLOR_SCHEME_KEY = "mozac-readerview-colorscheme"
        internal const val FONT_TYPE_KEY = "mozac-readerview-fonttype"
        internal const val FONT_SIZE_KEY = "mozac-readerview-fontsize"
        internal const val FONT_SIZE_DEFAULT = 3

        internal fun createCheckReaderStateMessage(): JSONObject {
            return JSONObject().put(ACTION_MESSAGE_KEY, ACTION_CHECK_READER_STATE)
        }


        internal fun createHideReaderMessage(): JSONObject {
            return JSONObject().put(ACTION_MESSAGE_KEY, ACTION_HIDE)
        }
    }
}

interface HistorySearchStorageDelegate {
    fun store(url: String, content: String)
}
