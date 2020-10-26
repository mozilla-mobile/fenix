/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.downloads

import android.os.Environment
import io.mockk.every
import io.mockk.mockk
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.content.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import java.io.File

@Suppress("DEPRECATION")
@RunWith(FenixRobolectricTestRunner::class)
class DownloadFragmentTest {

    @Test
    fun `downloads are sorted from newest to oldest`() {

        val downloadedFile1 = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "1.pdf"
        )

        val downloadedFile2 = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "2.pdf"
        )

        val downloadedFile3 = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "3.pdf"
        )

        downloadedFile1.createNewFile()
        downloadedFile2.createNewFile()
        downloadedFile3.createNewFile()

        val fragment = DownloadFragment()

        val expectedList = listOf(
            DownloadItem(
                "3",
                "3.pdf",
                downloadedFile3.path,
                "null",
                null,
                DownloadState.Status.COMPLETED
            ),
            DownloadItem(
                "2",
                "2.pdf",
                downloadedFile2.path,
                "null",
                null,
                DownloadState.Status.COMPLETED
            ),
            DownloadItem(
                "1",
                "1.pdf",
                downloadedFile1.path,
                "null",
                null,
                DownloadState.Status.COMPLETED
            )
        )

        val state: BrowserState = mockk(relaxed = true)

        every { state.downloads } returns mapOf(
            "1" to DownloadState(
                id = "1",
                createdTime = 1,
                url = "url",
                fileName = "1.pdf",
                status = DownloadState.Status.COMPLETED
            ),
            "2" to DownloadState(
                id = "2",
                createdTime = 2,
                url = "url",
                fileName = "2.pdf",
                status = DownloadState.Status.COMPLETED
            ),
            "3" to DownloadState(
                id = "3",
                createdTime = 3,
                url = "url",
                fileName = "3.pdf",
                status = DownloadState.Status.COMPLETED
            )
        )

        val list = fragment.provideDownloads(state)

        assertEquals(expectedList, list)

        downloadedFile1.delete()
        downloadedFile2.delete()
        downloadedFile3.delete()
    }
}
