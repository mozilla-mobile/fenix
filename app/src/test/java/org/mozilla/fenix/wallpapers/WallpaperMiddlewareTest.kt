/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.utils.Settings

class WallpaperMiddlewareTest {
    private val mockSettings: Settings = mockk()
    private val middleware = WallpaperMiddleware(mockSettings)
    private val captureMiddleware = CaptureActionsMiddleware<AppState, AppAction>()
    private val appStore = AppStore(middlewares = listOf(captureMiddleware, middleware))

    @Before
    fun setup() {
        captureMiddleware.reset()
    }

    @Test
    fun `WHEN InitAction intercepted THEN store updated with current wallpaper`() {
        val currentWallpaper = Wallpaper.FIRST
        every { mockSettings.currentWallpaper } returns currentWallpaper.name

        appStore.dispatch(AppAction.InitAction).joinBlocking()

        val expectedAction = AppAction.UpdateWallpaper(currentWallpaper)
        val resultAction = captureMiddleware.findFirstAction(expectedAction::class)
        assertEquals(expectedAction, resultAction)
    }

    @Test
    fun `WHEN action to switch next wallpaper intercepted THEN settings updated with next wallpaper`() {
        val currentWallpaper = Wallpaper.FIRST
        val updatedWallpaperSlot = slot<String>()
        every { mockSettings.currentWallpaper } returns currentWallpaper.name
        every { mockSettings.currentWallpaper = capture(updatedWallpaperSlot) } just runs

        appStore.dispatch(AppAction.SwitchToNextWallpaper).joinBlocking()

        assertEquals(updatedWallpaperSlot.captured, currentWallpaper.nextWallpaper.name)
    }

    @Test
    fun `WHEN action to update wallpaper intercepted THEN settings updated with wallpaper`() {
        val updatedWallpaper = Wallpaper.SECOND
        val updatedWallpaperSlot = slot<String>()
        every { mockSettings.currentWallpaper = capture(updatedWallpaperSlot) } just runs

        appStore.dispatch(AppAction.UpdateWallpaper(updatedWallpaper)).joinBlocking()

        assertEquals(updatedWallpaperSlot.captured, updatedWallpaper.name)
    }
}
