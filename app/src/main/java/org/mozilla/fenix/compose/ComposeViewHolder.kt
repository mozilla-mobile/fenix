/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * [RecyclerView.ViewHolder] used for Jetpack Compose UI content .
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param viewLifecycleOwner [LifecycleOwner] life cycle owner for the view.
 */
abstract class ComposeViewHolder(
    val composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner
) : RecyclerView.ViewHolder(composeView) {

    /**
     * Composable that contains the content for a specific [ComposeViewHolder] implementation.
     */
    @Composable
    abstract fun Content()

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        composeView.setContent {
            FirefoxTheme {
                Content()
            }
        }

        ViewTreeLifecycleOwner.set(composeView, viewLifecycleOwner)
        ViewTreeSavedStateRegistryOwner.set(
            composeView,
            viewLifecycleOwner as SavedStateRegistryOwner
        )
    }
}
