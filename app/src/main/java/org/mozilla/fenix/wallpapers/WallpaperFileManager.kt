/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.fenix.wallpapers.Wallpaper.Companion.getLocalPath
import java.io.File

/**
 * Manages various functions related to the locally-stored wallpaper assets.
 *
 * @property storageRootDirectory The top level app-local storage directory.
 * @param coroutineDispatcher Dispatcher used to execute suspending functions. Default parameter
 * should be likely be used except for when under test.
 */
class WallpaperFileManager(
    private val storageRootDirectory: File,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(coroutineDispatcher)
    private val wallpapersDirectory = File(storageRootDirectory, "wallpapers")

    /**
     * Lookup all the files for a wallpaper name. This lookup will fail if there are not
     * files for each of a portrait and landscape orientation as well as a thumbnail.
     */
    suspend fun lookupExpiredWallpaper(name: String): Wallpaper? = withContext(Dispatchers.IO) {
        if (allAssetsExist(name)) {
            Wallpaper(
                name = name,
                collection = Wallpaper.DefaultCollection,
                textColor = null,
                cardColor = null,
                thumbnailFileState = Wallpaper.ImageFileState.Downloaded,
                assetsFileState = Wallpaper.ImageFileState.Downloaded,
            )
        } else null
    }

    private fun allAssetsExist(name: String): Boolean =
        Wallpaper.ImageType.values().all { type ->
            File(storageRootDirectory, getLocalPath(name, type)).exists()
        }

    /**
     * Remove all wallpapers that are not the [currentWallpaper] or in [availableWallpapers].
     */
    suspend fun clean(currentWallpaper: Wallpaper, availableWallpapers: List<Wallpaper>) = withContext(Dispatchers.IO) {
        scope.launch {
            val wallpapersToKeep = (listOf(currentWallpaper) + availableWallpapers).map { it.name }
            wallpapersDirectory.listFiles()?.forEach { file ->
                if (file.isDirectory && !wallpapersToKeep.contains(file.name)) {
                    file.deleteRecursively()
                }
            }
        }
    }

    /**
     * Checks whether all the assets for a wallpaper exist on the file system.
     */
    suspend fun wallpaperImagesExist(wallpaper: Wallpaper): Boolean = withContext(Dispatchers.IO) {
        return@withContext allAssetsExist(wallpaper.name)
    }
}
