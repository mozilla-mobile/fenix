/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.fenix.collections.CollectionCreationController
import org.mozilla.fenix.collections.CollectionCreationInteractor
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.metrics
import org.json.JSONObject
import org.mozilla.fenix.collections.CollectionCreationFragment
import org.mozilla.fenix.collections.DefaultCollectionCreationInteractor
import org.mozilla.gecko.process.GeckoChildProcessServices

class UriOpenedObserver(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val sessionManager: SessionManager,
    private val metrics: MetricController
) : SessionManager.Observer {

    constructor(activity: FragmentActivity) : this(
        activity,
        activity,
        activity.components.core.sessionManager,
        activity.metrics
    )

    init {
        sessionManager.register(this, owner)
    }

    /**
     * Currently, [Session.Observer.onLoadingStateChanged] is called multiple times the first
     * time a new session loads a page. This is inflating our telemetry numbers, so we need to
     * handle it, but we will be able to remove this code when [onLoadingStateChanged] has
     * been fixed.
     *
     * See Fenix #3676
     * See AC https://github.com/mozilla-mobile/android-components/issues/4795
     * TODO remove this class after AC #4795 has been fixed
     */
    private class TemporaryFix {
        var eventSentFor: String? = null

        fun shouldSendEvent(newUrl: String): Boolean = eventSentFor != newUrl
    }

    @VisibleForTesting
    internal val singleSessionObserver = object : Session.Observer {
        private var urlLoading: String? = null

        private val temporaryFix = TemporaryFix()

        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            if (loading) {
                urlLoading = session.url
            } else if (urlLoading != null && !session.private && temporaryFix.shouldSendEvent(session.url)) {
                temporaryFix.eventSentFor = session.url
                // Innovation Week Work
                Log.d("HELLO", session.url)
                if (session.url.contains("google.com/search?q=", true)) {
                    // Innovation Code
                    // Innovation week code here
                    // Grab search term
                    val searchTerm = session.url.substringAfter("google.com/search?q=").substringBefore("&")
                    val queue = Volley.newRequestQueue(context)
                    val my_url = "https://www.googleapis.com/customsearch/v1?key=AIzaSyCScTTFWejTEPld0vrnE2tLhNnyuN9djNg&cx=003273877844097805647:pvopecflb5y&q="+searchTerm
                    val stringRequest = JsonObjectRequest(
                        Request.Method.GET, my_url,null,
                        Response.Listener { response ->
                            // Display the first 500 characters of the response string.
                            val my_string = "${response}"
                            // JSON Stuff
                            val root = JSONObject(my_string)
                            val results = root.getJSONArray("items")
                            for (i in 0 until 10) {
                                val urlTitle = JSONObject(results[i].toString()).get("link").toString()
                                Log.d("HELLO", urlTitle)
                                context.components.useCases.tabsUseCases.addTab.invoke(urlTitle, false)
                            }
                            Thread{context.components.core.tabCollectionStorage.createCollection(searchTerm, sessionManager.sessions) }.start()
                        },
                        Response.ErrorListener {val my_string = "That didn't work!"
                            Log.d("HELLO", my_string)})
                    queue.add(stringRequest)
                }
                metrics.track(Event.UriOpened)
            }
        }
    }

    override fun onAllSessionsRemoved() {
        sessionManager.sessions.forEach {
            it.unregister(singleSessionObserver)
        }
    }

    override fun onSessionAdded(session: Session) {
        session.register(singleSessionObserver, owner)
    }

    override fun onSessionRemoved(session: Session) {
        session.unregister(singleSessionObserver)
    }

    override fun onSessionsRestored() {
        sessionManager.sessions.forEach {
            it.register(singleSessionObserver, owner)
        }
    }
}
