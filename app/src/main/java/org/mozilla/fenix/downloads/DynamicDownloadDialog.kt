/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.downloads

import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.download_dialog_layout.*
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.feature.downloads.toMegabyteString
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.settings

/**
 * [DynamicDownloadDialog] is used to show a view in the current tab to the user, triggered when
 *  downloadFeature.onDownloadStopped gets invoked. It uses [DynamicDownloadDialogBehavior] to
 *  hide when the users scrolls through a website as to not impede his activities.
 * */

class DynamicDownloadDialog(
    private val downloadState: DownloadState?,
    private val didFail: Boolean,
    private val tryAgain: (Long) -> Unit,
    private val onCannotOpenFile: () -> Unit,
    private val view: View,
    private val toolbarHeight: Int,
    private val onDismiss: () -> Unit
) : LayoutContainer {

    override val containerView = view
    private val settings = view.context.settings()

    init {
        setupDownloadDialog()
    }

    private fun setupDownloadDialog() {
        if (downloadState == null) return
        val context = view.context

        if (FeatureFlags.dynamicBottomToolbar) {
            (view.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
                behavior = DynamicDownloadDialogBehavior<View>(
                    context,
                    null,
                    toolbarHeight.toFloat()
                )
            }
        }

        if (settings.shouldUseBottomToolbar) {
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = toolbarHeight
            }
        }

        if (didFail) {
            download_dialog_title.text =
                context.getString(R.string.mozac_feature_downloads_failed_notification_text2)

            download_dialog_icon.setImageResource(
                mozilla.components.feature.downloads.R.drawable.mozac_feature_download_ic_download_failed
            )

            download_dialog_action_button.apply {
                text = context.getString(
                    mozilla.components.feature.downloads.R.string.mozac_feature_downloads_button_try_again
                )
                setOnClickListener {
                    tryAgain(downloadState.id)
                    context.metrics.track(Event.InAppNotificationDownloadTryAgain)
                    dismiss(view)
                }
            }
        } else {
            val titleText = context.getString(
                R.string.mozac_feature_downloads_completed_notification_text2
            ) + " (${downloadState.contentLength?.toMegabyteString()})"

            download_dialog_title.text = titleText

            download_dialog_icon.setImageResource(
                mozilla.components.feature.downloads.R.drawable.mozac_feature_download_ic_download_complete
            )

            download_dialog_action_button.apply {
                text = context.getString(
                    mozilla.components.feature.downloads.R.string.mozac_feature_downloads_button_open
                )
                setOnClickListener {
                    val fileWasOpened = AbstractFetchDownloadService.openFile(
                        context = context,
                        contentType = downloadState.contentType,
                        filePath = downloadState.filePath
                    )

                    if (!fileWasOpened) {
                        onCannotOpenFile()
                    }

                    context.metrics.track(Event.InAppNotificationDownloadOpen)
                    dismiss(view)
                }
            }
        }

        download_dialog_close_button.setOnClickListener {
            dismiss(view)
        }

        download_dialog_filename.text = downloadState.fileName
    }

    fun show() {
        view.visibility = View.VISIBLE

        if (FeatureFlags.dynamicBottomToolbar) {
            (view.layoutParams as CoordinatorLayout.LayoutParams).apply {
                (behavior as DynamicDownloadDialogBehavior).forceExpand(view)
            }
        }
    }

    private fun dismiss(view: View) {
        view.visibility = View.GONE
        onDismiss()
    }
}
