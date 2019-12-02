/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.helpers

import android.net.Uri

import okhttp3.mockwebserver.MockWebServer
import org.mozilla.fenix.helpers.ext.toUri
import java.util.concurrent.TimeUnit

/**
 * Helper for hosting web pages locally for testing purposes.
 */
object TestAssetHelper {
    @Suppress("MagicNumber")
    val waitingTime: Long = TimeUnit.SECONDS.toMillis(15)
    val waitingTimeShort: Long = TimeUnit.SECONDS.toMillis(1)
    data class TestAsset(val url: Uri, val content: String)

    /**
     * Hosts 3 simple websites, found at androidTest/assets/pages/generic[1|2|3].html
     * Returns a list of TestAsset, which can be used to navigate to each and
     * assert that the correct information is being displayed.
     *
     * Content for these pages all follow the same pattern. See [generic1.html] for
     * content implementation details.
     */
    fun getGenericAssets(server: MockWebServer): List<TestAsset> {
        @Suppress("MagicNumber")
        return (1..3).map {
            TestAsset(
                server.url("pages/generic$it.html").toString().toUri()!!,
                "Page content: $it"
            )
        }
    }

    fun getGenericAsset(server: MockWebServer, pageNum: Int): TestAsset {
        val url = server.url("pages/generic$pageNum.html").toString().toUri()!!
        val content = "Page content: $pageNum"

        return TestAsset(url, content)
    }

    fun getLoremIpsumAsset(server: MockWebServer): TestAsset {
        val url = server.url("pages/lorem-ipsum.html").toString().toUri()!!
        val content = "Page content: lorem ipsum"
        return TestAsset(url, content)
    }

    fun getRefreshAsset(server: MockWebServer): TestAsset {
        val url = server.url("pages/refresh.html").toString().toUri()!!
        val content = "Page content: refresh"

        return TestAsset(url, content)
    }

    fun getUUIDPage(server: MockWebServer): TestAsset {
        val url = server.url("pages/basic_nav_uuid.html").toString().toUri()!!
        val content = "Page content: basic_nav_uuid"

        return TestAsset(url, content)
    }

    fun getDownloadAsset(server: MockWebServer): TestAsset {
        val url = server.url("pages/download.html").toString().toUri()!!
        val content = "Page content: Globe.svg"

        return TestAsset(url, content)
    }
}
