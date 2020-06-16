/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.Login
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components

/**
 * Controller for all saved logins interactions with the password storage component
 */
open class SavedLoginsController(
    context: Context,
    private val navController: NavController,
    private val loginsFragmentStore: LoginsFragmentStore
) {

    private val activity: HomeActivity = context as HomeActivity

    private suspend fun getLogin(loginId: String): Login? =
        activity.components.core.passwordsStorage.get(loginId)

    fun delete(loginId: String) {
        var deleteLoginJob: Deferred<Boolean>? = null
        val deleteJob = activity.lifecycleScope.launch(Dispatchers.IO) {
            deleteLoginJob = async {
                activity.components.core.passwordsStorage.delete(loginId)
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

    fun save(loginId: String, usernameText: String, passwordText: String) {
        var saveLoginJob: Deferred<Unit>? = null
        activity.lifecycleScope.launch(Dispatchers.IO) {
            saveLoginJob = async {
                // must retrieve from storage to get the httpsRealm and formActionOrigin
                val oldLogin = activity.components.core.passwordsStorage.get(loginId)

                // Update requires a Login type, which needs at least one of
                // httpRealm or formActionOrigin
                val loginToSave = Login(
                    guid = oldLogin?.guid,
                    origin = oldLogin?.origin!!,
                    username = usernameText, // new value
                    password = passwordText, // new value
                    httpRealm = oldLogin.httpRealm,
                    formActionOrigin = oldLogin.formActionOrigin
                )

                save(loginToSave)
                syncAndUpdateList(loginToSave)
            }
            saveLoginJob?.await()
            withContext(Dispatchers.Main) {
                val directions =
                    EditLoginFragmentDirections
                        .actionEditLoginFragmentToLoginDetailFragment(loginId)
                navController.navigate(directions)
            }
        }
        saveLoginJob?.invokeOnCompletion {
            if (it is CancellationException) {
                saveLoginJob?.cancel()
            }
        }
    }

    private suspend fun save(loginToSave: Login) {
        activity.components.core.passwordsStorage.update(loginToSave)
    }

    private fun syncAndUpdateList(updatedLogin: Login) {
        val login = updatedLogin.mapToSavedLogin()
        loginsFragmentStore.dispatch(LoginsAction.UpdateLoginsList(listOf(login)))
    }

    fun findPotentialDuplicates(loginId: String) {
        var deferredLogin: Deferred<List<Login>>? = null
        // What scope should be used here?
        val fetchLoginJob = activity.lifecycleScope.launch(Dispatchers.IO) {
            deferredLogin = async {
                val login = getLogin(loginId)
                activity.components.core.passwordsStorage.getPotentialDupesIgnoringUsername(login!!)
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

    fun fetchLoginDetails(loginId: String) {
        var deferredLogin: Deferred<List<Login>>? = null
        val fetchLoginJob = activity.lifecycleScope.launch(Dispatchers.IO) {
            deferredLogin = async {
                activity.components.core.passwordsStorage.list()
            }
            val fetchedLoginList = deferredLogin?.await()

            fetchedLoginList?.let {
                withContext(Dispatchers.Main) {
                    val login = fetchedLoginList.filter {
                        it.guid == loginId
                    }.first()
                    loginsFragmentStore.dispatch(
                        LoginsAction.UpdateCurrentLogin(login.mapToSavedLogin())
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
}

