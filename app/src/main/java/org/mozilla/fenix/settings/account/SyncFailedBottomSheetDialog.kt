/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.account

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.sync_failed_layout.*
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.theme.ThemeManager

class SyncFailedBottomSheetDialog(
    context: Context,
    private val action: String,
    private val tryAgain: () -> Unit
    // We must pass in the BottomSheetDialog theme for the transparent window background to apply properly
) : BottomSheetDialog(context, R.style.Theme_MaterialComponents_BottomSheetDialog) {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.sync_failed_layout)
        val text = "Cannot X. Please check your Internet connection and try again."
        sync_failed_title.text = text.replace("X", action)

        sync_failed_action_button.apply {
            setOnClickListener {
                context.metrics.track(Event.InAppNotificationDownloadTryAgain)
                dismiss()
                tryAgain()
            }
        }

        sync_failed_close_button.setOnClickListener {
            dismiss()
        }

        setOnShowListener {
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                navigationBarColor = ContextCompat.getColor(
                        context,
                        ThemeManager.resolveAttribute(R.attr.foundation, context
                    )
                )
            }
        }
    }
}
