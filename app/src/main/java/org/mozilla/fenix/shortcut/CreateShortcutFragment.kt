/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shortcut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_create_shortcut.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.loadIntoView
import org.mozilla.fenix.ext.requireComponents

class CreateShortcutFragment : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CreateShortcutDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_create_shortcut, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = requireComponents.core.sessionManager.selectedSession!!
        requireComponents.core.icons.loadIntoView(favicon_image, session.url)
        shortcut_text.setText(session.title)

        cancel_button.setOnClickListener { dismiss() }
        add_button.setOnClickListener {
            val text = shortcut_text.text.toString()
            MainScope().launch {
                requireComponents.useCases.webAppUseCases.addToHomescreen(text)
            }.invokeOnCompletion { dismiss() }
        }
    }
}
