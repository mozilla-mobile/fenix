/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.appstate

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.wallpapers.Wallpaper

class AppStoreReducerTest {
    @Test
    fun `WHEN SwitchToNextWallpaper action received THEN state updated with next wallpaper`() {
        val initialState = AppState(wallpaper = Wallpaper.FIRST)

        val resultState = AppStoreReducer.reduce(initialState, AppAction.SwitchToNextWallpaper)

        assertEquals(Wallpaper.SECOND, resultState.wallpaper)
    }

    @Test
    fun `WHEN UpdateWallpaper action received THEN state updated with new wallpaper`() {
        val initialState = AppState()

        val updatedWallpaper = Wallpaper.SECOND
        val resultState = AppStoreReducer.reduce(initialState, AppAction.UpdateWallpaper(updatedWallpaper))

        assertEquals(updatedWallpaper, resultState.wallpaper)
    }
}
