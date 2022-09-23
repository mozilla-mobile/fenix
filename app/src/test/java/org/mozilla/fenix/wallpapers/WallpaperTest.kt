package org.mozilla.fenix.wallpapers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperTest {
    @Test
    fun `GIVEN blank wallpaper name WHEN checking whether is default THEN is default`() {
        val result = Wallpaper.nameIsDefault("")

        assertTrue(result)
    }

    @Test
    fun `GIVEN the default wallpaper is set to be shown WHEN checking whether the current wallpaper should be default THEN return true`() {
        val result = Wallpaper.nameIsDefault("default")

        assertTrue(result)
    }

    @Test
    fun `GIVEN a custom wallpaper is set to be shown WHEN checking whether the current wallpaper should be default THEN return false`() {
        val result = Wallpaper.nameIsDefault("wally world")

        assertFalse(result)
    }

    @Test
    fun `GIVEN the legacy wallpaper default name none WHEN checking whether the current wallpaper should be default THEN return true`() {
        val result = Wallpaper.nameIsDefault("NONE")

        assertTrue(result)
    }
}
