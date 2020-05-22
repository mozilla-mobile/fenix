/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.downloads

import android.view.Gravity
import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.downloads.AbstractFetchDownloadService.DownloadJobStatus
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.home.SharedViewModel
import org.mozilla.fenix.theme.ThemeManager

class DownloadsIntegration(
    private val rootView: View,
    private val dynamicDownloadDialogView: View,
    private val store: BrowserStore,
    private val tabId: String,
    useCases: DownloadsUseCases,
    onNeedToRequestPermissions: OnNeedToRequestPermissions,
    fragmentManager: FragmentManager,
    private val viewModel: SharedViewModel,
    private val expandToolbar: () -> Unit
) : LifecycleAwareFeature, PermissionsFeature {

    private val context = rootView.context
    private val toolbarHeight = context.resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)

    private val feature = DownloadsFeature(
        context.applicationContext,
        store = store,
        useCases = useCases,
        fragmentManager = fragmentManager,
        tabId = tabId,
        downloadManager = FetchDownloadManager(
            context.applicationContext,
            store,
            DownloadService::class
        ),
        promptsStyling = DownloadsFeature.PromptsStyling(
            gravity = Gravity.BOTTOM,
            shouldWidthMatchParent = true,
            positiveButtonBackgroundColor = ThemeManager.resolveAttribute(
                R.attr.accent,
                context
            ),
            positiveButtonTextColor = ThemeManager.resolveAttribute(
                R.attr.contrastText,
                context
            ),
            positiveButtonRadius = context.resources.getDimension(R.dimen.tab_corner_radius)
        ),
        onNeedToRequestPermissions = onNeedToRequestPermissions
    )

    init {
        feature.onDownloadStopped = { downloadState, _, downloadJobStatus ->
            // If the download is just paused, don't show any in-app notification
            if (downloadJobStatus == DownloadJobStatus.COMPLETED ||
                downloadJobStatus == DownloadJobStatus.FAILED
            ) {

                saveDownloadDialogState(tabId, downloadState, downloadJobStatus)

                buildDialog(
                    downloadState,
                    didFail = downloadJobStatus == DownloadJobStatus.FAILED,
                    tryAgain = feature::tryAgain
                )
            }
        }

        resumeDownloadDialogState(tabId)
    }

    override val onNeedToRequestPermissions = feature.onNeedToRequestPermissions

    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        feature.onPermissionsResult(permissions, grantResults)
    }

    override fun start() = feature.start()

    override fun stop() = feature.stop()

    /**
     * Preserves current state of the [DynamicDownloadDialog] to persist through tab changes and
     * other fragments navigation.
     * */
    private fun saveDownloadDialogState(
        tabId: String,
        downloadState: DownloadState,
        downloadJobStatus: DownloadJobStatus
    ) {
        viewModel.downloadDialogState[tabId] = Pair(
            downloadState,
            downloadJobStatus == DownloadJobStatus.FAILED
        )
    }

    /**
     * Re-initializes [DynamicDownloadDialog] if the user hasn't dismissed the dialog
     * before navigating away from it's original tab.
     * onTryAgain it will use [ContentAction.UpdateDownloadAction] to re-enqueue the former failed
     * download, because [DownloadsFeature] clears any queued downloads onStop.
     * */
    private fun resumeDownloadDialogState(tabId: String) {
        val (savedDownloadState, didFail) = viewModel.downloadDialogState[tabId] ?: return

        buildDialog(savedDownloadState, didFail) {
            savedDownloadState?.let { dlState ->
                store.dispatch(
                    ContentAction.UpdateDownloadAction(
                        tabId, dlState.copy(skipConfirmation = true)
                    )
                )
            }
        }
    }

    private fun buildDialog(
        downloadState: DownloadState?,
        didFail: Boolean,
        tryAgain: (Long) -> Unit
    ) {
        DynamicDownloadDialog(
            downloadState = downloadState,
            didFail = didFail,
            view = dynamicDownloadDialogView,
            toolbarHeight = toolbarHeight,
            tryAgain = tryAgain,
            onCannotOpenFile = {
                FenixSnackbar.make(
                    view = rootView,
                    duration = Snackbar.LENGTH_SHORT,
                    isDisplayedWithBrowserToolbar = true
                )
                    .setText(context.getString(R.string.mozac_feature_downloads_could_not_open_file))
                    .show()
            },
            onDismiss = {
                viewModel.downloadDialogState.remove(tabId)
            }
        ).show()

        expandToolbar()
    }
}
