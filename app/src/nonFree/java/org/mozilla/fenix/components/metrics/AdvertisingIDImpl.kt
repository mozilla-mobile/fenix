/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import mozilla.components.support.base.log.logger.Logger
import java.io.IOException

interface AdvertisingIDImpl : AdvertisingID {
    @Suppress("TooGenericExceptionCaught")
    override fun getAdvertisingID(context: Context): String? {
        return try {
            AdvertisingIdClient.getAdvertisingIdInfo(context).id
        } catch (e: GooglePlayServicesNotAvailableException) {
            Logger.debug("ActivationPing - Google Play not installed on the device")
            null
        } catch (e: GooglePlayServicesRepairableException) {
            Logger.debug("ActivationPing - recoverable error connecting to Google Play Services")
            null
        } catch (e: IllegalStateException) {
            // This is unlikely to happen, as this should be running off the main thread.
            Logger.debug("ActivationPing - AdvertisingIdClient must be called off the main thread")
            null
        } catch (e: IOException) {
            Logger.debug("ActivationPing - unable to connect to Google Play Services")
            null
        } catch (e: NullPointerException) {
            Logger.debug("ActivationPing - no Google Advertising ID available")
            null
        }
    }
}
