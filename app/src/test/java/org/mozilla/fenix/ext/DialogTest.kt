/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.app.Activity
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class DialogTest {

    @MockK private lateinit var dialog: Dialog
    @MockK private lateinit var window: Window
    @MockK(relaxed = true) private lateinit var activity: Activity
    private lateinit var attributes: WindowManager.LayoutParams

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        attributes = WindowManager.LayoutParams()

        every { dialog.window } returns window
        every { window.setFlags(any(), any()) } just Runs
        every { activity.window.attributes } returns attributes
    }

    @Test
    fun `no effect if window is null`() {
        every { dialog.window } returns null
        dialog.secure(activity)

        verify { window wasNot Called }
        verify { activity wasNot Called }
    }

    @Test
    fun `no effect if activity is null`() {
        dialog.secure(activity = null)

        verify { window wasNot Called }
        verify { activity wasNot Called }
    }

    @Test
    fun `no effect if activity is not secure`() {
        attributes.flags = 0

        dialog.secure(activity)

        verify { window wasNot Called }
    }

    @Test
    fun `secure dialog if activity is secure`() {
        attributes.flags = FLAG_SECURE

        dialog.secure(activity)

        verify { window.setFlags(FLAG_SECURE, FLAG_SECURE) }
    }
}
