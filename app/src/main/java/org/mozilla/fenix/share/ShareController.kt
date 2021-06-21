/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.navigation.NavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.concept.sync.Device
import mozilla.components.concept.sync.TabData
import mozilla.components.feature.accounts.push.SendTabUseCases
import mozilla.components.feature.share.RecentAppsStorage
import mozilla.components.support.ktx.kotlin.isExtensionUrl
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.share.listadapters.AppShareOption

/**
 * [ShareFragment] controller.
 *
 * Delegated by View Interactors, handles container business logic and operates changes on it.
 */
interface ShareController {
    fun handleReauth()
    fun handleShareClosed()
    fun handleShareToApp(app: AppShareOption)
    fun handleAddNewDevice()
    fun handleShareToDevice(device: Device)
    fun handleShareToAllDevices(devices: List<Device>)
    fun handleSignIn()

    enum class Result {
        DISMISSED, SHARE_ERROR, SUCCESS
    }
}

/**
 * Default behavior of [ShareController]. Other implementations are possible.
 *
 * @param context [Context] used for various Android interactions.
 * @param shareSubject desired message subject used when sharing through 3rd party apps, like email clients.
 * @param shareData the list of [ShareData]s that can be shared.
 * @param sendTabUseCases instance of [SendTabUseCases] which allows sending tabs to account devices.
 * @param snackbar - instance of [FenixSnackbar] for displaying styled snackbars
 * @param navController - [NavController] used for navigation.
 * @param dismiss - callback signalling sharing can be closed.
 */
@Suppress("TooManyFunctions")
class DefaultShareController(
    private val context: Context,
    private val shareSubject: String?,
    private val shareData: List<ShareData>,
    private val sendTabUseCases: SendTabUseCases,
    private val snackbar: FenixSnackbar,
    private val navController: NavController,
    private val recentAppsStorage: RecentAppsStorage,
    private val viewLifecycleScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val dismiss: (ShareController.Result) -> Unit
) : ShareController {

    override fun handleReauth() {
        val directions = ShareFragmentDirections.actionGlobalAccountProblemFragment()
        navController.nav(R.id.shareFragment, directions)
        dismiss(ShareController.Result.DISMISSED)
    }

    override fun handleShareClosed() {
        dismiss(ShareController.Result.DISMISSED)
    }

    override fun handleShareToApp(app: AppShareOption) {
        viewLifecycleScope.launch(dispatcher) {
            recentAppsStorage.updateRecentApp(app.activityName)
        }

        val intent = Intent(ACTION_SEND).apply {
            putExtra(EXTRA_TEXT, getShareText())
            putExtra(EXTRA_SUBJECT, getShareSubject())
            type = "text/plain"
            flags = FLAG_ACTIVITY_NEW_DOCUMENT + FLAG_ACTIVITY_MULTIPLE_TASK
            setClassName(app.packageName, app.activityName)
        }

        @Suppress("TooGenericExceptionCaught")
        val result = try {
            context.startActivity(intent)
            ShareController.Result.SUCCESS
        } catch (e: Exception) {
            when (e) {
                is SecurityException, is ActivityNotFoundException -> {
                    snackbar.setText(context.getString(R.string.share_error_snackbar))
                    snackbar.show()
                    ShareController.Result.SHARE_ERROR
                }
                else -> throw e
            }
        }
        dismiss(result)
    }

    override fun handleAddNewDevice() {
        val directions = ShareFragmentDirections.actionShareFragmentToAddNewDeviceFragment()
        navController.navigate(directions)
    }

    override fun handleShareToDevice(device: Device) {
        context.metrics.track(Event.SendTab)
        shareToDevicesWithRetry { sendTabUseCases.sendToDeviceAsync(device.id, shareData.toTabData()) }
    }

    override fun handleShareToAllDevices(devices: List<Device>) {
        shareToDevicesWithRetry { sendTabUseCases.sendToAllAsync(shareData.toTabData()) }
    }

    override fun handleSignIn() {
        context.metrics.track(Event.SignInToSendTab)
        val directions =
            ShareFragmentDirections.actionGlobalTurnOnSync(padSnackbar = true)
        navController.nav(R.id.shareFragment, directions)
        dismiss(ShareController.Result.DISMISSED)
    }

    private fun shareToDevicesWithRetry(shareOperation: () -> Deferred<Boolean>) {
        // Use GlobalScope to allow the continuation of this method even if the share fragment is closed.
        GlobalScope.launch(Dispatchers.Main) {
            val result = if (shareOperation.invoke().await()) {
                showSuccess()
                ShareController.Result.SUCCESS
            } else {
                showFailureWithRetryOption { shareToDevicesWithRetry(shareOperation) }
                ShareController.Result.DISMISSED
            }
            if (navController.currentDestination?.id == R.id.shareFragment) {
                dismiss(result)
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showSuccess() {
        snackbar.apply {
            setText(getSuccessMessage())
            setLength(Snackbar.LENGTH_SHORT)
            show()
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showFailureWithRetryOption(operation: () -> Unit) {
        snackbar.setText(context.getString(R.string.sync_sent_tab_error_snackbar))
        snackbar.setLength(Snackbar.LENGTH_LONG)
        snackbar.setAction(context.getString(R.string.sync_sent_tab_error_snackbar_action), operation)
        snackbar.setAppropriateBackground(true)
        snackbar.show()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getSuccessMessage(): String = with(context) {
        when (shareData.size) {
            1 -> getString(R.string.sync_sent_tab_snackbar)
            else -> getString(R.string.sync_sent_tabs_snackbar)
        }
    }

    @VisibleForTesting
    fun getShareText() = shareData.joinToString("\n\n") { data ->
        val url = data.url.orEmpty()
        if (url.isExtensionUrl()) {
            // Sharing moz-extension:// URLs is not practical in general, as
            // they will only work on the current device.

            // We solve this for URLs from our reader extension as they contain
            // the original URL as a query parameter. This is a workaround for
            // now and needs a clean fix once we have a reader specific protocol
            // e.g. ext+reader://
            // https://github.com/mozilla-mobile/android-components/issues/2879
            Uri.parse(url).getQueryParameter("url") ?: url
        } else {
            url
        }
    }

    @VisibleForTesting
    internal fun getShareSubject() = shareSubject ?: shareData.map { it.title }.joinToString(", ")

    // Navigation between app fragments uses ShareTab as arguments. SendTabUseCases uses TabData.
    @VisibleForTesting
    internal fun List<ShareData>.toTabData() = map { data ->
        TabData(title = data.title.orEmpty(), url = data.url ?: data.text?.toDataUri().orEmpty())
    }

    private fun String.toDataUri(): String {
        return "data:,${Uri.encode(this)}"
    }
}
