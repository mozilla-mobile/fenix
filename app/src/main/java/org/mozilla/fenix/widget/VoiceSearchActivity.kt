/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.widget

import android.content.Intent
import android.os.StrictMode
import mozilla.components.feature.search.widget.BaseVoiceSearchActivity
import mozilla.components.support.locale.LocaleManager
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.SearchWidget
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.ext.components
import java.util.Locale

/**
 * Implementation of voice search that is needed in search widget
 */
class VoiceSearchActivity : BaseVoiceSearchActivity() {

    override fun getCurrentLocale(): Locale {
        val locale = components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            LocaleManager.getCurrentLocale(this@VoiceSearchActivity)
                ?: LocaleManager.getSystemDefault()
        }
        return locale
    }

    override fun onSpeechRecognitionEnded(spokenText: String) {
        val intent = Intent(this, IntentReceiverActivity::class.java)
        intent.putExtra(SPEECH_PROCESSING, spokenText)
        intent.putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, true)
        startActivity(intent)
    }

    override fun onSpeechRecognitionStarted() {
        SearchWidget.voiceButton.record(NoExtras())
    }
}
