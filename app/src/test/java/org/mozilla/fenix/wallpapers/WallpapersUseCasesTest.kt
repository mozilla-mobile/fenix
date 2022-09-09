package org.mozilla.fenix.wallpapers

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Wallpapers
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.utils.Settings
import java.util.Calendar
import java.util.Date

@RunWith(AndroidJUnit4::class)
class WallpapersUseCasesTest {

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    // initialize this once, so it can be shared throughout tests
    private val baseFakeDate = Date()
    private val fakeCalendar = Calendar.getInstance()

    private val appStore = AppStore()
    private val mockSettings = mockk<Settings> {
        every { currentWallpaperTextColor } returns 0L
        every { currentWallpaperTextColor = any() } just Runs
        every { currentWallpaperCardColor } returns 0L
        every { currentWallpaperCardColor = any() } just Runs
    }
    private val mockLegacyDownloader = mockk<LegacyWallpaperDownloader>(relaxed = true)
    private val mockLegacyFileManager = mockk<LegacyWallpaperFileManager> {
        every { clean(any(), any()) } just runs
    }

    private val mockMetadataFetcher = mockk<WallpaperMetadataFetcher>()
    private val mockDownloader = mockk<WallpaperDownloader> {
        coEvery { downloadWallpaper(any()) } returns mockk()
    }
    private val mockFileManager = mockk<WallpaperFileManager> {
        coEvery { clean(any(), any()) } returns mockk()
    }

    @Test
    fun `GIVEN legacy use case WHEN initializing THEN the default wallpaper is not downloaded`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockLegacyFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.LegacyInitializeWallpaperUseCase(
            appStore,
            mockLegacyDownloader,
            mockLegacyFileManager,
            mockSettings,
            "en-US",
            possibleWallpapers = listOf(Wallpaper.Default) + fakeRemoteWallpapers,
        ).invoke()

