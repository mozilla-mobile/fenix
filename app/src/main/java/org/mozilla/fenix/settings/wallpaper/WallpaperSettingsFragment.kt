/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.components.service.glean.private.NoExtras
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.Wallpapers
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.Wallpaper

class WallpaperSettingsFragment : Fragment() {
    private val appStore by lazy {
        requireComponents.appStore
    }

    private val wallpaperUseCases by lazy {
        requireComponents.useCases.wallpaperUseCases
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Wallpapers.wallpaperSettingsOpened.record(NoExtras())
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FirefoxTheme {
                    val wallpapers = appStore.observeAsComposableState { state ->
                        state.wallpaperState.availableWallpapers
                    }.value ?: listOf()
                    val currentWallpaper = appStore.observeAsComposableState { state ->
                        state.wallpaperState.currentWallpaper
                    }.value ?: Wallpaper.Default

                    val coroutineScope = rememberCoroutineScope()

                    WallpaperSettings(
                        wallpaperGroups = wallpapers.groupByDisplayableCollection(),
                        defaultWallpaper = Wallpaper.Default,
                        selectedWallpaper = currentWallpaper,
                        loadWallpaperResource = {
                            wallpaperUseCases.loadThumbnail(it)
                        },
                        onSelectWallpaper = {
                            coroutineScope.launch {
                                val result = wallpaperUseCases.selectWallpaper(it)
                                onWallpaperSelected(it, result, this@apply)
                            }
                        },
                        onLearnMoreClick = { url ->
                            (activity as HomeActivity).openToBrowserAndLoad(
                                searchTermOrURL = url,
                                newTab = true,
                                from = BrowserDirection.FromWallpaper,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun onWallpaperSelected(
        wallpaper: Wallpaper,
        result: Wallpaper.ImageFileState,
        view: View,
    ) {
        when (result) {
            Wallpaper.ImageFileState.Downloaded -> {
                FenixSnackbar.make(
                    view = view,
                    isDisplayedWithBrowserToolbar = false,
                )
                    .setText(view.context.getString(R.string.wallpaper_updated_snackbar_message))
                    .setAction(requireContext().getString(R.string.wallpaper_updated_snackbar_action)) {
                        findNavController().navigate(R.id.homeFragment)
                    }
                    .show()
            }
            Wallpaper.ImageFileState.Error -> {
                FenixSnackbar.make(
                    view = view,
                    isDisplayedWithBrowserToolbar = false,
                )
                    .setText(view.context.getString(R.string.wallpaper_download_error_snackbar_message))
                    .setAction(view.context.getString(R.string.wallpaper_download_error_snackbar_action)) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val retryResult = wallpaperUseCases.selectWallpaper(wallpaper)
                            onWallpaperSelected(wallpaper, retryResult, view)
                        }
                    }
                    .show()
            }
            else -> { /* noop */ }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.customize_wallpapers))
    }
}
