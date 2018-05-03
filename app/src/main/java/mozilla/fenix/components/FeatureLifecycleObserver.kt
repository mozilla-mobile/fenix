/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.fenix.components

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.toolbar.ToolbarFeature

/**
 * LifecycleObserver implementation that will forward lifecycle callbacks to "feature" components.
 */
class FeatureLifecycleObserver(
    private val sessionFeature: SessionFeature,
    private val toolbarFeature: ToolbarFeature
): LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun startFeatures() {
        sessionFeature.start()
        toolbarFeature.start()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stopFeatures() {
        sessionFeature.stop()
        toolbarFeature.stop()
    }
}
