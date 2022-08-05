/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.search

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import mozilla.components.feature.search.widget.AppSearchWidgetProvider
import mozilla.components.feature.search.widget.BaseVoiceSearchActivity
import mozilla.components.feature.search.widget.SearchWidgetConfig
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor
import org.mozilla.fenix.utils.IntentUtils
import org.mozilla.fenix.widget.VoiceSearchActivity

/**
 * Implementation of search widget
 */
class SearchWidgetProvider : AppSearchWidgetProvider() {

    // Implementation note:
    // This class name (SearchWidgetProvider) and package name (org.mozilla.gecko.search) should
    // not be changed because otherwise this widget will disappear from the home screen of the user.
    // The existing name replicates the name and package we used in Fennec.

    override fun onEnabled(context: Context) {
        context.settings().addSearchWidgetInstalled(1)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        context.settings().addSearchWidgetInstalled(-appWidgetIds.size)
    }

    override val config: SearchWidgetConfig =
        SearchWidgetConfig(
            searchWidgetIconResource = R.drawable.ic_launcher_foreground,
            searchWidgetMicrophoneResource = R.drawable.ic_microphone_widget,
            appName = R.string.app_name
        )

    override fun createTextSearchIntent(context: Context): PendingIntent {
        val textSearchIntent = Intent(context, IntentReceiverActivity::class.java)
            .apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                this.putExtra(
                    HomeActivity.OPEN_TO_SEARCH,
                    StartSearchIntentProcessor.SEARCH_WIDGET
                )
            }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_NEW_TAB,
            textSearchIntent,
            IntentUtils.defaultIntentPendingFlags or
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun shouldShowVoiceSearch(context: Context): Boolean {
        return context.settings().shouldShowVoiceSearch
    }

    override fun voiceSearchActivity(): Class<out BaseVoiceSearchActivity> {
        return VoiceSearchActivity::class.java
    }

    companion object {
        private const val REQUEST_CODE_NEW_TAB = 0
    }
}
