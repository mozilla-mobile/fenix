/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.view

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_saved_logins.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.settings.logins.LoginsListState
import org.mozilla.fenix.settings.logins.interactor.SavedLoginsInteractor
import org.mozilla.fenix.ext.addUnderline

/**
 * View that contains and configures the Saved Logins List
 */
class SavedLoginsListView(
    override val containerView: ViewGroup,
    val interactor: SavedLoginsInteractor
) : LayoutContainer {

    val view: FrameLayout = LayoutInflater.from(containerView.context)
        .inflate(R.layout.component_saved_logins, containerView, true)
        .findViewById(R.id.saved_logins_wrapper)

    private val loginsAdapter = LoginsAdapter(interactor)

    init {
        view.saved_logins_list.apply {
            adapter = loginsAdapter
            layoutManager = LinearLayoutManager(containerView.context)
            itemAnimator = null
        }

        with(view.saved_passwords_empty_learn_more) {
            movementMethod = LinkMovementMethod.getInstance()
            addUnderline()
            setOnClickListener { interactor.onLearnMoreClicked() }
        }

        with(view.saved_passwords_empty_message) {
            val appName = context.getString(R.string.app_name)
            text = String.format(
                context.getString(
                    R.string.preferences_passwords_saved_logins_description_empty_text
                ), appName
            )
        }
    }

    fun update(state: LoginsListState) {
        if (state.isLoading) {
            view.progress_bar.isVisible = true
        } else {
            view.progress_bar.isVisible = false
            view.saved_logins_list.isVisible = state.loginList.isNotEmpty()
            view.saved_passwords_empty_view.isVisible = state.loginList.isEmpty()
        }
        loginsAdapter.submitList(state.filteredItems)
    }
}
