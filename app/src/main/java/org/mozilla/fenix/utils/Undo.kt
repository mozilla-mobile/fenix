/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import java.util.concurrent.atomic.AtomicBoolean

internal const val UNDO_DELAY = 3000L
internal const val ACCESSIBLE_UNDO_DELAY = 15000L

/**
 * Get the recommended time an "undo" action should be available until it can automatically be
 * dismissed. The delay may be different based on the accessibility settings of the device.
 */
fun Context.getUndoDelay(): Long {
    return if (settings().accessibilityServicesEnabled) {
        ACCESSIBLE_UNDO_DELAY
    } else {
        UNDO_DELAY
    }
}

/**
 * Runs [operation] after giving user time (see [UNDO_DELAY]) to cancel it.
 * In case of cancellation, [onCancel] is executed.
 *
 * Execution of suspend blocks happens on [Dispatchers.Main].
 *
 * @param view A [View] used to determine a parent for the [FenixSnackbar].
 * @param message A message displayed as part of [FenixSnackbar].
 * @param undoActionTitle Label for the action associated with the [FenixSnackbar].
 * @param onCancel A suspend block to execute in case of cancellation.
 * @param operation A suspend block to execute if user doesn't cancel via the displayed [FenixSnackbar].
 * @param anchorView A [View] to which [FenixSnackbar] should be anchored.
 */
@Suppress("LongParameterList")
fun CoroutineScope.allowUndo(
    view: View,
    message: String,
    undoActionTitle: String,
    onCancel: suspend () -> Unit = {},
    operation: suspend (context: Context) -> Unit,
    anchorView: View? = null,
    elevation: Float? = null,
    paddedForBottomToolbar: Boolean = false,
) {
    // By using an AtomicBoolean, we achieve memory effects of reading and
    // writing a volatile variable.
    val requestedUndo = AtomicBoolean(false)

    @Suppress("ComplexCondition")
    fun showUndoSnackbar() {
        val snackbar = FenixSnackbar
            .make(
                view = view,
                duration = FenixSnackbar.LENGTH_INDEFINITE,
                isDisplayedWithBrowserToolbar = paddedForBottomToolbar,
            )
            .setText(message)
            .setAnchorView(anchorView)
            .setAction(undoActionTitle) {
                requestedUndo.set(true)
                launch {
                    onCancel.invoke()
                }
            }

        elevation?.also {
            snackbar.view.elevation = it
        }

        snackbar.show()

        // Wait a bit, and if user didn't request cancellation, proceed with
        // requested operation and hide the snackbar.
        launch {
            delay(view.context.getUndoDelay())

            if (!requestedUndo.get()) {
                snackbar.dismiss()
                operation.invoke(view.context)
            }
        }
    }

    showUndoSnackbar()
}

/**
 * Static object for showing a snackbar to undo the previous tab(s) close action.
 */
object UndoCloseTabSnackBar {
    /**
     * Show a snackbar that allows the user to undo the previous tab(s) close action.
     *
     * @param fragment The [Fragment] to obtain its view's LifecycleOwner and components.
     * @param isPrivate True if the snackbar message is for private tab(s), otherwise false.
     * @param multiple True if the snackbar message if for potentially multiple tabs, otherwise false.
     * @param onCancel block to execute in case of cancellation;
     * called after the undo tab(s) close action in a suspend block.
     * @param view A [View] used to determine a parent for the [FenixSnackbar],
     * defaulting to the view of [fragment].
     * @param anchorView A [View] to which [FenixSnackbar] should be anchored.
     * @param elevation Base elevation of the snackbar view.
     */
    fun show(
        fragment: Fragment,
        isPrivate: Boolean,
        multiple: Boolean = false,
        onCancel: () -> Unit = {},
        view: View = fragment.requireView(),
        anchorView: View? = null,
        elevation: Float? = null,
        paddedForBottomToolbar: Boolean = false,
    ) {
        val snackBarMessageId = getSnackBarMessageId(isPrivate, multiple)

        fragment.viewLifecycleOwner.lifecycleScope.allowUndo(
            view,
            message = fragment.requireContext().getString(snackBarMessageId),
            fragment.requireContext().getString(R.string.snackbar_deleted_undo),
            onCancel = suspend {
                fragment.requireComponents.useCases.tabsUseCases.undo.invoke()
                onCancel.invoke()
            },
            operation = { },
            anchorView = anchorView,
            elevation = elevation,
            paddedForBottomToolbar = paddedForBottomToolbar,
        )
    }

    @StringRes
    private fun getSnackBarMessageId(isPrivate: Boolean, multiple: Boolean): Int {
        return if (isPrivate) {
            if (multiple) {
                R.string.snackbar_private_tabs_closed
            } else {
                R.string.snackbar_private_tab_closed
            }
        } else {
            if (multiple) {
                R.string.snackbar_tabs_closed
            } else {
                R.string.snackbar_tab_closed
            }
        }
    }
}
