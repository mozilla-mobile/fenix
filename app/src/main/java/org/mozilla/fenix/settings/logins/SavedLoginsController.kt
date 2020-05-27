/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings
import kotlin.coroutines.CoroutineContext

interface SavedLoginsController {
    suspend fun findPotentialDuplicates(editedItem: SavedLogin)
}

/**
 * Controller for the saved logins screen
 */
class DefaultSavedLoginsController(
    val context: Context,
    val coroutineContext: CoroutineContext = Dispatchers.Main,
    val loginsFragmentStore: LoginsFragmentStore,
    val settings: Settings
): SavedLoginsController {
    fun handleSort(sortingStrategy: SortingStrategy) {
        loginsFragmentStore.dispatch(LoginsAction.SortLogins(sortingStrategy))
        settings.savedLoginsSortingStrategy = sortingStrategy
    }

    // TODO: What is the correct scope to use outside of the fragments?
    override suspend fun findPotentialDuplicates(editedItem: SavedLogin) {
        val duplicatesList = withContext(coroutineContext) {
            context.components.core.passwordsStorage.getPotentialDupesIgnoringUsername(editedItem.mapToLogin())
        }

        withContext(Dispatchers.Main) {
            val dupesExist = duplicatesList.filter {
                it.username != editedItem.username
            }.any()
            loginsFragmentStore.dispatch(
                LoginsAction.ListOfDupes(dupesExist)
            )
        }

//        var deferredLogin: Deferred<List<Login>>? = null
//        val fetchLoginJob = GlobalScope.launch(Dispatchers.IO) {
//            deferredLogin = async {
//                context.components.core
//                    .passwordsStorage.getPotentialDupesIgnoringUsername(editedItem.mapToLogin())
//            }
//            val fetchedDuplicatesList = deferredLogin?.await()
//            fetchedDuplicatesList?.let {
//                withContext(Dispatchers.Main) {
//                    val dupesExist = fetchedDuplicatesList.filter {
//                        it.username != editedItem.username
//                    }.any()
//                    loginsFragmentStore.dispatch(
//                        LoginsAction.ListOfDupes(dupesExist)
//                    )
//                }
//            }
//        }
//        fetchLoginJob.invokeOnCompletion {
//            if (it is CancellationException) {
//                deferredLogin?.cancel()
//            }
//        }
    }
}
