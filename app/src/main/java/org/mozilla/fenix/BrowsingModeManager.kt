/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

interface BrowsingModeManager {
    enum class Mode {
        Normal, Private
    }
}

var temporaryModeStorage = BrowsingModeManager.Mode.Normal
class DefaultBrowsingModeManager(private val homeActivity: HomeActivity) : BrowsingModeManager {
    val isPrivate: Boolean
        get() = mode == BrowsingModeManager.Mode.Private
    var mode: BrowsingModeManager.Mode
        get() = temporaryModeStorage
        set(value) {
            temporaryModeStorage = value
            updateTheme(value)
        }

    private fun updateTheme(value: BrowsingModeManager.Mode) {
        homeActivity.themeManager.apply {
            val newTheme = when (value) {
                BrowsingModeManager.Mode.Normal -> ThemeManager.Theme.Light
                BrowsingModeManager.Mode.Private -> ThemeManager.Theme.Private
            }
            setTheme(newTheme)
        }
    }
}
