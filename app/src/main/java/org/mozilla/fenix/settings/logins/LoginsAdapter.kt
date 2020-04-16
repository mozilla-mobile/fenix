/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import mozilla.components.concept.storage.Login
import org.mozilla.fenix.R

class LoginsAdapter(
    private val interactor: SavedLoginsInteractor
) : ListAdapter<Login, LoginsListViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LoginsListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.logins_item, parent, false)
        return LoginsListViewHolder(view, interactor)
    }

    override fun onBindViewHolder(holder: LoginsListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<Login>() {
        override fun areItemsTheSame(oldItem: Login, newItem: Login) =
            oldItem.origin == newItem.origin

        override fun areContentsTheSame(oldItem: Login, newItem: Login) =
            oldItem == newItem
    }
}
