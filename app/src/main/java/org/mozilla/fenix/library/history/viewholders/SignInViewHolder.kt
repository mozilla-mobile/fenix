/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history.viewholders

import android.view.View
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.HistoryListSignInBinding
import org.mozilla.fenix.library.history.HistoryAdapter
import org.mozilla.fenix.library.history.HistoryInteractor

/**
 * A view representing a sign in window inside the synced history screen.
 * [HistoryAdapter] is responsible for creating and populating the view.
 *
 * @param view that is passed down to the parent's constructor.
 * @param historyInteractor Interactor to capture user interactions with the UI element.
 */
class SignInViewHolder(
    view: View,
    private val historyInteractor: HistoryInteractor,
) : RecyclerView.ViewHolder(view) {

    private val binding = HistoryListSignInBinding.bind(view)

    init {
        binding.signInButton.setOnClickListener {
            historyInteractor.onSignInClicked()
        }
        binding.createAccount.setOnClickListener {
            historyInteractor.onCreateAccountClicked()
        }
        binding.createAccount.text = HtmlCompat.fromHtml(
            binding.root.resources.getString(R.string.history_sign_in_create_account),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    companion object {
        const val LAYOUT_ID = R.layout.history_list_sign_in
    }
}
