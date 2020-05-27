/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components

open class LoginsDataStore(
    val fragment: Fragment
) {

    fun delete(loginId: String) {
        val viewLifecycleOwner = fragment.viewLifecycleOwner
        val navController = fragment.findNavController()

        var deleteLoginJob: Deferred<Boolean>? = null
        val deleteJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            deleteLoginJob = async {
                fragment.requireContext().components.core.passwordsStorage.delete(loginId)
            }
            deleteLoginJob?.await()
            withContext(Dispatchers.Main) {
                navController.popBackStack(R.id.savedLoginsFragment, false)
            }
        }
        deleteJob.invokeOnCompletion {
            if (it is CancellationException) {
                deleteLoginJob?.cancel()
            }
        }
    }
}
