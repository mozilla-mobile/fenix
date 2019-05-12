/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.preference.PreferenceManager
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.leakcanary.LeakCanaryFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import leakcanary.LeakCanary
import org.mozilla.fenix.ext.getPreferenceKey

class DebugFenixApplication : FenixApplication() {

    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, false)

        if (FlipperUtils.shouldEnableFlipper(this)) {
            AndroidFlipperClient.getInstance(this).apply {
                addPlugin(InspectorFlipperPlugin(this@DebugFenixApplication,
                    DescriptorMapping.withDefaults()))
                addPlugin(LeakCanaryFlipperPlugin())
                addPlugin(SharedPreferencesFlipperPlugin(this@DebugFenixApplication,
                    this@DebugFenixApplication.packageName + "_preferences"))
                start()
            }
        }
    }

    override fun setupLeakCanary() {
        // TODO setup RecordLeakService
        updateLeakCanaryState()
    }

    override fun updateLeakCanaryState() {
        val isEnabled = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(getPreferenceKey(R.string.pref_key_leakcanary), false)

        LeakCanary.config = LeakCanary.config.copy(
            dumpHeap = isEnabled
        )
    }
}
