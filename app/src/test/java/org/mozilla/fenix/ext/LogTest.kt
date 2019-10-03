/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.util.Log
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ObsoleteCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)

class LogTest {

    val numCalls = if (BuildConfig.DEBUG) 1 else 0

    @Test
    fun `Test log debug function`() {
        mockkStatic(Log::class)
        logDebug("hi", "hi")
        verify(exactly = numCalls) { (Log.d("hi", "hi")) }
    }

    @Test
    fun `Test log warn function with tag and message args`() {
        mockkStatic(Log::class)
        logWarn("hi", "hi")
        verify(exactly = numCalls) { (Log.w("hi", "hi")) }
    }

    @Test
    fun `Test log warn function with tag, message, and exception args`() {
        mockkStatic(Log::class)
        val mockThrowable: Throwable = mockk(relaxed = true)
        logWarn("hi", "hi", mockThrowable)
        verify(exactly = numCalls) { (Log.w("hi", "hi", mockThrowable)) }
    }

    @Test
    fun `Test log error function with tag, message, and exception args`() {
        mockkStatic(Log::class)
        val mockThrowable: Throwable = mockk(relaxed = true)
        logErr("hi", "hi", mockThrowable)
        verify(exactly = numCalls) { (Log.e("hi", "hi", mockThrowable)) }
    }
}
