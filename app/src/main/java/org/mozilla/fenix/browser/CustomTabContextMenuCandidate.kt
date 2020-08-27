/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.content.Context
import android.os.Environment
import android.view.View
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.contextmenu.DefaultSnackbarDelegate
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.getPreferenceKey

class CustomTabContextMenuCandidate {
    companion object {
        /**
         * Returns the default list of context menu candidates for custom tabs/external apps.
         *
         */
        fun defaultCandidates(
            context: Context,
            contextMenuUseCases: ContextMenuUseCases,
            snackBarParentView: View,
            snackbarDelegate: ContextMenuCandidate.SnackbarDelegate = DefaultSnackbarDelegate()
        ): List<ContextMenuCandidate> {
            val downloadPathPrefKey = context.getPreferenceKey(R.string.pref_key_download_path)
            return listOf(
                ContextMenuCandidate.createCopyLinkCandidate(
                    context,
                    snackBarParentView,
                    snackbarDelegate
                ),
                ContextMenuCandidate.createShareLinkCandidate(context),
                ContextMenuCandidate.createSaveImageCandidate(
                    context,
                    contextMenuUseCases,
                    context.settings().preferences.getString(
                        downloadPathPrefKey, null
                    ) ?: Environment.DIRECTORY_DOWNLOADS
                ),
                ContextMenuCandidate.createCopyImageLocationCandidate(
                    context,
                    snackBarParentView,
                    snackbarDelegate
                )
            )
        }
    }
}
