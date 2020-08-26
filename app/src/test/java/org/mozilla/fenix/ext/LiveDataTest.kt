/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class LiveDataTest {

    @Test
    fun `observe once then remove observer`() {
        val callback = mockk<(String) -> Unit>(relaxed = true)
        val observer = slot<Observer<String>>()
        val liveData = mockk<LiveData<String>>(relaxed = true)

        liveData.observeOnceAndRemoveObserver(callback)
        verify { liveData.observeForever(capture(observer)) }

        verify(inverse = true) { liveData.removeObserver(observer.captured) }
        verify(inverse = true) { callback(any()) }

        observer.captured.onChanged("test")
        verify { liveData.removeObserver(observer.captured) }
        verify { callback("test") }
    }
}
