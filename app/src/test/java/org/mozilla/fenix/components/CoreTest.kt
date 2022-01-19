/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import mozilla.components.concept.engine.Engine.HttpsOnlyMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.Settings

private const val HTTPS_ONLY_ALL_TABS_MODE = "all"
private const val HTTPS_ONLY_PRIVATE_TABS_MODE = "private"

class CoreTest {
    @Test
    fun `GIVEN Https-only mode is disabled WHEN getHttpsOnlyMode is called THEN return HttpsOnlyMode#DISABLED`() {
        val settings: Settings = mockk(relaxed = true) {
            every { shouldUseHttpOnly } returns false
        }
        mockkStatic("org.mozilla.fenix.ext.ContextKt") {
            every { any<Context>().settings() } returns settings
            val core = Core(mockk(relaxed = true), mockk(), mockk())

            val result = core.getHttpsOnlyMode()

            assertEquals(HttpsOnlyMode.DISABLED, result)
        }
    }

    @Test
    fun `GIVEN Https-only mode is enabled WHEN getHttpsOnlyMode is called THEN return by default HttpsOnlyMode#ENABLED`() {
        val settings: Settings = mockk(relaxed = true) {
            every { shouldUseHttpOnly } returns true
        }
        mockkStatic("org.mozilla.fenix.ext.ContextKt") {
            every { any<Context>().settings() } returns settings
            val core = Core(mockk(relaxed = true), mockk(), mockk())

            val result = core.getHttpsOnlyMode()

            assertEquals(HttpsOnlyMode.ENABLED, result)
        }
    }

    @Test
    fun `GIVEN Https-only mode is enabled for all tabs WHEN getHttpsOnlyMode is called THEN return HttpsOnlyMode#ENABLED`() {
        val context: Context = mockk(relaxed = true)
        val settings: Settings = mockk(relaxed = true) {
            every { shouldUseHttpOnly } returns true
            every { shouldUseHttpOnlyMode } returns HTTPS_ONLY_ALL_TABS_MODE
        }
        mockkStatic("org.mozilla.fenix.ext.ContextKt") {
            every { any<Context>().settings() } returns settings
            every { context.getString(any()) } returns HTTPS_ONLY_ALL_TABS_MODE
            val core = Core(context, mockk(), mockk())

            val result = core.getHttpsOnlyMode()

            assertEquals(HttpsOnlyMode.ENABLED, result)
        }
    }

    @Test
    fun `GIVEN Https-only mode is enabled for only private tabs WHEN getHttpsOnlyMode is called THEN return HttpsOnlyMode#ENABLED_PRIVATE_ONLY`() {
        val context: Context = mockk(relaxed = true)
        val settings: Settings = mockk(relaxed = true) {
            every { shouldUseHttpOnly } returns true
            every { shouldUseHttpOnlyMode } returns HTTPS_ONLY_PRIVATE_TABS_MODE
        }
        mockkStatic("org.mozilla.fenix.ext.ContextKt") {
            every { any<Context>().settings() } returns settings
            every { context.getString(any()) } returns HTTPS_ONLY_ALL_TABS_MODE
            val core = Core(context, mockk(), mockk())

            val result = core.getHttpsOnlyMode()

            assertEquals(HttpsOnlyMode.ENABLED_PRIVATE_ONLY, result)
        }
    }

    @Test
    fun `GIVEN Https-only mode is enabled WHEN getHttpsOnlyMode is called specifying the mode to be disabled THEN return HttpsOnlyMode#DISABLED`() {
        val settings: Settings = mockk(relaxed = true) {
            every { shouldUseHttpOnly } returns true
        }
        mockkStatic("org.mozilla.fenix.ext.ContextKt") {
            every { any<Context>().settings() } returns settings
            val core = Core(mockk(relaxed = true), mockk(), mockk())

            val result = core.getHttpsOnlyMode(enabled = false)

            assertEquals(HttpsOnlyMode.DISABLED, result)
        }
    }

    @Test
    fun `GIVEN Https-only mode is disabled WHEN getHttpsOnlyMode is called specifying the mode to be enabled THEN return by default HttpsOnlyMode#ENABLED`() {
        val settings: Settings = mockk(relaxed = true) {
            every { shouldUseHttpOnly } returns false
        }
        mockkStatic("org.mozilla.fenix.ext.ContextKt") {
            every { any<Context>().settings() } returns settings
            val core = Core(mockk(relaxed = true), mockk(), mockk())

            val result = core.getHttpsOnlyMode(enabled = true)

            assertEquals(HttpsOnlyMode.ENABLED, result)
        }
    }

    @Test
    fun `GIVEN Https-only mode is disabled WHEN getHttpsOnlyMode is called specifying the mode to be enabled for private tabs THEN return by default HttpsOnlyMode#ENABLED_PRIVATE_ONLY`() {
        val context: Context = mockk(relaxed = true)
        val settings: Settings = mockk(relaxed = true) {
            every { shouldUseHttpOnly } returns false
            every { shouldUseHttpOnlyMode } returns HTTPS_ONLY_ALL_TABS_MODE
        }
        mockkStatic("org.mozilla.fenix.ext.ContextKt") {
            every { any<Context>().settings() } returns settings
            every { context.getString(any()) } returns HTTPS_ONLY_ALL_TABS_MODE
            val core = Core(mockk(relaxed = true), mockk(), mockk())

            val result = core.getHttpsOnlyMode(enabled = true, mode = HTTPS_ONLY_PRIVATE_TABS_MODE)

            assertEquals(HttpsOnlyMode.ENABLED_PRIVATE_ONLY, result)
        }
    }
}
