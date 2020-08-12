/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Context

interface AdvertisingID {
    /**
     * Query the Google Advertising API to get the Google Advertising ID.
     *
     * This is meant to be used off the main thread. The API will throw an
     * exception and we will print a log message otherwise.
     *
     * @return a String containing the Google Advertising ID or null.
     */
    fun query(context: Context): String?
}
