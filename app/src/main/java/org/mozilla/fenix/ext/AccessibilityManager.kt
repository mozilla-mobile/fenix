/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * Registers an [AccessibilityManager.AccessibilityStateChangeListener] for changes in
 * the global accessibility state of the system. It will automatically be unregistered
 * once [owner] is destroyed.
 *
 * @param owner Lifecycle owner to observer.
 * @param listener The listener.
 */
fun AccessibilityManager.addAccessibilityStateChangeListener(
    owner: LifecycleOwner,
    listener: AccessibilityManager.AccessibilityStateChangeListener
) {
    // Don't register if the owner is already destroyed
    if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
        return
    }

    addAccessibilityStateChangeListener(listener)

    owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            removeAccessibilityStateChangeListener(listener)
        }
    })
}
