/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.appstate

import mozilla.components.lib.state.Action
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.wallpapers.Wallpaper

/**
 * [Action] implementation related to [AppStore].
 */
sealed class AppAction : Action {
    object InitAction : AppAction()
    data class UpdateInactiveExpanded(val expanded: Boolean) : AppAction()
    object SwitchToNextWallpaper : AppAction()
    data class UpdateWallpaper(val wallpaper: Wallpaper) : AppAction()
}
