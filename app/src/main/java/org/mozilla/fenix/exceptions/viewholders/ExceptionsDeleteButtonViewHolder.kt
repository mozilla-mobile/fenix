/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.exceptions.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observer
import kotlinx.android.synthetic.main.delete_exceptions_button.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.exceptions.ExceptionsAction

class ExceptionsDeleteButtonViewHolder(
    view: View,
    private val actionEmitter: Observer<ExceptionsAction>
) : RecyclerView.ViewHolder(view) {
    private val deleteButton = view.removeAllExceptions

    init {
        deleteButton.setOnClickListener {
            actionEmitter.onNext(ExceptionsAction.Delete.All)
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.delete_exceptions_button
    }
}
