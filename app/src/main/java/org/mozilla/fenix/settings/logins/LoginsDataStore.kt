/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.Login
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components

open class LoginsDataStore(
    val fragment: Fragment,
    val loginsFragmentStore: LoginsFragmentStore
) {

    private val viewLifecycleOwner = fragment.viewLifecycleOwner
    private val navController = fragment.findNavController()

    fun delete(loginId: String) {
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

    fun save(loginId: String, usernameText: String, passwordText: String) {
        var saveLoginJob: Deferred<Unit>? = null
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            saveLoginJob = async {
                // must retrieve from storage to get the httpsRealm and formActionOrigin
                val oldLogin =
                    fragment.requireContext().components.core.passwordsStorage.get(loginId)

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

    private suspend fun save(loginToSave: Login) =
        fragment.requireContext().components.core.passwordsStorage.update(loginToSave)


    private fun syncAndUpdateList(updatedLogin: Login) {
        val login = updatedLogin.mapToSavedLogin()
        loginsFragmentStore.dispatch(LoginsAction.UpdateLoginsList(listOf(login)))
    }
}