        appStore.waitUntilIdle()
        coVerify(exactly = 0) { mockLegacyDownloader.downloadWallpaper(Wallpaper.Default) }
    }

    @Test
    fun `GIVEN legacy use case WHEN initializing THEN default wallpaper is included in available wallpapers`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockLegacyFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.LegacyInitializeWallpaperUseCase(
            appStore,
            mockLegacyDownloader,
            mockLegacyFileManager,
            mockSettings,
            "en-US",
            possibleWallpapers = listOf(Wallpaper.Default) + fakeRemoteWallpapers,
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.availableWallpapers.contains(Wallpaper.Default))
    }

    @Test
    fun `GIVEN legacy use case and wallpapers that expired WHEN invoking initialize use case THEN expired wallpapers are filtered out and cleaned up`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val fakeExpiredRemoteWallpapers = listOf("expired").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.BEFORE, name)
        }
        val possibleWallpapers = fakeRemoteWallpapers + fakeExpiredRemoteWallpapers
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockLegacyFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.LegacyInitializeWallpaperUseCase(
            appStore,
            mockLegacyDownloader,
            mockLegacyFileManager,
            mockSettings,
            "en-US",
            possibleWallpapers = possibleWallpapers,
        ).invoke()

        val expectedFilteredWallpaper = fakeExpiredRemoteWallpapers[0]
        appStore.waitUntilIdle()
        assertFalse(appStore.state.wallpaperState.availableWallpapers.contains(expectedFilteredWallpaper))
        verify { mockLegacyFileManager.clean(Wallpaper.Default, possibleWallpapers) }
    }

    @Test
    fun `GIVEN leagacy use case and wallpapers that expired and an expired one is selected WHEN invoking initialize use case THEN selected wallpaper is not filtered out`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val expiredWallpaper = makeFakeRemoteWallpaper(TimeRelation.BEFORE, "expired")
        every { mockSettings.currentWallpaperName } returns expiredWallpaper.name
        coEvery { mockLegacyFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.LegacyInitializeWallpaperUseCase(
            appStore,
            mockLegacyDownloader,
            mockLegacyFileManager,
            mockSettings,
            "en-US",
            possibleWallpapers = fakeRemoteWallpapers + listOf(expiredWallpaper),
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.availableWallpapers.contains(expiredWallpaper))
        assertEquals(expiredWallpaper, appStore.state.wallpaperState.currentWallpaper)
    }

    @Test
    fun `GIVEN legacy use case and wallpapers that are in promotions outside of locale WHEN invoking initialize use case THEN promotional wallpapers are filtered out`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val locale = "en-CA"
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockLegacyFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.LegacyInitializeWallpaperUseCase(
            appStore,
            mockLegacyDownloader,
            mockLegacyFileManager,
            mockSettings,
            locale,
            possibleWallpapers = fakeRemoteWallpapers,
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.availableWallpapers.isEmpty())
    }

    @Test
    fun `GIVEN legacy use case and available wallpapers WHEN invoking initialize use case THEN available wallpapers downloaded`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockLegacyFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.LegacyInitializeWallpaperUseCase(
            appStore,
            mockLegacyDownloader,
            mockLegacyFileManager,
            mockSettings,
            "en-US",
            possibleWallpapers = fakeRemoteWallpapers,
        ).invoke()

        for (fakeRemoteWallpaper in fakeRemoteWallpapers) {
            coVerify { mockLegacyDownloader.downloadWallpaper(fakeRemoteWallpaper) }
        }
    }

    @Test
    fun `GIVEN legacy use case and a wallpaper has not been selected WHEN invoking initialize use case THEN store contains default`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockLegacyFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.LegacyInitializeWallpaperUseCase(
            appStore,
            mockLegacyDownloader,
            mockLegacyFileManager,
            mockSettings,
            "en-US",
            possibleWallpapers = fakeRemoteWallpapers,
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.currentWallpaper == Wallpaper.Default)
    }

    @Test
    fun `GIVEN legacy use case a wallpaper is selected and there are available wallpapers WHEN invoking initialize use case THEN these are dispatched to the store`() = runTest {
        val selectedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "selected")
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val possibleWallpapers = listOf(selectedWallpaper) + fakeRemoteWallpapers
        every { mockSettings.currentWallpaperName } returns selectedWallpaper.name
        coEvery { mockLegacyFileManager.lookupExpiredWallpaper(any()) } returns null

        WallpapersUseCases.LegacyInitializeWallpaperUseCase(
            appStore,
            mockLegacyDownloader,
            mockLegacyFileManager,
            mockSettings,
            "en-US",
            possibleWallpapers = possibleWallpapers,
        ).invoke()

        appStore.waitUntilIdle()
        assertEquals(selectedWallpaper, appStore.state.wallpaperState.currentWallpaper)
        assertEquals(possibleWallpapers, appStore.state.wallpaperState.availableWallpapers)
    }

    @Test
    fun `WHEN initializing THEN the default wallpaper is not downloaded`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        appStore.waitUntilIdle()
        coVerify(exactly = 0) { mockDownloader.downloadWallpaper(Wallpaper.Default) }
    }

    @Test
    fun `WHEN initializing THEN default wallpaper is included in available wallpapers`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.availableWallpapers.contains(Wallpaper.Default))
    }

    @Test
    fun `GIVEN wallpapers that expired WHEN invoking initialize use case THEN expired wallpapers are filtered out and cleaned up`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val fakeExpiredRemoteWallpapers = listOf("expired").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.BEFORE, name)
        }
        val possibleWallpapers = fakeRemoteWallpapers + fakeExpiredRemoteWallpapers
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns possibleWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        val expectedFilteredWallpaper = fakeExpiredRemoteWallpapers[0]
        appStore.waitUntilIdle()
        assertFalse(appStore.state.wallpaperState.availableWallpapers.contains(expectedFilteredWallpaper))
        coVerify { mockFileManager.clean(Wallpaper.Default, fakeRemoteWallpapers) }
    }

    @Test
    fun `GIVEN wallpapers that expired and an expired one is selected WHEN invoking initialize use case THEN selected wallpaper is not filtered out`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val expiredWallpaper = makeFakeRemoteWallpaper(TimeRelation.BEFORE, "expired")
        val allWallpapers = listOf(expiredWallpaper) + fakeRemoteWallpapers
        every { mockSettings.currentWallpaperName } returns "expired"
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns expiredWallpaper
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns allWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        val expectedWallpaper = expiredWallpaper.copy(
            thumbnailFileState = Wallpaper.ImageFileState.Downloaded,
        )
        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.availableWallpapers.contains(expectedWallpaper))
        assertEquals(expiredWallpaper, appStore.state.wallpaperState.currentWallpaper)
    }

    @Test
    fun `GIVEN wallpapers that are in promotions outside of locale WHEN invoking initialize use case THEN promotional wallpapers are filtered out`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val locale = "en-CA"
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            locale,
        ).invoke()

        appStore.waitUntilIdle()
        assertEquals(1, appStore.state.wallpaperState.availableWallpapers.size)
        assertEquals(Wallpaper.Default, appStore.state.wallpaperState.availableWallpapers[0])
    }

    @Test
    fun `GIVEN available wallpapers WHEN invoking initialize use case THEN available wallpaper thumbnails downloaded`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        for (fakeRemoteWallpaper in fakeRemoteWallpapers) {
            coVerify { mockDownloader.downloadThumbnail(fakeRemoteWallpaper) }
        }
    }

    @Test
    fun `GIVEN available wallpapers WHEN invoking initialize use case THEN thumbnails downloaded and the store state is updated to reflect that`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        for (fakeRemoteWallpaper in fakeRemoteWallpapers) {
            coVerify { mockDownloader.downloadThumbnail(fakeRemoteWallpaper) }
        }
        appStore.waitUntilIdle()
        assertTrue(
            appStore.state.wallpaperState.availableWallpapers.all {
                it.thumbnailFileState == Wallpaper.ImageFileState.Downloaded
            },
        )
    }

    @Test
    fun `GIVEN thumbnail download fails WHEN invoking initialize use case THEN the store state is updated to reflect that`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val failedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "failed")
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns listOf(failedWallpaper) + fakeRemoteWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded
        coEvery { mockDownloader.downloadThumbnail(failedWallpaper) } returns Wallpaper.ImageFileState.Error

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        val expectedWallpaper = failedWallpaper.copy(thumbnailFileState = Wallpaper.ImageFileState.Error)
        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.availableWallpapers.contains(expectedWallpaper))
    }

    @Test
    fun `GIVEN a wallpaper has not been selected WHEN invoking initialize use case THEN store contains default`() = runTest {
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        every { mockSettings.currentWallpaperName } returns ""
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns fakeRemoteWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        appStore.waitUntilIdle()
        assertTrue(appStore.state.wallpaperState.currentWallpaper == Wallpaper.Default)
    }

    @Test
    fun `GIVEN a wallpaper is selected and there are available wallpapers WHEN invoking initialize use case THEN these are dispatched to the store`() = runTest {
        val selectedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "selected")
        val fakeRemoteWallpapers = listOf("first", "second", "third").map { name ->
            makeFakeRemoteWallpaper(TimeRelation.LATER, name)
        }
        val possibleWallpapers = listOf(selectedWallpaper) + fakeRemoteWallpapers
        every { mockSettings.currentWallpaperName } returns selectedWallpaper.name
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        coEvery { mockMetadataFetcher.downloadWallpaperList() } returns possibleWallpapers
        coEvery { mockDownloader.downloadThumbnail(any()) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultInitializeWallpaperUseCase(
            appStore,
            mockDownloader,
            mockFileManager,
            mockMetadataFetcher,
            mockSettings,
            "en-US",
        ).invoke()

        val expectedWallpapers = (listOf(Wallpaper.Default) + possibleWallpapers).map {
            it.copy(thumbnailFileState = Wallpaper.ImageFileState.Downloaded)
        }
        appStore.waitUntilIdle()
        assertEquals(selectedWallpaper, appStore.state.wallpaperState.currentWallpaper)
        assertEquals(expectedWallpapers, appStore.state.wallpaperState.availableWallpapers)
    }

    @Test
    fun `WHEN legacy selected wallpaper usecase invoked THEN storage updated and store receives dispatch`() = runTest {
        val selectedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "selected")
        val slot = slot<String>()
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        every { mockSettings.currentWallpaperName } returns ""
        every { mockSettings.currentWallpaperName = capture(slot) } just runs

        WallpapersUseCases.LegacySelectWallpaperUseCase(
            mockSettings,
            appStore,
        ).invoke(selectedWallpaper)

        appStore.waitUntilIdle()
        assertEquals(selectedWallpaper.name, slot.captured)
        assertEquals(selectedWallpaper, appStore.state.wallpaperState.currentWallpaper)
        assertEquals(selectedWallpaper.name, Wallpapers.wallpaperSelected.testGetValue()?.first()?.extra?.get("name")!!)
    }

    @Test
    fun `GIVEN wallpaper downloaded WHEN selecting a wallpaper THEN storage updated and store receives dispatch`() = runTest {
        val selectedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "selected")
        val slot = slot<String>()
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        every { mockSettings.currentWallpaperName } returns ""
        every { mockSettings.currentWallpaperName = capture(slot) } just runs
        coEvery { mockFileManager.wallpaperImagesExist(selectedWallpaper) } returns true

        WallpapersUseCases.DefaultSelectWallpaperUseCase(
            mockSettings,
            appStore,
            mockFileManager,
            mockDownloader,
        ).invoke(selectedWallpaper)

        appStore.waitUntilIdle()
        assertEquals(selectedWallpaper.name, slot.captured)
        assertEquals(selectedWallpaper, appStore.state.wallpaperState.currentWallpaper)
        assertEquals(selectedWallpaper.name, Wallpapers.wallpaperSelected.testGetValue()?.first()?.extra?.get("name")!!)
    }

    @Test
    fun `GIVEN wallpaper is not downloaded WHEN selecting a wallpaper and download succeeds THEN storage updated and store receives dispatch`() = runTest {
        val selectedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "selected")
        val slot = slot<String>()
        val mockStore = mockk<AppStore>(relaxed = true)
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        every { mockSettings.currentWallpaperName } returns ""
        every { mockSettings.currentWallpaperName = capture(slot) } just runs
        coEvery { mockFileManager.wallpaperImagesExist(selectedWallpaper) } returns false
        coEvery { mockDownloader.downloadWallpaper(selectedWallpaper) } returns Wallpaper.ImageFileState.Downloaded

        WallpapersUseCases.DefaultSelectWallpaperUseCase(
            mockSettings,
            mockStore,
            mockFileManager,
            mockDownloader,
        ).invoke(selectedWallpaper)

        verify { mockStore.dispatch(AppAction.WallpaperAction.UpdateWallpaperDownloadState(selectedWallpaper, Wallpaper.ImageFileState.Downloading)) }
        verify { mockStore.dispatch(AppAction.WallpaperAction.UpdateWallpaperDownloadState(selectedWallpaper, Wallpaper.ImageFileState.Downloaded)) }
        verify { mockStore.dispatch(AppAction.WallpaperAction.UpdateCurrentWallpaper(selectedWallpaper)) }
    }

    @Test
    fun `GIVEN wallpaper is not downloaded WHEN selecting a wallpaper and any download fails THEN wallpaper not set and store receives dispatch`() = runTest {
        val selectedWallpaper = makeFakeRemoteWallpaper(TimeRelation.LATER, "selected")
        val slot = slot<String>()
        val mockStore = mockk<AppStore>(relaxed = true)
        coEvery { mockFileManager.lookupExpiredWallpaper(any()) } returns null
        every { mockSettings.currentWallpaperName } returns ""
        every { mockSettings.currentWallpaperName = capture(slot) } just runs
        coEvery { mockFileManager.wallpaperImagesExist(selectedWallpaper) } returns false
        coEvery { mockDownloader.downloadWallpaper(selectedWallpaper) } returns Wallpaper.ImageFileState.Error

        WallpapersUseCases.DefaultSelectWallpaperUseCase(
            mockSettings,
            mockStore,
            mockFileManager,
            mockDownloader,
        ).invoke(selectedWallpaper)

        verify { mockStore.dispatch(AppAction.WallpaperAction.UpdateWallpaperDownloadState(selectedWallpaper, Wallpaper.ImageFileState.Downloading)) }
        verify { mockStore.dispatch(AppAction.WallpaperAction.UpdateWallpaperDownloadState(selectedWallpaper, Wallpaper.ImageFileState.Error)) }
    }

    private enum class TimeRelation {
        BEFORE,
        NOW,
        LATER,
    }

    /**
     * [timeRelation] should specify a time relative to the time the tests are run
     */
    private fun makeFakeRemoteWallpaper(
        timeRelation: TimeRelation,
        name: String = "name",
        isInPromo: Boolean = true,
    ): Wallpaper {
        fakeCalendar.time = baseFakeDate
        when (timeRelation) {
            TimeRelation.BEFORE -> fakeCalendar.add(Calendar.DATE, -5)
            TimeRelation.NOW -> Unit
            TimeRelation.LATER -> fakeCalendar.add(Calendar.DATE, 5)
        }
        val relativeTime = fakeCalendar.time
        return if (isInPromo) {
            Wallpaper(
                name = name,
                collection = Wallpaper.Collection(
                    name = Wallpaper.firefoxCollectionName,
                    heading = null,
                    description = null,
                    availableLocales = listOf("en-US"),
                    startDate = null,
                    endDate = relativeTime,
                    learnMoreUrl = null,
                ),
                textColor = null,
                cardColor = null,
                thumbnailFileState = Wallpaper.ImageFileState.Unavailable,
                assetsFileState = Wallpaper.ImageFileState.Unavailable,
            )
        } else {
            Wallpaper(
                name = name,
                collection = Wallpaper.Collection(
                    name = Wallpaper.firefoxCollectionName,
                    heading = null,
                    description = null,
                    availableLocales = null,
                    startDate = null,
                    endDate = relativeTime,
                    learnMoreUrl = null,
                ),
                textColor = null,
                cardColor = null,
                thumbnailFileState = Wallpaper.ImageFileState.Unavailable,
                assetsFileState = Wallpaper.ImageFileState.Unavailable,
            )
        }
    }
}
