/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AccessibilityManagerTest {

    private lateinit var accessibilityManager: AccessibilityManager

    @Before
    fun setup() {
        accessibilityManager = mockk {
            every { addAccessibilityStateChangeListener(any()) } returns true
            every { removeAccessibilityStateChangeListener(any()) } returns true
        }
    }

    @Test
    fun `observer will not get registered if lifecycle state is DESTROYED`() {
        val owner = MockedLifecycleOwner(Lifecycle.State.DESTROYED)
        val observer = mockk<AccessibilityManager.AccessibilityStateChangeListener>()

        accessibilityManager.addAccessibilityStateChangeListener(owner, observer)

        verify { accessibilityManager wasNot Called }
        verify { observer wasNot Called }
    }

    @Test
    fun `observer will get removed if lifecycle gets destroyed`() {
        val owner = MockedLifecycleOwner(Lifecycle.State.STARTED)
        val observer = mockk<AccessibilityManager.AccessibilityStateChangeListener>()

        accessibilityManager.addAccessibilityStateChangeListener(owner, observer)

        // Observer gets notified
        verify { accessibilityManager.addAccessibilityStateChangeListener(observer) }

        // Pretend lifecycle gets destroyed
        owner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        verify { accessibilityManager.removeAccessibilityStateChangeListener(observer) }
    }

    private class MockedLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this).apply {
            currentState = initialState
        }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }
}
