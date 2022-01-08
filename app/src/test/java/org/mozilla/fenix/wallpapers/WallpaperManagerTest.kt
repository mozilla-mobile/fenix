package org.mozilla.fenix.wallpapers

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.utils.Settings

class WallpaperManagerTest {

    private val mockSettings: Settings = mockk()

    @Test
    fun `WHEN manager created THEN current wallpaper delegated by settings`() = runBlockingTest {
        val expectedWallpaper = Wallpaper.FIRST
        every { mockSettings.currentWallpaper } returns expectedWallpaper.name

        val manager = WallpaperManager(mockSettings)

        val result = manager.currentWallpaper.first()
        assertEquals(expectedWallpaper, result)
    }

    @Test
    fun `WHEN wallpaper edited THEN settings receive update and new value emitted`() = runBlockingTest {
        val currentWallpaper = Wallpaper.NONE
        every { mockSettings.currentWallpaper } returns currentWallpaper.name
        val wallpaperSlot = slot<String>()
        every { mockSettings.currentWallpaper = capture(wallpaperSlot) } just runs

        val newWallpaper = Wallpaper.FIRST
        val manager = WallpaperManager(mockSettings)
        manager.updateWallpaperSelection(newWallpaper)

        val result = manager.currentWallpaper.first()
        assertEquals(newWallpaper, result)
        assertEquals(newWallpaper.name, wallpaperSlot.captured)
    }

    @Test
    fun `WHEN wallpaper switched to next THEN settings receive update and new value emitted`() = runBlockingTest {
        val currentWallpaper = Wallpaper.values()[0]
        every { mockSettings.currentWallpaper } returns currentWallpaper.name
        val wallpaperSlot = slot<String>()
        every { mockSettings.currentWallpaper = capture(wallpaperSlot) } just runs

        val manager = WallpaperManager(mockSettings)
        manager.switchToNextWallpaper()

        val newWallpaper = Wallpaper.values()[1]
        val result = manager.currentWallpaper.first()
        assertEquals(newWallpaper, result)
        assertEquals(newWallpaper.name, wallpaperSlot.captured)
    }

    @Test
    fun `GIVEN current wallpaper is last WHEN wallpaper switched to next THEN first value is used`() = runBlockingTest {
        val currentWallpaper = Wallpaper.values().last()
        every { mockSettings.currentWallpaper } returns currentWallpaper.name
        val wallpaperSlot = slot<String>()
        every { mockSettings.currentWallpaper = capture(wallpaperSlot) } just runs

        val manager = WallpaperManager(mockSettings)
        manager.switchToNextWallpaper()

        val newWallpaper = Wallpaper.values().first()
        val result = manager.currentWallpaper.first()
        assertEquals(newWallpaper, result)
        assertEquals(newWallpaper.name, wallpaperSlot.captured)
    }
}
