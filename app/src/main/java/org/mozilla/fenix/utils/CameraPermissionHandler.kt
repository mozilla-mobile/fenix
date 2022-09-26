package org.mozilla.fenix.utils

import android.Manifest
import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import org.mozilla.fenix.ext.settings

/**
 * Helper to handle the logic around the camera permission
 * The logic is to always prompt the user to grant the permission.
 * But the permission can reach a state of Denied Forever (the user won't be prompted anymore)
 *      - On android 11+: if the user deny twice the permission in a row
 *      - On android <= 10 : if the user select the `Deny Forever` option
 * We use a setting to memorize when we reach this state in order to display the permission dialog
 */
interface CameraPermissionHandler {

    /**
     *  Implementation should be provided by Fragment
     */
    fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I>

    /**
     * Implementation should be provided by Fragment
     */
    fun requireContext(): Context

    /**
     * Implementation should be provided by Fragment
     */
    fun shouldShowRequestPermissionRationale(permission: String): Boolean

    /**
     * Create an `ActivityResultLauncher<String>` used to request the camera permission.
     * Add the common logic around handling the result to track if we are denied forever.
     * Callbacks allow to add custom logic.
     */
    fun registerCameraLauncher(
        permissionGranted: () -> Unit,
        permissionDenied: (forever: Boolean) -> Unit,
    ): ActivityResultLauncher<String> {
        return registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                val wasBlocked = requireContext().settings().shouldShowCameraPermissionDialog
                requireContext().settings().setCameraPermissionDeniedForeverState =
                    !shouldShowRequestPermissionRationale(
                        Manifest.permission.CAMERA,
                    )

                permissionDenied(wasBlocked && requireContext().settings().shouldShowCameraPermissionDialog)
            } else {
                requireContext().settings().setCameraPermissionDeniedForeverState = false
                permissionGranted()
            }
        }
    }
}
