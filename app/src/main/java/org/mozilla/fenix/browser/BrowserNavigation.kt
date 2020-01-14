package org.mozilla.fenix.browser

import android.annotation.SuppressLint
import androidx.annotation.IdRes
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.session.Session
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlin.toNormalizedUrl
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.DefaultBrowsingModeManager
import org.mozilla.fenix.components.UseCases
import org.mozilla.fenix.ext.alreadyOnDestination
import org.mozilla.fenix.ext.nav

interface DirectionsProvider {
    fun getNavDirections(
        from: BrowserDirection,
        customTabSessionId: String?
    ): NavDirections?
}

object BrowserNavigation {
    // these are obtained from an application context or these will be destroyed by the activity in onDestroy()
    @SuppressLint("StaticFieldLeak")
    private lateinit var useCases: UseCases
    private var navHost: NavHostFragment? = null
    private var createSessionObserver: (() -> Unit)? = null
    private var directionsProvider: DirectionsProvider? = null

    fun init(
        navHost: NavHostFragment,
        useCases: UseCases,
        directionsProvider: DirectionsProvider,
        createSessionObserver: () -> Unit
    ) {
        this.navHost = navHost
        this.useCases = useCases
        this.createSessionObserver = createSessionObserver
        this.directionsProvider = directionsProvider
    }

    fun clearData() {
        createSessionObserver = null
        navHost = null
        directionsProvider = null
    }

    @Suppress("LongParameterList")
    fun openToBrowserAndLoad(
        searchTermOrURL: String,
        newTab: Boolean,
        from: BrowserDirection,
        customTabSessionId: String? = null,
        engine: SearchEngine? = null,
        forceSearch: Boolean = false
    ) {
        if (isObjectValid()) {
            openToBrowser(from, customTabSessionId)
            load(searchTermOrURL, newTab, engine, forceSearch)
        }
    }

    fun openToBrowser(from: BrowserDirection, customTabSessionId: String? = null) {
        if (isObjectValid()) {
            createSessionObserver?.invoke()

            if (navHost?.navController?.alreadyOnDestination(R.id.browserFragment) == true) {
                return
            }

            @IdRes val fragmentId = if (from.fragmentId != 0) from.fragmentId else null
            val directions = directionsProvider!!.getNavDirections(from, customTabSessionId)
            if (directions != null) {
                navHost?.navController?.nav(fragmentId, directions)
            }
        }
    }

    internal fun load(
        searchTermOrURL: String,
        newTab: Boolean,
        engine: SearchEngine?,
        forceSearch: Boolean
    ) {
        val mode = DefaultBrowsingModeManager.mode

        val loadUrlUseCase = if (newTab) {
            when (mode) {
                BrowsingMode.Private -> useCases.tabsUseCases.addPrivateTab
                BrowsingMode.Normal -> useCases.tabsUseCases.addTab
            }
        } else useCases.sessionUseCases.loadUrl

        val searchUseCase: (String) -> Unit = { searchTerms ->
            if (newTab) {
                useCases.searchUseCases.newTabSearch
                    .invoke(
                        searchTerms,
                        Session.Source.USER_ENTERED,
                        true,
                        mode.isPrivate,
                        searchEngine = engine
                    )
            } else useCases.searchUseCases.defaultSearch.invoke(searchTerms, engine)
        }

        if (!forceSearch && searchTermOrURL.isUrl()) {
            loadUrlUseCase.invoke(searchTermOrURL.toNormalizedUrl())
        } else {
            searchUseCase.invoke(searchTermOrURL)
        }
    }

    /**
     * Make sure the object is still valid when the public methods are called
     */
    fun isObjectValid(): Boolean {
        if (navHost != null && createSessionObserver != null) {
            return true
        }

        return false
    }
}
