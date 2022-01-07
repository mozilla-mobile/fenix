/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperTest {
    @Test
    fun `WHEN next wallpaper accessed on last wallpaper THEN first wallpaper is result`() {
        val lastWallpaper = Wallpaper.values().last()

        val result = lastWallpaper.nextWallpaper

        assertEquals(Wallpaper.values().first(), result)
    }
}
