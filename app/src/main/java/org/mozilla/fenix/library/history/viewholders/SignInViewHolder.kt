/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history.viewholders

import android.util.Log
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.HistoryListSignInBinding
import org.mozilla.fenix.library.history.HistoryViewItem
import org.mozilla.fenix.tabstray.syncedtabs.SyncedTabsErrorItem
import org.mozilla.fenix.tabstray.syncedtabs.SyncedTabsListItem
import org.mozilla.fenix.theme.FirefoxTheme

class SignInViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val binding = HistoryListSignInBinding.bind(view)

    init {
        binding.composeContainer.setViewCompositionStrategy(
            ViewCompositionStrategy
                .DisposeOnViewTreeLifecycleDestroyed
        )
    }

    fun bind(item: HistoryViewItem.SignInHistoryItem) {
        binding.composeContainer.setContent {
            FirefoxTheme {
                SyncedTabsErrorItem(
                    errorText = item.instructionText,
                    errorButton = SyncedTabsListItem.ErrorButton(
                        buttonText = "Sign in",
                        onClick = {
                            Log.d("SignInViewHolder", "Sign in pressed!")
                        }
                    )
                )
            }
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.history_list_sign_in
    }
}
