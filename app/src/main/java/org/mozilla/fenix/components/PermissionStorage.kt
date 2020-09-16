/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import androidx.paging.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.feature.sitepermissions.SitePermissions
import mozilla.components.feature.sitepermissions.SitePermissions.Status
import mozilla.components.feature.sitepermissions.SitePermissionsStorage
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Mockable

@Mockable
class PermissionStorage(private val context: Context) {

    val permissionsStorage by lazy {
        SitePermissionsStorage(context, context.components.core.engine)
    }

    fun addSitePermissionException(
        origin: String,
        location: Status,
        notification: Status,
        microphone: Status,
        camera: Status
    ): SitePermissions {
        val sitePermissions = SitePermissions(
            origin = origin,
            location = location,
            camera = camera,
            microphone = microphone,
            notification = notification,
            savedAt = System.currentTimeMillis()
        )
        permissionsStorage.save(sitePermissions)
        return sitePermissions
    }

    suspend fun findSitePermissionsBy(origin: String): SitePermissions? = withContext(Dispatchers.IO) {
        permissionsStorage.findSitePermissionsBy(origin)
    }

    suspend fun updateSitePermissions(sitePermissions: SitePermissions) = withContext(Dispatchers.IO) {
        permissionsStorage.update(sitePermissions)
    }

    fun getSitePermissionsPaged(): DataSource.Factory<Int, SitePermissions> {
        return permissionsStorage.getSitePermissionsPaged()
    }

    suspend fun deleteSitePermissions(sitePermissions: SitePermissions) = withContext(Dispatchers.IO) {
        permissionsStorage.remove(sitePermissions)
    }

    suspend fun deleteAllSitePermissions() = withContext(Dispatchers.IO) {
        permissionsStorage.removeAll()
    }
}
