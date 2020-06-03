/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.Login
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings
import org.mozilla.gecko.GeckoThread.launch
import kotlin.coroutines.CoroutineContext

interface SavedLoginsController {
    // NOOP
}

/**
 * Controller for the saved logins screen
 */
class DefaultSavedLoginsController(
    val context: Context,
    val loginsFragmentStore: LoginsFragmentStore,
    val settings: Settings
): SavedLoginsController {
    fun handleSort(sortingStrategy: SortingStrategy) {
        loginsFragmentStore.dispatch(LoginsAction.SortLogins(sortingStrategy))
        settings.savedLoginsSortingStrategy = sortingStrategy
    }
}

/**
 * Controller for editing a saved login
 */
class EditSavedLoginsController(
    val context: Context,
    val coroutineContext: CoroutineContext = Dispatchers.Main,
    val loginsFragmentStore: LoginsFragmentStore
): SavedLoginsController {

    fun findPotentialDuplicates(editedItem: SavedLogin) {
        var deferredLogin: Deferred<List<Login>>? = null
        val fetchLoginJob = MainScope().launch(IO) {
            deferredLogin = async {
                context.components.core
                    .passwordsStorage.getPotentialDupesIgnoringUsername(editedItem.mapToLogin())
            }
            val fetchedDuplicatesList = deferredLogin?.await()
            fetchedDuplicatesList?.let { list ->
                withContext(Dispatchers.Main) {
                    val savedLoginList = list.map { it.mapToSavedLogin() }
                    loginsFragmentStore.dispatch(
                        LoginsAction.ListOfDupes(savedLoginList)
                    )
                }
            }
        }
        fetchLoginJob.invokeOnCompletion {
            if (it is CancellationException) {
                deferredLogin?.cancel()
            }
        }
    }


//    withContext(coroutineContext) {
//        val duplicatesList =
//            context.components.core.passwordsStorage
//                .getPotentialDupesIgnoringUsername(editedItem.mapToLogin())
//
//        withContext(Dispatchers.Main) {
//            val mapped = duplicatesList.map { it.mapToSavedLogin() }
//            loginsFragmentStore.dispatch(
//                LoginsAction.ListOfDupes(mapped)
//            )
//        }
//    }
}
