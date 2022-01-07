
/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.utils.Settings

class WallpaperMiddleware(private val settings: Settings) : Middleware<AppState, AppAction> {
    override fun invoke(
        context: MiddlewareContext<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction
    ) {
        when (action) {
            AppAction.InitAction -> {
                val wallpaper = Wallpaper.valueOf(settings.currentWallpaper)
                context.store.dispatch(AppAction.UpdateWallpaper(wallpaper))
            }
            is AppAction.SwitchToNextWallpaper -> {
                val current = Wallpaper.valueOf(settings.currentWallpaper)
                settings.currentWallpaper = current.nextWallpaper.name
            }
            is AppAction.UpdateWallpaper -> {
                settings.currentWallpaper = action.wallpaper.name
            }
            else -> Unit
        }
        next(action)
    }
}
