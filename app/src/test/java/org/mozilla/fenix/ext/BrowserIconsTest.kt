/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.widget.ImageView
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.engine.gecko.fetch.GeckoViewFetchClient
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest
import org.mozilla.fenix.test.fenixTestContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class BrowserIconsTest {
    @Test
    fun loadIntoViewTest() {
        val imageView = spyk(ImageView(fenixTestContext))
        val icons = spyk(BrowserIcons(fenixTestContext, httpClient = GeckoViewFetchClient(fenixTestContext)))
        val myUrl = "https://mozilla.com"
        val request = spyk(IconRequest(url = myUrl))
        icons.loadIntoView(imageView, myUrl)
        verify { icons.loadIntoView(imageView, request) }
    }
}
