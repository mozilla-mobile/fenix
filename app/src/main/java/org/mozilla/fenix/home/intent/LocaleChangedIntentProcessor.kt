/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.navOptions
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.nav

/**
 * When the system locale is changed, Fenix should update its locale.
 */
class LocaleChangedIntentProcessor(
    private val metrics: MetricController
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        // IntentFilter(Intent.ACTION_LOCALE_CHANGED)
        if(intent.action is Intent.FilterComparison) {

        }
        val event = intent.extras?.getString(HomeActivity.OPEN_TO_SEARCH)
        return if (event != null) {
            val source = when (event) {
                SEARCH_WIDGET -> {
                    metrics.track(Event.SearchWidgetNewTabPressed)
                    Event.PerformedSearch.SearchAccessPoint.WIDGET
                }
                STATIC_SHORTCUT_NEW_TAB -> {
                    metrics.track(Event.PrivateBrowsingStaticShortcutTab)
                    Event.PerformedSearch.SearchAccessPoint.SHORTCUT
                }
                STATIC_SHORTCUT_NEW_PRIVATE_TAB -> {
                    metrics.track(Event.PrivateBrowsingStaticShortcutPrivateTab)
                    Event.PerformedSearch.SearchAccessPoint.SHORTCUT
                }
                PRIVATE_BROWSING_PINNED_SHORTCUT -> {
                    metrics.track(Event.PrivateBrowsingPinnedShortcutPrivateTab)
                    Event.PerformedSearch.SearchAccessPoint.SHORTCUT
                }
                else -> null
            }

            out.removeExtra(HomeActivity.OPEN_TO_SEARCH)

            val directions = source?.let {
                NavGraphDirections.actionGlobalSearchDialog(
                    sessionId = null,
                    searchAccessPoint = it
                )
            }
            directions?.let {
                val options = navOptions {
                    popUpTo = R.id.homeFragment
                }
                navController.nav(null, it, options)
            }
            true
        } else {
            false
        }
    }

    companion object {
        const val SEARCH_WIDGET = "search_widget"
        const val STATIC_SHORTCUT_NEW_TAB = "static_shortcut_new_tab"
        const val STATIC_SHORTCUT_NEW_PRIVATE_TAB = "static_shortcut_new_private_tab"
        const val PRIVATE_BROWSING_PINNED_SHORTCUT = "private_browsing_pinned_shortcut"
    }
}
