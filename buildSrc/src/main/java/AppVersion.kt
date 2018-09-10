/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The first Nightly releases of Fenix  will ship in 2018 so we are using this as the base year for
// our version code.
private const val BASE_YEAR = 18

object AppVersion {
    /**
     * The version number shown to users.
     */
    val versionName = "1.0"

    /**
     * Internal version number used for release builds.
     */
    val baseVersionCode: Int

    init {
        // This code generates our base "unique" version code for release builds.
        //
        // The result of the version code generation depends on the timezone. We assume that this
        // code will only be used  for release versions and running on our build servers with a
        // fixed timezone.
        //
        // The version code is composed like: yDDDHHmm
        //  * y   = Double digit year, with 18 subtracted: 2018 -> 18 -> 0 or 2019 -> 19 -> 1
        //  * DDD = Day of the year, pad with zeros if needed: September 6th -> 249
        //  * HH  = Hour in day (00-23)
        //  * mm  = Minute in hour
        //
        // For September 10th, 2018, 5:04pm am this will generate the versionCode: 2531704 (0-253-1704).

        val today = Date()

        // We use the current year (double digit) and subtract the base year (see above). This value
        // will start counting at 0 and increment by one every year.
        val year = (SimpleDateFormat("yy", Locale.US).format(today).toInt() - BASE_YEAR).toString()

        // We use the day in the Year (e.g. 248) as opposed to month + day (0510) because it's one
        // digit shorter. If needed we pad with zeros (e.g. 25 -> 025)
        val day = String.format("%03d", SimpleDateFormat("D", Locale.US).format(today).toInt())

        // We append the hour in day (24h) and minute in hour (7:26 pm -> 1926). We do not append
        // seconds. This assumes that we do not need to build multiple release(!) builds the same
        // minute.
        val time = SimpleDateFormat("HHmm", Locale.US).format(today)

        baseVersionCode = (year + day + time).toInt()
    }
}
