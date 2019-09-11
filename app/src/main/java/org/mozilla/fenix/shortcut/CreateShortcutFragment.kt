/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shortcut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_create_shortcut.*
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

        val session = requireComponents.core.sessionManager.selectedSession

        if (session == null) {
            dismiss()
        } else {
            requireComponents.core.icons.loadIntoView(favicon_image, session.url)

            cancel_button.setOnClickListener { dismiss() }
            add_button.setOnClickListener {
                val text = shortcut_text.text.toString()
                viewLifecycleOwner.lifecycleScope.launch {
                    requireComponents.useCases.webAppUseCases.addToHomescreen(text)
                }.invokeOnCompletion { dismiss() }
            }

            shortcut_text.addTextChangedListener {
                updateAddButtonEnabledState()
            }

            shortcut_text.setText(session.title)
        }
    }

    private fun updateAddButtonEnabledState() {
        add_button.isEnabled = shortcut_text.text.isNotEmpty()
        add_button.alpha = if (shortcut_text.text.isNotEmpty()) ENABLED_ALPHA else DISABLED_ALPHA
    }

    companion object {
        private const val ENABLED_ALPHA = 1.0f
        private const val DISABLED_ALPHA = 0.4f
    }
}
